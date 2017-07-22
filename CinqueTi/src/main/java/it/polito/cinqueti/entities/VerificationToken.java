package it.polito.cinqueti.entities;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

// http://www.baeldung.com/registration-verify-user-by-email
public class VerificationToken {

	private final Integer EXPIRE_HOURS = 24;
	
	private String token;
	
	private String userId;
	
	private Date expiryDate;
	
	public VerificationToken(String token, String userId) {
		this.userId = userId;
		this.token = token;
		this.expiryDate = calculateExpiryDate(EXPIRE_HOURS);
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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
