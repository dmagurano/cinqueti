package it.polito.cinqueti.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.polito.cinqueti.entities.DecodedAddress;
import it.polito.cinqueti.services.LineService;
import it.polito.cinqueti.services.UserService;

@RestController
public class UtilsRestController {

	@Autowired
	LineService lineService;
	
	@Autowired
	private UserService userService;
	
	// read the alert types from application.properties file
	@Value("#{'${alerts.types}'.split(',')}")
	private List<String> alertTypes;
	
	private byte[] defaultPicture = null;
	
	@PostConstruct
	public void init() {
		try {
			defaultPicture = extractBytes("/static/assets/default-profile.jpg");
		} catch (IOException e) {
			System.out.println("No default profile picture");
		}
	}
	
	@RequestMapping(value="/rest/address", method=RequestMethod.GET)
	public List<DecodedAddress> getAddresses(@RequestParam String address)
	{
		return lineService.getAddressInformation(address);
	}

	@RequestMapping(value="/rest/users/{nickname}/image", method=RequestMethod.GET)
	public byte[] getUserImage(@PathVariable(required=true) String nickname)
	{
		
		try{
			byte[] picture = userService.findByNickname(nickname).getImage();
			if(picture == null)
			{
				return defaultPicture;
			}
			return picture;
				
		}
		catch (NullPointerException e)
		{
			return defaultPicture;
		}

	}
	
	private byte[] extractBytes (String ImageName) throws IOException {
		 // open image
		InputStream  stream = AppRestController.class.getResourceAsStream(ImageName);
		byte[] buffer = new byte[8192];
		stream.read(buffer, 0, 8192);
		//close the stream
		stream.close();
		return buffer;
	}

	@RequestMapping(value="/rest/alerttypes", method=RequestMethod.GET)
	public List<String> getAlertTypes()
	{
		return alertTypes;
	}
}
