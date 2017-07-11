package it.polito.cinqueti.chat.model;

//Used to store the user in a map of users
public class ChatUser {
	
	String email;
	String nickname;
	String topicname;
	
	public ChatUser(String nickname, String topicname,String email) {
		this.email = email;
		this.nickname = nickname;
		this.topicname = topicname;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public String getTopicname() {
		return topicname;
	}
	public void setTopicname(String topicname) {
		this.topicname = topicname;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	
}
