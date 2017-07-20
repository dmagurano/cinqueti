package it.polito.cinqueti.controllers;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.entities.VerificationToken;
import it.polito.cinqueti.services.SecurityService;
import it.polito.cinqueti.services.UserService;

@Controller
public class UserController {
	
	@Autowired
	private ServletContext servletContext;
	
    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;
    
    @Autowired
    private JavaMailSender javaMailSender;
	
    @Value("${user.minPasswordLength}")
	private Integer minPasswordLength;
    
    @Value("#{'${user.educationLevels}'.split(',')}")
	private List<String> educationLevels;
    
    @Value("#{'${user.jobs}'.split(',')}")
	private List<String> jobs;
    
    @Value("#{'${user.carSharingServices}'.split(',')}")
	private List<String> carSharingServices;
    
    // two phase registration
    // http://blog.codeleak.pl/2014/08/validation-groups-in-spring-mvc.html
    
    @RequestMapping(value = {"/register-first-phase"}, method = RequestMethod.GET)
    public String getRegister(Model model) {
    	model.addAttribute("user", new User());
    	
        return "register-first-phase";
    }
    
    @RequestMapping(value = "/register-first-phase", method = RequestMethod.POST)
    public String postRegister(
    		@Validated(User.FirstPhaseValidation.class) User user,
    		BindingResult bindingResult,
    		Model model) {
    	
    	String token = null;
    	
    	if (bindingResult.hasErrors()) {
            return "register-first-phase";
    	}
    	else{
        	try {
            	token = UUID.randomUUID().toString();
        		
        		userService.saveVerificationToken(user, token);
        		
        		String recipientAddress = user.getEmail();
        		String subject = "CinqueTi - Confirm registration";
        		String confirmationUrl = "https://localhost:8443/"
        				+ "registrationConfirm.html?token=" + token;
        		
        		SimpleMailMessage email = new SimpleMailMessage();
        		email.setTo(recipientAddress);
        		email.setSubject(subject);
        		email.setText(confirmationUrl);
        		javaMailSender.send(email);
        		
            } catch (Exception e) {
            	e.printStackTrace();
            	
            	model.addAttribute("exceptionMessage", "Some problems occured while registration. "
            			+ "Please register again.");
            	
            	userService.clearVerificationToken(user, token);
            	
                return "register-first-phase";
            }
        	
        	model.addAttribute("infoMessage", "Confirm your account by clicking on the link "
        			+ "we sent you on your email.");
        	
        	return "login";
        }
    }
    
    @RequestMapping(value = "/registrationConfirm", method = RequestMethod.GET)
    public String confirmRegistration(
    		@RequestParam("token") String token,
    		Model model,
    		RedirectAttributesModelMap redirectAttributes){
    	
    	User user = checkVerificationToken(token);
    	
    	if (user == null){
    		model.addAttribute("dangerMessage", "Your token is not valid. Please register.");
    		
    		return "bad-verification";
    	}
    	
    	model.addAttribute("educationLevels", educationLevels);
    	model.addAttribute("jobs", jobs);
    	model.addAttribute("carSharingServices", carSharingServices);
    	
    	model.addAttribute("token", token);
    	model.addAttribute("user", new User());
    	
    	return "register-second-phase";
    }
    
    @RequestMapping(value = "/register-second-phase", method = RequestMethod.POST)
    public String postRegisterSecondPhase(
    		@RequestParam("token") String token,
    		@Validated(User.SecondPhaseValidation.class) @ModelAttribute User user,
    		BindingResult bindingResult, 
    		@RequestParam MultipartFile file,
    		Model model) {
    	
    	User databaseUser = checkVerificationToken(token);
    	
    	if (databaseUser == null){
    		model.addAttribute("dangerMessage", "Your token is not valid. Please register again.");
    		
    		return "bad-verification";
    	}
    	
    	if (bindingResult.hasErrors()) {
    		model.addAttribute("educationLevels", educationLevels);
        	model.addAttribute("jobs", jobs);
        	model.addAttribute("carSharingServices", carSharingServices);

        	model.addAttribute("token", token);
        	
            return "register-second-phase";
    	}
    	else{
    		byte[] image = checkImage(file);
    		
    		if (image == null){
    			model.addAttribute("educationLevels", educationLevels);
            	model.addAttribute("jobs", jobs);
            	model.addAttribute("carSharingServices", carSharingServices);

            	model.addAttribute("token", token);
            	
                bindingResult.rejectValue("image", "user.registration.invalidImage");
                
            	return "register-second-phase";
    		}
    		
        	user.setId(databaseUser.getId());
        	user.setEmail(databaseUser.getEmail());
        	user.setPassword(databaseUser.getPassword());
        	user.setImage(image);
        	user.setEnabled(true);
            boolean registered = registerNewUser(user);
        	
            if (registered == false) {
        		model.addAttribute("educationLevels", educationLevels);
            	model.addAttribute("jobs", jobs);
            	model.addAttribute("carSharingServices", carSharingServices);

            	model.addAttribute("token", token);
            	
                bindingResult.rejectValue("email", "user.registration.alreadyInUseEmail");
                
            	return "register-second-phase";
            }
            else{
            	userService.removeVerificationToken(token);
            	
            	securityService.autologin(user.getUsername(), user.getConfirmedPassword());
            	
            	return "redirect:profile";
            }
        }
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model, String error, String logout) {
        if (error != null)
            model.addAttribute("error", "Your username and password is invalid.");

        if (logout != null)
            model.addAttribute("message", "You have been logged out successfully.");

        return "login";
    }
    
