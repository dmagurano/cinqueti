package it.polito.cinqueti.controllers;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.entities.VerificationToken;
import it.polito.cinqueti.services.MailService;
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
    private MailService mailService;
	
    @Value("${user.minPasswordLength}")
	private Integer minPasswordLength;
    
    @Value("#{'${user.educationLevels}'.split(',')}")
	private List<String> educationLevels;
    
    @Value("#{'${user.jobs}'.split(',')}")
	private List<String> jobs;
    
    @Value("#{'${user.carSharingServices}'.split(',')}")
	private List<String> carSharingServices;
    
    @Value("#{'${user.car.fuelTypes}'.split(',')}")
	private List<String> fuelTypes;
    
    @Value("#{'${user.passTypes}'.split(',')}")
	private List<String> passTypes;
    
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
        	token = UUID.randomUUID().toString();
    		
    		if (mailService.sendConfirmationEmail(user.getEmail(), token) == false){
            	model.addAttribute("exceptionMessage", "Ci sono stati dei problemi durante la registrazione. "
            			+ "Registrati.");
            	
                return "register-first-phase";
    		}
    		else{
    			registerNewUser(user);
        		
        		userService.saveVerificationToken(user.getId(), token);
    			
	        	model.addAttribute("infoMessage", "Conferma il tuo account tramite la mail "
	        			+ "che abbiamo inviato sul tuo indirizzo di posta elettronica.");
	        	
	        	return "login";
    		}
        }
    }
    
    @RequestMapping(value = "/register-second-phase", method = RequestMethod.GET)
    public String confirmRegistration(
    		@RequestParam("token") String token,
    		Model model,
    		RedirectAttributesModelMap redirectAttributes){
    	
    	User databaseUser = checkVerificationToken(token);
    	
    	if (databaseUser == null){
    		model.addAttribute("dangerMessage", "Il tuo token non è valido. Registrati.");
    		
    		return "bad-verification";
    	}
    	
    	fillSecondPhaseModel(model, token);
    	
    	model.addAttribute("user", databaseUser);
    	
    	return "register-second-phase";
    }
    
    @RequestMapping(value = "/register-second-phase", method = RequestMethod.POST)
    public String postRegisterSecondPhase(
    		@RequestParam("token") String token,
    		@Validated(User.SecondPhaseValidation.class) User user,
    		BindingResult bindingResult, 
    		@RequestParam MultipartFile file,
    		Model model) {
    	
    	User databaseUser = checkVerificationToken(token);
    	
    	if (databaseUser == null){
    		model.addAttribute("dangerMessage", "Il tuo token non è valido. Registrati.");
    		
    		return "bad-verification";
    	}
    	
    	if (bindingResult.hasErrors()) {
    		fillSecondPhaseModel(model, token);
        	
            return "register-second-phase";
    	}
    	else{
    		byte[] image = parseImage(file);
    		
    		if (image == null){
    			fillSecondPhaseModel(model, token);
            	
                bindingResult.rejectValue("image", "user.registration.invalidImage");
                
            	return "register-second-phase";
    		}
    		else{
    			if (image.length > 0)
            		databaseUser.setImage(image);
    		}

    		databaseUser.setNickname(user.getNickname());
    		databaseUser.setGender(user.getGender());
    		databaseUser.setAge(user.getAge());
    		databaseUser.setEducation(user.getEducation());
    		databaseUser.setJob(user.getJob());
            boolean updated = updateNewUser(databaseUser);
        	
            if (updated == false) {
            	fillSecondPhaseModel(model, token);
            	
                bindingResult.rejectValue("email", "user.registration.emailNotFound");
                
            	return "register-second-phase";
            }
            else{
            	fillThirdPhaseModel(model, token);
            	
            	model.addAttribute("user", databaseUser);
            	
            	return "register-third-phase";
            }
        }
    }
    
    @RequestMapping(value = "/register-third-phase", method = RequestMethod.POST)
    public String postRegisterThirdPhase(
    		@RequestParam("token") String token,
    		@Validated(User.ThirdPhaseValidation.class) User user,
    		BindingResult bindingResult,
    		Model model) {
    	
    	User databaseUser = checkVerificationToken(token);
    	
    	if (databaseUser == null){
    		model.addAttribute("dangerMessage", "Il tuo token non è valido. Registrati.");
    		
    		return "bad-verification";
    	}
    	
    	if (bindingResult.hasErrors()) {
        	fillThirdPhaseModel(model, token);
        	
            return "register-second-phase";
    	}
    	else{
    		databaseUser.setOwnCar(user.getOwnCar());
    		databaseUser.setCarSharing(user.getCarSharing());
    		databaseUser.setBikeUsage(user.getBikeUsage());
    		databaseUser.setPubTransport(user.getPubTransport());
    		databaseUser.setEnabled(true);
            boolean updated = updateNewUser(databaseUser);
        	
            if (updated == false) {
            	fillThirdPhaseModel(model, token);
            	
                bindingResult.rejectValue("email", "user.registration.emailNotFound");
                
            	return "register-second-phase";
            }
            else{
            	userService.removeVerificationToken(token);
            	
            	securityService.autologin(databaseUser.getUsername(), databaseUser.getConfirmedPassword());
            	
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
    
    private boolean updateNewUser(User user){
    	try{
    		userService.updateNewUser(user);
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
    	
    	return userService.getUser(token);
    }
    
    private byte[] parseImage(MultipartFile multipartFile){
    	String mimeType = null;
    	byte[] image = null;
    	
    	if (multipartFile.isEmpty())
    		return new byte[0];
    	
    	mimeType = servletContext.getMimeType(multipartFile.getOriginalFilename());
		
		if (mimeType != null && mimeType.startsWith("image/")){
			
	    	try {
	    		
				image = multipartFile.getBytes();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	
    	return image;
    }
    
    private void fillSecondPhaseModel(Model model, String token){
    	model.addAttribute("educationLevels", educationLevels);
    	model.addAttribute("jobs", jobs);

    	model.addAttribute("token", token);
    }
    
    private void fillThirdPhaseModel(Model model, String token){
    	model.addAttribute("fuelTypes", fuelTypes);
    	model.addAttribute("passTypes", passTypes);
    	model.addAttribute("carSharingServices", carSharingServices);
    	
    	model.addAttribute("token", token);

    }
}
