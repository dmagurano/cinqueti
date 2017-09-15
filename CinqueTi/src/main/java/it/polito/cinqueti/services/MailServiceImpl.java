package it.polito.cinqueti.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService{
	
	@Autowired
	JavaMailSender javaMailSender;

	@Override
	public boolean sendConfirmationEmail(String userEmail, String token) {
		
		String subject = "CinqueTi - Conferma il tuo account";
		
		String emailText = "Per confermare il tuo account su CinqueTi clicca sul seguente link.";
		
		String confirmationUrl = "https://localhost:8443/"
				+ "register-second-phase?token=" + token;
		
		SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(userEmail);
		email.setSubject(subject);
		email.setText(emailText + "\n\n" + confirmationUrl);
		
		try{
			javaMailSender.send(email);
		}
		catch(Exception e){
			e.printStackTrace();
			
			return false;
		}
		
		return true;
	}

}
