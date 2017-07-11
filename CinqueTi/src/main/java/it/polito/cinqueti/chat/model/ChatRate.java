package it.polito.cinqueti.chat.model;

public class ChatRate {
	private String alertId;
	private Integer value;
	private String username;
	
	public ChatRate() {
	}
	
	public ChatRate(String alertId, Integer rate) {
		this.alertId = alertId;
		this.value = rate;
	}

	public String getAlertId() {
		return alertId;
	}
	
	public void setAlertId(String alertId) {
		this.alertId = alertId;
	}
	
	public Integer getValue() {
		return value;
	}
	
	public void setValue(Integer rate) {
		this.value = rate;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
