package it.polito.cinqueti.chat.model;


import java.util.ArrayList;
import java.util.List;

//Incapsula una lista di stringhe

public class Roster {

	private List<String> users = new ArrayList<String>();

	public Roster(List<ChatUser> users) {
		
		for(ChatUser cu : users)
		
			this.users.add(cu.getNickname());
		
	}

	public List<String> getUsers() {
		return users;
	}

	public void setUsers(List<String> users) {
		this.users = users;
	}
}
