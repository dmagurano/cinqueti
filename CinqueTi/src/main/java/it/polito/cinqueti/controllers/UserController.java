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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
    	if (!model.containsAttribute("org.springframework.validation.BindingResult.user"))
    		model.addAttribute("user", new User());
    	
        return "register-first-phase";
    }
    
    @RequestMapping(value = "/register-first-phase", method = RequestMethod.POST)
    public String postRegister(
    		@Validated(User.FirstPhaseValidation.class) User user,
    		BindingResult bindingResult,
    		RedirectAttributes redirectAttributes) {
    	
    	String token = null;
    	
    	if (bindingResult.hasErrors()) {
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		
            return "redirect:register-first-phase";
    	}
    	else{
        	token = UUID.randomUUID().toString();
    		
    		if (mailService.sendConfirmationEmail(user.getEmail(), token) == false){
            	redirectAttributes.addFlashAttribute("exceptionMessage", "Ci sono stati dei problemi durante la registrazione. "
            			+ "Registrati.");
            	
                return "redirect:register-first-phase";
    		}
    		else{
    			registerNewUser(user);
        		
        		userService.saveVerificationToken(user.getId(), token);
        		
	        	redirectAttributes.addFlashAttribute("infoMessage", "Conferma il tuo account tramite la mail "
	        			+ "che abbiamo inviato sul tuo indirizzo di posta elettronica.");
            	
	        	return "redirect:login";
    		}
        }
    }
    
    @RequestMapping(value = "/register-second-phase", method = RequestMethod.GET)
    public String confirmRegistration(
    		@RequestParam String token,
    		Model model){
    	
    	if (!model.containsAttribute("org.springframework.validation.BindingResult.user")){
	    	User databaseUser = checkVerificationToken(token);
	    	
	    	if (databaseUser == null){
	    		model.addAttribute("dangerMessage", "Il tuo token non è valido. Registrati.");
	    		
	    		return "bad-verification";
	    	}
	    	
	    	model.addAttribute("token", token);
	    	model.addAttribute("user", databaseUser);
	    }

    	fillSecondPhaseModel(model);
    	
    	return "register-second-phase";
    }
    
    @RequestMapping(value = "/register-second-phase", method = RequestMethod.POST)
    public String postRegisterSecondPhase(
    		@RequestParam String token,
    		@RequestParam MultipartFile file,
    		@Validated(User.SecondPhaseValidation.class) User user,
    		BindingResult bindingResult,
    		RedirectAttributes redirectAttributes) {
    	
    	User databaseUser = checkVerificationToken(token);
    	
    	if (databaseUser == null){
    		redirectAttributes.addFlashAttribute("dangerMessage", "Il tuo token non è valido. Registrati.");
    		
    		return "redirect:bad-verification";
    	}
    	
    	if (bindingResult.hasErrors()) {
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
        	
            return "redirect:register-second-phase";
    	}
    	else{
    		byte[] image = parseImage(file);
    		
    		if (image == null){
                bindingResult.rejectValue("image", "user.registration.invalidImage");
                
        		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
        		redirectAttributes.addAttribute("token", token);
                
            	return "redirect:register-second-phase";
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
            updateNewUser(databaseUser);
        	
        	redirectAttributes.addAttribute("token", token);
        	redirectAttributes.addFlashAttribute("user", databaseUser);
        	
        	return "redirect:register-third-phase";
        }
    }
    
    @RequestMapping(value = "register-third-phase", method = RequestMethod.GET)
    public String getRegisterThirdPhase(
    		@RequestParam("token") String token,
    		Model model){
    	
    	if (!model.containsAttribute("org.springframework.validation.BindingResult.user")){
	    	User databaseUser = checkVerificationToken(token);
	    	
	    	if (databaseUser == null){
	    		model.addAttribute("dangerMessage", "Il tuo token non è valido. Registrati.");
	    		
	    		return "bad-verification";
	    	}
	    	
	    	model.addAttribute("token", token);
	    	model.addAttribute("user", databaseUser);
	    }

    	fillThirdPhaseModel(model);
    	
    	return "register-third-phase";
    }
    
    @RequestMapping(value = "/register-third-phase", method = RequestMethod.POST)
    public String postRegisterThirdPhase(
    		@RequestParam("token") String token,
    		@Validated(User.ThirdPhaseValidation.class) User user,
    		BindingResult bindingResult,
    		RedirectAttributes redirectAttributes) {
    	
    	User databaseUser = checkVerificationToken(token);
    	
    	if (databaseUser == null){
    		redirectAttributes.addFlashAttribute("dangerMessage", "Il tuo token non è valido. Registrati.");
    		
    		return "redirect:bad-verification";
    	}
    	
    	if (bindingResult.hasErrors()) {
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
        	
            return "redirect:register-third-phase";
    	}
    	else{
    		databaseUser.setOwnCar(user.getOwnCar());
    		databaseUser.setCarSharing(user.getCarSharing());
    		databaseUser.setBikeUsage(user.getBikeUsage());
    		databaseUser.setPubTransport(user.getPubTransport());
    		databaseUser.setEnabled(true);
            updateNewUser(databaseUser);
            
        	userService.removeVerificationToken(token);
        	
        	securityService.autologin(databaseUser.getUsername(), databaseUser.getConfirmedPassword());
        	
        	return "redirect:profile";
        }
    }
    
    @RequestMapping(value = "/bad-verification", method = RequestMethod.GET)
    public String getBadVerification() {
    	return "bad-verification";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(
    		Model model, 
    		String error, 
    		String logout) {
    	
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

    @RequestMapping(value = "/change-nickname", method = RequestMethod.POST)
    public String changeNickname(
    		@RequestParam(value = "new-nickname", required = true) String newNickname,
    		RedirectAttributes redirectAttributes){
    	
    	User currentUser = userService.findByEmail(securityService.findLoggedInUsername());
    	
    	if (newNickname.length() == 0){
    		redirectAttributes.addFlashAttribute("emptyNickname", "Il nickname deve essere di almeno 1 carattere.");
    	}
    	else if (newNickname.compareTo(currentUser.getNickname()) == 0){
    		redirectAttributes.addFlashAttribute("alreadyInUseNickname", "Scegliere un nickname diverso da quello già in uso.");
    	}
    	else{
    		userService.updateUserNewNickname(currentUser, newNickname);
    		
    		redirectAttributes.addFlashAttribute("updatedNickname", "Nickname cambiato.");
    	}
    	
    	return "redirect:profile";
    }

    @RequestMapping(value = "/change-password", method = RequestMethod.POST)
    public String changPassword(
    		@RequestParam(value = "old-password", required = true) String oldPassword,
    		@RequestParam(value = "new-password", required = true) String newPassword,
    		@RequestParam(value = "new-password-confirmed", required = true) String newPasswordConfirmed,
    		RedirectAttributes redirectAttributes){
    	
    	User currentUser = userService.findByEmail(securityService.findLoggedInUsername());
    	
    	if (oldPassword.length() == 0 || newPassword.length() == 0 || newPasswordConfirmed.length() == 0){
    		redirectAttributes.addFlashAttribute("emptyPassword", "Le password non possono essere vuote.");
    	}
    	else if (newPassword.compareTo(newPasswordConfirmed) != 0){
    		redirectAttributes.addFlashAttribute("mismatchPassword", "Le password non coincidono.");
    	}
    	else if (newPassword.length() < minPasswordLength){
    		redirectAttributes.addFlashAttribute("shortPassword", true);
    	}
    	else if (!userService.checkPassword(currentUser, oldPassword)){
    		redirectAttributes.addFlashAttribute("wrongOldPassword", true);
    	}
    	else if (oldPassword.compareTo(newPassword) == 0){
    		redirectAttributes.addFlashAttribute("samePassword", true);
    	}
    	else{
	    	userService.updateUserNewPassword(currentUser, newPassword);
	    	
	    	redirectAttributes.addFlashAttribute("updatedPassword", true);
    	}
    	
    	return "redirect:profile";
    }
    
    @RequestMapping(value = "/change-profile-picture", method = RequestMethod.POST)
    public String changProfilePicture(
    		@RequestParam MultipartFile file,
    		RedirectAttributes redirectAttributes){
    	
    	User currentUser = userService.findByEmail(securityService.findLoggedInUsername());
    	
    	byte[] image = parseImage(file);
		
		if (image == null){
			redirectAttributes.addFlashAttribute("notValidImage", "Immagine non valida.");
            
        	return "redirect:profile";
		}
		else{
			if (image.length > 0)
	        	currentUser.setImage(image);
			else
				currentUser.setImage(null);
		}
    	
        if (updateNewUser(currentUser) == false) 
        	redirectAttributes.addFlashAttribute("profilePictureUpdateProblems", "Si sono verificati dei problemi durante l'aggiornamento.");
        else
        	redirectAttributes.addFlashAttribute("successfullProfilePictureUpdate", "Immagine del profilo aggiornata.");
        
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
    
    private void fillSecondPhaseModel(Model model){
    	model.addAttribute("educationLevels", educationLevels);
    	model.addAttribute("jobs", jobs);
    }
    
    private void fillThirdPhaseModel(Model model){
    	model.addAttribute("fuelTypes", fuelTypes);
    	model.addAttribute("passTypes", passTypes);
    	model.addAttribute("carSharingServices", carSharingServices);
    }
}
