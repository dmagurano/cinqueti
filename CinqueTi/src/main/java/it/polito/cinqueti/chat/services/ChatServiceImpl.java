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
import it.polito.cinqueti.entities.DecodedAddress;
import it.polito.cinqueti.entities.Message;
import it.polito.cinqueti.entities.Rate;
import it.polito.cinqueti.repositories.AlertRepository;
import it.polito.cinqueti.repositories.MessageRepository;
import it.polito.cinqueti.services.LineService;

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
	
	@Autowired
	private LineService lineService;
	
	@Value("#{'${alerts.expireAfter.minutes}'}")
	private Integer expireAfter;
	
	public void updateUsersList (String topic){
		
		String topicUsersList = "/topic/presence/"+  topic;
		// extract the list of user in the chat room 'topic' and send the updated list to its users
		messagingTemplate.convertAndSend(topicUsersList,new Roster(users.getUsers().stream()
																	.filter(u -> u.getTopicname().equals(topic))
																	.collect(Collectors.toList())));
	}
	
	public void sendMessage (String topic, ChatMessage msg) {
		String chatMessagesList = "/topic/chat/"+ topic;
		String alertTopic = "/topic/chat/alerts";
		
		// case 0: the message is sent by the client because an alert popup is opened
		// it's type should be 'update'
		if(msg.getType() != null && msg.getType().equals("update"))
		{
			// check alert validy and refresh its last access timestamp
			retireAlertIfExpired(msg.getAlertId());
			return; // done
		}
		// the message is not an update
		// let's try to extract an alert from it (if no alert is contained, the variable will be null)
		Alert alert = msg.extractAlert();
		if (alert != null){
			// case 1: new alert
			// check address validity (no malicius data could be saved in db!)
			boolean isValid = false;
			// query the ArcGis server with the address providen by the alert
			List<DecodedAddress> results = lineService.getAddressInformation(alert.getAddress());
			// search for a result with the same display_name, lat and lng
			for(DecodedAddress res: results)
			{
				if(res.getDisplay_name().equals(alert.getAddress()))
				{
					if(res.getAddress().getLat().equals(alert.getLat()) && res.getAddress().getLon().equals(alert.getLng()))
					{
						isValid = true;
						break;
					}
				}
			}
			if (!isValid)
				// no match found, reject the alert request
				return; 
			// no danger!
			// save the alert into the db
			alertRepository.save(alert);
			// the alert object has been updated: it contains the alertId assigned by mongo
			messagingTemplate.convertAndSend(alertTopic, alert);
			// we need also to process the ChatMessage as a normal message
		}
	    // convert the ChatMessage msg into a Message object in order to update the db
	    Message mess = new Message(msg.getMessage(), topic, msg.getUsername(), msg.getNickname(), msg.getDate());
	    // set as default false in quote properties (overriden if the message has a valid ref)
	    mess.setQuote(false);
	    msg.setQuote(false);
	    if (msg.getAlertId() != null)	// case 2: reference to existing alert
	    {
	    	// check if the alert is still alive
	    	if (!retireAlertIfExpired(msg.getAlertId()))
	    	{
	    		// valid reference if the alert is not expired
	    		mess.setAlertId(msg.getAlertId()); 
	    		mess.setQuote(true);
	    		msg.setQuote(true);
	    	}
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
	
	// check if the alert is still active
	// if not remove
	// otherwise update the last access ts
	// return: true if the alert is expired, false otherwise
	public boolean retireAlertIfExpired(String id) {
		String alertTopic = "/topic/chat/alerts";
		// check if the alert is still alive
    	Alert dbAlert = alertRepository.findById(id);
    	boolean expired = false;
    	if (dbAlert == null)
    		expired = true; // already expired and deleted
    	else 
    	{
    		Long minutesBetween = ((new Date()).getTime() - dbAlert.getLastAccessTimestamp().getTime()) / (60*1000) % 60;
    		if (minutesBetween > expireAfter)
    		{
    			expired = true; // expired right now, delete it
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
			// no update is sent to the users in order to avoid heavy load
		}
	}
	
	// push last 10 messages to the user
	public void retrieveLastMessages (String topic, String user) {
		String chatMessagesList = "/queue/"+ topic;
		List<Message> msgs= new ArrayList<Message>();
		// retrieve last 10 msg from db
		msgs = messageRepo.findTop10ByTopicOrderByTimestampDesc(topic);
		// reverse for user visualization
		Collections.reverse(msgs);
		// transform each Message into a ChatMessage
		List<ChatMessage> chmsgs = msgs.stream().map(m -> {
			ChatMessage cm = new ChatMessage();
			cm.setDate(m.getTimestamp())
			  .setMessage(m.getText())
			  .setNickname(m.getNickname())
			  .setUsername(m.getUserEmail()).setQuote(m.isQuote());
			if (alertRepository.findById(m.getAlertId()) != null)
				cm.setAlertId(m.getAlertId());
			return cm;
		}).collect(Collectors.toList());
		// send the list to a specific user
		messagingTemplate.convertAndSendToUser(user, chatMessagesList, chmsgs);
	}

	// send active alerts to a new user
	public void retrieveAlerts(String user){
		String chatMessagesList = "/queue/alerts";
		
		List<Alert> alerts = alertRepository.findAll();
		//check the last access time of every alert and remove the expired
		Iterator<Alert> it = alerts.iterator();
		while (it.hasNext()) {
			Alert alert = it.next();
			// calc the difference in minutes
			Long minutesBetween = ((new Date()).getTime() - alert.getLastAccessTimestamp().getTime()) / (60 * 1000) % 60;
			if (minutesBetween > expireAfter)
			{
				// delete the item from db and then remove it from list
				alertRepository.deleteAlertById(alert.getId());
				it.remove();
			}
			else
			{	// (NOT NEEDED?) update the lastAccessTimestamp and sync to db
				/*alert.setLastAccess();
				alertRepository.save(alert);*/
				
				// add to the alert the rate given by the current user, if any
				// used by client for real time avg
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
		// send the remaining alerts to user
		messagingTemplate.convertAndSendToUser(user, chatMessagesList, alerts);
	}
}