    @RequestMapping(value = {"/profile"}, method = RequestMethod.GET)
    public String profile(Model model) {
    	User currentUser = userService.findByEmail(securityService.findLoggedInUsername());
    	
    	model.addAttribute("user", currentUser);
    	
        return "profile";
    }
    
    @RequestMapping(value = "/change-nickname", method = RequestMethod.GET)
    public String changeNickname(Model model){
    	return "change-nickname";
    }
    
    @RequestMapping(value = "/change-nickname", method = RequestMethod.POST)
    public String changeNickname(
    		@RequestParam(value = "new-nickname", required = true) String newNickname,
    		Model model){
    	
    	if (newNickname.length() == 0){
    		model.addAttribute("emptyNickname", true);
    		return "change-nickname";
    	}
    	
    	User currentUser = userService.findByEmail(securityService.findLoggedInUsername());
    	
    	if (newNickname.compareTo(currentUser.getNickname()) != 0){
    		userService.updateUserNewNickname(currentUser, newNickname);
    	}
    	else{
    		model.addAttribute("alreadyInUseNickname", true);
    		// "redirect:change-nickname" does not show alert msg
    		return "change-nickname";
    	}
    	
    	return "redirect:profile";
    }
    
    @RequestMapping(value = "/change-password", method = RequestMethod.GET)
    public String changePassword(Model model){
    	return "change-password";
    }
    
    @RequestMapping(value = "/change-password", method = RequestMethod.POST)
    public String changPassword(
    		@RequestParam(value = "old-password", required = true) String oldPassword,
    		@RequestParam(value = "new-password", required = true) String newPassword,
    		@RequestParam(value = "new-password-confirmed", required = true) String newPasswordConfirmed, 
    		Model model){

    	if (newPassword.compareTo(newPasswordConfirmed) != 0){
    		model.addAttribute("passwordsMismatch", true);
    		return "change-password";
    	}
    	
    	// TODO check old == new
    	
    	if (newPassword.length() < minPasswordLength){
    		model.addAttribute("shortPassword", true);
    		return "change-password";
    	}
    	
    	User currentUser = userService.findByEmail(securityService.findLoggedInUsername());
    	
    	if (!userService.checkPassword(currentUser, oldPassword)){
    		model.addAttribute("wrongOldPassword", true);
    		return "change-password";
    	}
    	
    	userService.updateUserNewPassword(currentUser, newPassword);
    	model.addAttribute("passwordUpdated");
    	
    	return "redirect:profile";
    }
    
    private boolean registerNewUser(User user){
    	try{
    		userService.registerNewUser(user);
    	}
    	catch(Exception e){
    		e.printStackTrace();
    		return false;
    	}
    	
    	return true;
    }
    
    private User checkVerificationToken(String token){
    	VerificationToken verificationToken = userService.getVerificationToken(token);
    	
    	if ( verificationToken == null)
    		return null;
    	
    	if ( verificationToken.isExpired() )
    		return null;
    	
    	return verificationToken.getUser();
    }
    
    private byte[] checkImage(MultipartFile multipartFile){
    	byte[] image = null;
    	
    	try {
    		String mimeType = servletContext.getMimeType(multipartFile.getOriginalFilename());
    		
    		if (mimeType != null && mimeType.startsWith("image/"))
    			image = multipartFile.getBytes();
    		
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return image;
    }
}
