package it.polito.cinqueti.chat.model;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

//Incapsula una lista di stringhe

public class Roster {

	private Set<String> users = new TreeSet<String>();

	public Roster(List<ChatUser> users) {
		
		for(ChatUser cu : users)
		
			this.users.add(cu.getNickname());
		
	}

	public Set<String> getUsers() {
		return users;
	}

	public void setUsers(Set<String> users) {
		this.users = users;
	}
}
