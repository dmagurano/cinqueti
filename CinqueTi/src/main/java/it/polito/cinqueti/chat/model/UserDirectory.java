package it.polito.cinqueti.chat.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

//Map contains the users connected for each chatroom of the websocket
@Component
public class UserDirectory {

		private Map<String,ChatUser> users = new HashMap<String,ChatUser>(); 
		
		public ChatUser getUser(String sessionId){
			return users.get(sessionId);
		}
		
		public void removeUser(String sessionId){
			users.remove(sessionId);
		}
		
		public void putUser(String sessionId, ChatUser nickname){
		    users.put(sessionId, nickname);
		}
		
		public Collection<ChatUser> getUsers(){
			return users.values();
		}
}
