package it.polito.cinqueti.chat.services;

import it.polito.cinqueti.chat.model.ChatMessage;
import it.polito.cinqueti.chat.model.ChatRate;

public interface ChatService {
	
	void updateUsersList (String topic);
	void sendMessage (String topic, ChatMessage msg);
	void sendRate (ChatRate chatRate);
	void retrieveLastMessages (String topic, String user);
	void retrieveAlerts(String name);

}
