package it.polito.cinqueti.entities;

public class Rate {
	private String email;
	private Integer value;
	
	public Rate() {
	}

	public Rate(String email, Integer value) {
		this.email = email;
		this.value = value;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}
}
