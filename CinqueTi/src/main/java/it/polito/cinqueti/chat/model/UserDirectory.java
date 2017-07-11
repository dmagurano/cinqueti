package it.polito.cinqueti.chat.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

//Usata per gestire gli utenti connessi al websocket
//E' una mappa che dice al web socket con questo id qual è l'utente connesso
//a tale web socket
@Component
public class UserDirectory {

		private Map<String,ChatUser> users = new HashMap<String,ChatUser>(); //E' una mappa che ha il sessionId
										  //e l'oggetto user relativo
		
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
