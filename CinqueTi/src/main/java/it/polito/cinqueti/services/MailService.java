package it.polito.cinqueti.services;

public interface MailService {
	
	public boolean sendConfirmationEmail(String userEmail, String token);

}
