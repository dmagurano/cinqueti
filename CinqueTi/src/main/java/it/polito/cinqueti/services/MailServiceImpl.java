package it.polito.cinqueti.services;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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
		
		String serverIpAddress = getServerIpAddress();
		
		String confirmationUrl = "https://" + serverIpAddress + ":8443/"
				+ "register-second-phase?token=" + token;
		
		String recommendationText = "N.B. completa la registrazione prima di usare l'applicazione.";
		
		SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(userEmail);
		email.setSubject(subject);
		email.setText(emailText + "\n\n" + confirmationUrl + "\n\n" + recommendationText);
		
		try{
			javaMailSender.send(email);
		}
		catch(Exception e){
			e.printStackTrace();
			
			return false;
		}
		
		return true;
	}
	
	private String getServerIpAddress(){
		
		try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<?> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    
                    if (!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address) {
                        String ipAddress=inetAddress.getHostAddress().toString();
                        
                        if (ipAddress.startsWith("192.168"))
                        	return ipAddress;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
		
        return "localhost";
	}

}
