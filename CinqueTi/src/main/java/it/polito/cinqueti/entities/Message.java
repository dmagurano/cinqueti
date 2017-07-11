package it.polito.cinqueti.entities;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Message {
	
	@Id
	private String id;
	
	private String text;
	
	private String topic;
	
	@Field("email")
	private String userEmail;
	
	private String nickname;
	
	private Date timestamp;
	
	private String alertId;


	public Message() {
	}
	
	public Message(String text, String topic, String userEmail, String nickname, Date timestamp) {
		this.text = text;
		this.topic = topic;
		this.userEmail = userEmail;
		this.nickname = nickname;
		this.timestamp = timestamp;
	}
	
	@JsonIgnore
	public String getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	@JsonIgnore
	public String getTopic() {
		return topic;
	}

	@JsonIgnore
	public String getUserEmail() {
		return userEmail;
	}

	@JsonIgnore
	public String getNickname() {
		return nickname;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getAlertId() {
		return alertId;
	}

	public void setAlertId(String alertId) {
		this.alertId = alertId;
	}
}
