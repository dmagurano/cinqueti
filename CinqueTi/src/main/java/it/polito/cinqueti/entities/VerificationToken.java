package it.polito.cinqueti.entities;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.springframework.data.annotation.Id;

// http://www.baeldung.com/registration-verify-user-by-email
public class VerificationToken {

	private final Integer EXPIRE_HOURS = 24;
	
	@Id
	private String token;
	
	private User user;
	
	private Date expiryDate;
	
	public VerificationToken(String token, User user) {
		this.setUser(user);
		this.token = token;
		this.expiryDate = calculateExpiryDate(EXPIRE_HOURS);
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Date getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	private Date calculateExpiryDate(int expiryTimeInHours){
		Calendar calendar = Calendar.getInstance();
		
		calendar.setTime(new Timestamp(calendar.getTime().getTime()));
		calendar.add(Calendar.HOUR, expiryTimeInHours);
		
		return new Date(calendar.getTime().getTime());
	}
	
	public boolean isExpired(){
		Calendar calendar = Calendar.getInstance();
		
		if ( (this.getExpiryDate().getTime() - calendar.getTime().getTime()) <= 0 )
    		return true;
		else
			return false;
	}
}
