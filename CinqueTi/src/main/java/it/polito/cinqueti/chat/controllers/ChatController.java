package it.polito.cinqueti.chat.controllers;

import java.security.Principal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import it.polito.cinqueti.chat.model.ChatMessage;
import it.polito.cinqueti.chat.model.ChatUser;
import it.polito.cinqueti.chat.model.ChatRate;
import it.polito.cinqueti.chat.model.JoinMessage;
import it.polito.cinqueti.chat.model.UserDirectory;
import it.polito.cinqueti.chat.services.ChatService;
import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.services.UserService;

@Controller(value="/app")
public class ChatController implements ApplicationListener<ApplicationEvent> {
	
	
	@Autowired
	private UserDirectory users;
	
	@Autowired
	private ChatService chatService;
	
	@Autowired
	private UserService userService;
	
	// react to user join message
	@MessageMapping("/join") 
	public void join(
		  MessageHeaders hs,
		  @Payload JoinMessage msg, Principal principal) {
	  
		String sessionId=(String)hs.get("simpSessionId");
		
		/*
		 * Cannot access an user from SecurityContext normally
		 * 
		 * http://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-stomp-authentication
		 */
		// find the user details from the user email 
		User u = userService.findByEmail(principal.getName());
		// convert the user information into a ChatUser object
		ChatUser cuser = new ChatUser(u.getNickname(),msg.getTopicName(),u.getEmail());
		// add the user to the users list
		users.putUser(sessionId,cuser);
		// send the updated list to every user in the chat room
		chatService.updateUsersList(msg.getTopicName());
		
		// send the list of active alert to the new user
		chatService.retrieveAlerts(principal.getName());
		// send the last 10 messages to the new user
		chatService.retrieveLastMessages(msg.getTopicName(), principal.getName());
	}
	
	// process every new message, message + alert, message + alert ref
	@MessageMapping("/chat") 
	public void sendMessage(
			  MessageHeaders hs, 						
			  ChatMessage chatMessage) {
		  
		String sessionId=(String)hs.get("simpSessionId");
		// extract the user info from the sId
		ChatUser u = users.getUser(sessionId);
		
		chatMessage.setDate(new Date()) // set message date as the receive timestamp
		   .setUsername(u.getEmail())	// add user info to the message
		   .setNickname(u.getNickname());
		// send the message to the chat room
		chatService.sendMessage(users.getUser(sessionId).getTopicname(), chatMessage);
	}
	
	// process alert rating
	@MessageMapping("/rate") 
	public void rateAlert(
			MessageHeaders hs, 						
			ChatRate chatRate) {
		  
		String sessionId=(String)hs.get("simpSessionId");
		ChatUser u = users.getUser(sessionId);
		      
		chatRate.setUsername(u.getEmail());
		// save the rate    
		chatService.sendRate(chatRate);
	}
	
	  
	@Override
	public void onApplicationEvent(ApplicationEvent e) {
		if (e instanceof SessionDisconnectEvent) {
			SessionDisconnectEvent sde=(SessionDisconnectEvent)e;
			String userTopic = users.getUser(sde.getSessionId()).getTopicname();
			// remove the user from the chat room and notify everyone
		    users.removeUser(sde.getSessionId());
		    chatService.updateUsersList(userTopic);
			}
		}
}
