package it.polito.cinqueti.controllers;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.entities.VerificationToken;
import it.polito.cinqueti.services.SecurityService;
import it.polito.cinqueti.services.UserService;

@Controller
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;
    
    @Autowired
    private JavaMailSender javaMailSender;
	
    @Value("${user.minPasswordLength}")
	private Integer minPasswordLength;
    
    @Value("#{'${topics}'.split(',')}")
	private List<String> topics;
    
    @Value("#{'${user.educationLevels}'.split(',')}")
	private List<String> educationLevels;
    
    @Value("#{'${user.jobs}'.split(',')}")
	private List<String> jobs;
    
    @Value("#{'${user.carSharingServices}'.split(',')}")
	private List<String> carSharingServices;
    
    @RequestMapping(value = {"/register"}, method = RequestMethod.GET)
    public String test(Model model) {
    	model.addAttribute("user", new User());
    	model.addAttribute("educationLevels", educationLevels);
    	model.addAttribute("jobs", jobs);
    	model.addAttribute("carSharingServices", carSharingServices);
    	model.addAttribute("topics", topics);
    	
        return "register";
    }
    
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(
    		@ModelAttribute("user") @Valid User user, 
    		BindingResult result, 
    		@RequestParam MultipartFile file,
    		Model model,
    		WebRequest request) {
    	
    	boolean registered = true;
    	String token = null;
    	
    	if (result.hasErrors()) {
    		model.addAttribute("educationLevels", educationLevels);
        	model.addAttribute("jobs", jobs);
        	model.addAttribute("carSharingServices", carSharingServices);
        	model.addAttribute("topics", topics);
        	
            return "register";
    	}
    	else{
        	try {
    			user.setImage(file.getBytes());
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
        	
            registered = createUserAccount(user, result);
        	
            if (registered == false) {
        		model.addAttribute("educationLevels", educationLevels);
            	model.addAttribute("jobs", jobs);
            	model.addAttribute("carSharingServices", carSharingServices);
            	model.addAttribute("topics", topics);
            	
                result.rejectValue("email", "user.registration.alreadyInUseEmail");
                
            	return "register";
            }
            else{
            	try {
                	token = UUID.randomUUID().toString();
            		
            		userService.createVerificationToken(user, token);
            		
            		String recipientAddress = user.getEmail();
            		String subject = "Registration Confirmation";
            		String confirmationUrl = "https://localhost:8443/"
            				+ "registrationConfirm.html?token=" + token;
            		
            		SimpleMailMessage email = new SimpleMailMessage();
            		email.setTo(recipientAddress);
            		email.setSubject(subject);
            		email.setText(confirmationUrl);
            		javaMailSender.send(email);
            		
                } catch (Exception e) {
                	e.printStackTrace();
            		model.addAttribute("educationLevels", educationLevels);
                	model.addAttribute("jobs", jobs);
                	model.addAttribute("carSharingServices", carSharingServices);
                	model.addAttribute("topics", topics);
                	
                	model.addAttribute("exceptionMessage", "Some problems occured while registration. "
                			+ "Please register again.");
                	
                	userService.clearVerificationToken(user, token);
                	
                    return "register";
                }

            	model.addAttribute("topics", topics);
                
            	model.addAttribute("infoMessage", "Confirm your account by clicking on the link "
            			+ "we send you on your email.");
            	
            	return "login";
            }
        }
    }
    
    @RequestMapping(value = "/registrationConfirm", method = RequestMethod.GET)
    public String confirmRegistration(
    		WebRequest request,
    		Model model,
    		@RequestParam("token") String token){
    	
    	VerificationToken verificationToken = userService.getVerificationToken(token);
    	
    	if ( verificationToken == null){
        	model.addAttribute("topics", topics);
        	
    		model.addAttribute("dangerMessage", "Your token is not valid. Please register.");
    		
    		// TODO understand why "Chat e Segnalazioni" dropdown and "Login" dropdown don't work
    		
    		return "bad-verification";
    	}
    	
    	User user = verificationToken.getUser();
    	
    	Calendar calendar = Calendar.getInstance();
    	
    	if ( (verificationToken.getExpiryDate().getTime() - calendar.getTime().getTime()) <= 0 ){
        	model.addAttribute("topics", topics);
        	
    		model.addAttribute("dangerMessage", "Your token expired. Please register again.");
    		
    		userService.clearVerificationToken(user, token);
    		
    		return "bad-verification";
    	}
    	
    	user.setEnabled(true);
    	userService.saveRegisteredUser(user, token);
    	
    	model.addAttribute("topics", topics);

		model.addAttribute("infoMessage", "You confirmed your account.");
    	
    	return "login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model, String error, String logout) {
        if (error != null)
            model.addAttribute("error", "Your username and password is invalid.");

        if (logout != null)
            model.addAttribute("message", "You have been logged out successfully.");
        
    	model.addAttribute("topics", topics);

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
    	model.addAttribute("topics", topics);
    	
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
    	model.addAttribute("topics", topics);
    	
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
    
    private boolean createUserAccount(User user, BindingResult result) {
        
        try {
            userService.registerNewUserAccount(user);
        } catch (Exception e) {
            return false;
        }    
        return true;
    }
}
