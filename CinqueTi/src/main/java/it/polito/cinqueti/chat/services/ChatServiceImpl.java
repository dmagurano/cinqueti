package it.polito.cinqueti.chat.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import it.polito.cinqueti.chat.model.ChatMessage;
import it.polito.cinqueti.chat.model.ChatRate;
import it.polito.cinqueti.chat.model.Roster;
import it.polito.cinqueti.chat.model.UserDirectory;
import it.polito.cinqueti.entities.Alert;
import it.polito.cinqueti.entities.Message;
import it.polito.cinqueti.entities.Rate;
import it.polito.cinqueti.repositories.AlertRepository;
import it.polito.cinqueti.repositories.MessageRepository;

@Service
public class ChatServiceImpl implements ChatService {
	
	@Autowired
    private UserDirectory users;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	@Autowired
	private MessageRepository messageRepo;
	
	@Autowired
	private AlertRepository alertRepository;
	
	@Value("#{'${alerts.expireAfter.minutes}'}")
	private Integer expireAfter;
	
	public void updateUsersList (String topic){
		
		String topicUsersList = "/topic/presence/"+  topic;
	
		/*
		 * Added a filter to select users registered to the topic, before was sending all users to everyone
		 * 'https://www.sitepoint.com/java-8-streams-filter-map-reduce/'
		 */
		
		messagingTemplate.convertAndSend(topicUsersList,new Roster(users.getUsers().stream()
																	.filter(u -> u.getTopicname().equals(topic))
																	.collect(Collectors.toList()))); //Prendo l'elenco degli utenti
					
	}
	
	public void sendMessage (String topic, ChatMessage msg) {
		String chatMessagesList = "/topic/chat/"+ topic;
		String alertTopic = "/topic/chat/alerts";
		
		Alert alert = msg.extractAlert();
		if (alert != null){
			// case 1: new alert
			// save the alert into the db
			alertRepository.save(alert);
			// the alert object has been updated: it contains the alertId assigned by mongo
			messagingTemplate.convertAndSend(alertTopic, alert);
		}
	    
	    Message mess = new Message(msg.getMessage(), topic, msg.getUsername(), msg.getNickname(), msg.getDate());
	    
	    if (msg.getAlertId() != null)	// case 2: reference to existing alert
	    {
	    	// check if the alert is still alive
	    	if (!retireAlertIfExpired(msg.getAlertId()))
	    		mess.setAlertId(msg.getAlertId()); // valid reference if the alert is not expired
	    }
	    else if (alert != null)	// 	new alert
	    {
	    	// update the references with the id assigned by mongo
	    	mess.setAlertId(alert.getId());
	    	msg.setAlertId(alert.getId());
	    }
	    
	    messageRepo.save(mess);
	    
	    messagingTemplate.convertAndSend(chatMessagesList, msg);
	}
	
	// return: true if the alert is expired, false otherwise
	public boolean retireAlertIfExpired(String id) {
		String alertTopic = "/topic/chat/alerts";
		// check if the alert is still alive
    	Alert dbAlert = alertRepository.findById(id);
    	boolean expired = false;
    	if (dbAlert == null)
    		expired = true; // already expired
    	else 
    	{
    		Long minutesBetween = ((new Date()).getTime() - dbAlert.getLastAccessTimestamp().getTime()) / (60*1000) % 60;
    		if (minutesBetween > expireAfter)
    		{
    			expired = true; // expired right now
    			alertRepository.deleteAlertById(id);
    		}
    	}
    	if (expired)
    	{
    		messagingTemplate.convertAndSend(alertTopic, new Alert(id,"remove")); // send a special alert to everyone in order to retire the old alert
    		return true;
    	}
    	// the alert is not expired
    	// update the lastAccessTimestamp and sync to db
    	dbAlert.setLastAccess();
		alertRepository.save(dbAlert);
    	return false;

    		
	}
	
	public void sendRate (ChatRate chatRate) {
		// check if the rated alert is still alive 	
		if(!retireAlertIfExpired(chatRate.getAlertId()))
		{
			// it's alive! Rate it
			Rate rate = new Rate(chatRate.getUsername(), chatRate.getValue());
			alertRepository.updateUserRate(chatRate.getAlertId(), rate);
		}
	}
	
	public void retrieveLastMessages (String topic, String user) {
		String chatMessagesList = "/queue/"+ topic;
		List<Message> msgs= new ArrayList<Message>();
		msgs = messageRepo.findTop10ByTopicOrderByTimestampDesc(topic);
		
//		List<StaffPublic> result = staff.stream().map(temp -> {
//            StaffPublic obj = new StaffPublic();
//            obj.setName(temp.getName());
//            obj.setAge(temp.getAge());
//            if ("mkyong".equals(temp.getName())) {
//                obj.setExtra("this field is for mkyong only!");
//            }
//            return obj;
//        }).collect(Collectors.toList());
		Collections.reverse(msgs);
		
		List<ChatMessage> chmsgs = msgs.stream().map(m -> {
			ChatMessage cm = new ChatMessage();
			cm.setDate(m.getTimestamp())
			  .setMessage(m.getText())
			  .setNickname(m.getNickname())
			  .setUsername(m.getUserEmail());
			if (alertRepository.findById(m.getAlertId()) != null)
				cm.setAlertId(m.getAlertId());
			return cm;
		}).collect(Collectors.toList());
		
		messagingTemplate.convertAndSendToUser(user, chatMessagesList, chmsgs);
				
//		// Generate an iterator. Start just after the last element.
//		ListIterator<Message> li = msgs.listIterator(msgs.size());
//		// Iterate in reverse.
//		while(li.hasPrevious()) {
//		  Message mess = li.previous();
//		  messagingTemplate.convertAndSendToUser(user, chatMessagesList, new ChatMessage(mess.getUserEmail(), mess.getNickname(), mess.getText(), mess.getTimestamp()));
//		}
	}

	public void retrieveAlerts(String user){
		String chatMessagesList = "/queue/alerts";
		
		List<Alert> alerts = alertRepository.findAll();
		//check the last access time of every alert and remove the expired
		Iterator<Alert> it = alerts.iterator();
		while (it.hasNext()) {
			Alert alert = it.next();
			// calc the difference in minutes
			Long minutesBetween = ((new Date()).getTime() - alert.getLastAccessTimestamp().getTime()) / (60 * 1000) % 60;
			// TODO remove next comment
			// optional: for debug purpose use the next line to get 5 seconds interval
//			Long minutesBetween = ((new Date()).getTime() - alert.getLastAccessTimestamp().getTime()) / (1000) % 60;
			if (minutesBetween > expireAfter)
			{
				// delete the item from db and then remove it from list
				alertRepository.deleteAlertById(alert.getId());
				it.remove();
			}
			else
			{	// update the lastAccessTimestamp and sync to db
				alert.setLastAccess();
				alertRepository.save(alert);
				for (Rate rate: alert.getRates())
				{
					if (rate.getEmail().equals(user))
					{
						alert.setMyRate(rate.getValue());
						break;
					}
				}
			}
		}	
		
		messagingTemplate.convertAndSendToUser(user, chatMessagesList, alerts);
	}
}
