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
    
    @Value("#{'${user.pubTransport}'.split(',')}")
	private List<String> pubTransports;
    
    // two phase registration
    // http://blog.codeleak.pl/2014/08/validation-groups-in-spring-mvc.html
    
    @RequestMapping(value = {"/register-first-phase"}, method = RequestMethod.GET)
    public String getRegister(Model model) {
    	if (!model.containsAttribute("org.springframework.validation.BindingResult.user"))
        	// the request is not a redirect
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
    		// email already in user or in verificationToken collection
			if (userService.emailIsTaken(user.getEmail())){
				redirectAttributes.addFlashAttribute("exceptionMessage", "Indirizzo email già in uso. Cambiare indirizzo email.");
	        	
	        	return "redirect:register-first-phase";
			}
			
			// generate random token
			token = UUID.randomUUID().toString();
    		userService.saveVerificationToken(user, token);
    		
    		if (mailService.sendConfirmationEmail(user.getEmail(), token) == false){
    			// if sending the email fails, we clear verificationToken
    			userService.removeVerificationToken(userService.getVerificationToken(token));
    			
            	redirectAttributes.addFlashAttribute("exceptionMessage", "Ci sono stati dei problemi "
            			+ "durante l'invio dell'email di conferma. Registrati.");
            	
                return "redirect:register-first-phase";
    		}
    		
        	redirectAttributes.addFlashAttribute("infoMessage", "Conferma il tuo account tramite l'email "
        			+ "che abbiamo inviato sul tuo indirizzo di posta elettronica.");
        	
        	return "redirect:login";
        }
    }
    
    @RequestMapping(value = "/register-second-phase", method = RequestMethod.GET)
    public String confirmRegistration(
    		@RequestParam(required = true) String token,
    		Model model,
    		RedirectAttributes redirectAttributes){
    	
    	if (!model.containsAttribute("org.springframework.validation.BindingResult.user")){
        	// the request is not a redirect
    		VerificationToken databaseToken = getDatabaseToken(token);
    		
    		if (databaseToken == null){
    			redirectAttributes.addFlashAttribute("exceptionMessage", "Il tuo token non è valido. Registrati.");
    		
    			return "redirect:bad-verification";
    		}
    		
	    	User databaseUser = databaseToken.getUser();
	    	
	    	model.addAttribute("user", databaseUser);
	    }
    	
    	model.addAttribute("token", token);

    	model.addAttribute("educationLevels", educationLevels);
    	model.addAttribute("jobs", jobs);
    	
    	return "register-second-phase";
    }
    
    @RequestMapping(value = "/register-second-phase", method = RequestMethod.POST)
    public String postRegisterSecondPhase(
    		@RequestParam(required = true) String token,
    		@RequestParam MultipartFile file,
    		@Validated(User.SecondPhaseValidation.class) User user,
    		BindingResult bindingResult,
    		RedirectAttributes redirectAttributes) {
    	
    	VerificationToken databaseToken = getDatabaseToken(token);
    	
    	if (databaseToken == null){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Il tuo token non è valido. Registrati.");
    		
    		return "redirect:bad-verification";
    	}

    	User databaseUser = databaseToken.getUser();
    	
    	if (bindingResult.hasErrors()) {
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
        	
            return "redirect:register-second-phase";
    	}
    	else if(userService.findByNickname(user.getNickname()) != null){
    		// new nickname is used by another user
    		bindingResult.rejectValue("nickname", "user.registration.nicknameAlreadyInUse");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
        	
            return "redirect:register-second-phase";
    	}
    	else if ( !educationLevels.contains(user.getEducation())){
    		// invalid education
    		bindingResult.rejectValue("education", "user.registration.invalidEducation");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
            
        	return "redirect:register-second-phase";
    	}
    	else if ( !jobs.contains(user.getJob())){
    		// invalid job
    		bindingResult.rejectValue("job", "user.registration.invalidJob");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
            
        	return "redirect:register-second-phase";
    	}
    	else{
    		byte[] image = parseImage(file);
    		
    		if (image == null){
        		// file is not an image
                bindingResult.rejectValue("image", "user.registration.invalidImage");
                
        		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
        		redirectAttributes.addAttribute("token", token);
                
            	return "redirect:register-second-phase";
    		}
    		else{
    			// file is an image or is empty
    			if (image.length > 0)
            		databaseUser.setImage(image);
    		}

    		databaseUser.setNickname(user.getNickname());
    		databaseUser.setGender(user.getGender());
    		databaseUser.setAge(user.getAge());
    		databaseUser.setEducation(user.getEducation());
    		databaseUser.setJob(user.getJob());
    		
    		databaseToken.setUser(databaseUser);
    		
    		// updating verification token on database
            userService.updateVerificationToken(databaseToken);
        	
        	redirectAttributes.addAttribute("token", token);
        	redirectAttributes.addFlashAttribute("user", databaseUser);
        	
        	return "redirect:register-third-phase";
        }
    }
    
    @RequestMapping(value = "register-third-phase", method = RequestMethod.GET)
    public String getRegisterThirdPhase(
    		@RequestParam(required = true) String token,
    		Model model){
    	
    	if (!model.containsAttribute("org.springframework.validation.BindingResult.user")){
        	// the request is not a redirect
	    	User databaseUser = getDatabaseToken(token).getUser();
	    	
	    	if (databaseUser == null){
	    		model.addAttribute("exceptionMessage", "Il tuo token non è valido. Registrati.");
	    		
	    		return "bad-verification";
	    	}
	    	
	    	model.addAttribute("user", databaseUser);
	    }
    	
    	model.addAttribute("token", token);

    	model.addAttribute("fuelTypes", fuelTypes);
    	model.addAttribute("passTypes", pubTransports);
    	model.addAttribute("carSharingServices", carSharingServices);
    	
    	return "register-third-phase";
    }
    
    @RequestMapping(value = "/register-third-phase", method = RequestMethod.POST)
    public String postRegisterThirdPhase(
    		@RequestParam(required = true) String token,
    		@Validated(User.ThirdPhaseValidation.class) User user,
    		BindingResult bindingResult,
    		RedirectAttributes redirectAttributes) {
    	
    	VerificationToken databaseToken = getDatabaseToken(token);
    	
    	if (databaseToken == null){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Il tuo token non è valido. Registrati.");
    		
    		return "redirect:bad-verification";
    	}

    	User databaseUser = databaseToken.getUser();
    	
    	if (bindingResult.hasErrors()) {
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
        	
            return "redirect:register-third-phase";
    	}
    	else if( !(user.getCarYear() == null && user.getCarFuel().isEmpty()) ){
    		// car attributes are present, validating
    		boolean valid = true;
    		
    		if (user.getCarYear() == null){    		
	            bindingResult.rejectValue("carYear", "NotNull");
	            valid = false;
    		}
    		else if (user.getCarFuel() == null){    		
	            bindingResult.rejectValue("carFuel", "NotNull");
	            valid = false;
    		}
    		else if(user.getCarFuel().isEmpty()){
    			bindingResult.rejectValue("carFuel", "NotEmpty");
	            valid = false;
    		}
    		else if(user.getCarYear() < 1900 || user.getCarYear() > 2017){
    			bindingResult.rejectValue("carYear", "user.registration.invalidCarYear");
	            valid = false;
    		}
    		else if(!fuelTypes.contains(user.getCarFuel())){
    			bindingResult.rejectValue("carFuel", "user.registration.invalidCarFuel");
	            valid = false;
    		}
            
    		if ( !valid ){
	    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
	    		redirectAttributes.addAttribute("token", token);
	            
	        	return "redirect:register-third-phase";
    		}
    	}
    	
    	if ( !carSharingServices.contains(user.getCarSharing()) ){
    		//invalid car sharing service
            bindingResult.rejectValue("carSharing", "user.registration.invalidCarSharing");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
            
        	return "redirect:register-third-phase";
    	}
    	else if ( !pubTransports.contains(user.getPubTransport()) ){
    		//invalid public transport
            bindingResult.rejectValue("pubTransport", "user.registration.invalidPubTransport");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
            
        	return "redirect:register-third-phase";
    	}
    	else{
    		databaseUser.setCarFuel(user.getCarFuel());
    		databaseUser.setCarYear(user.getCarYear());
    		databaseUser.setCarSharing(user.getCarSharing());
    		databaseUser.setBikeUsage(user.getBikeUsage());
    		databaseUser.setPubTransport(user.getPubTransport());
    		databaseUser.setEnabled(true);
        	
    		// saving user
        	userService.saveUser(databaseUser);
        	// removing verification token
            userService.removeVerificationToken(databaseToken);
        	
            // auto login for registered user
        	securityService.autologin(databaseUser.getUsername(), databaseUser.getConfirmedPassword());
        	
        	redirectAttributes.addFlashAttribute("successMessage", "Registrazione completata con successo.");
        	
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
            model.addAttribute("error", "Credenziali non valide.");

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
    	
    	// validating nickname
    	if (newNickname.length() == 0){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Il nickname deve essere di almeno 1 carattere.");
    	}
    	else if (newNickname.compareTo(currentUser.getNickname()) == 0){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Scegliere un nickname diverso da quello già in uso.");
    	}
    	else if(userService.findByNickname(newNickname) != null){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Nickname utilizzato da un altro utente.");
    	}
    	else{
    		userService.updateUserNewNickname(currentUser, newNickname);
    		
    		redirectAttributes.addFlashAttribute("successMessage", "Nickname cambiato con successo.");
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
    	
    	// validating old and new password
    	if (oldPassword.length() == 0 || newPassword.length() == 0 || newPasswordConfirmed.length() == 0){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Le password non possono essere vuote.");
    	}
    	else if (newPassword.compareTo(newPasswordConfirmed) != 0){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Le password non coincidono.");
    	}
    	else if (newPassword.length() < minPasswordLength){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "La nuova password deve essere di almeno 8 caratteri.");
    	}
    	else if (!userService.checkPassword(currentUser, oldPassword)){
    		redirectAttributes.addFlashAttribute("dangerMessage", "La vecchia password è errata.");
    	}
    	else if (oldPassword.compareTo(newPassword) == 0){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "La nuova password non può essere uguale alla vecchia.");
    	}
    	else{
	    	userService.updateUserNewPassword(currentUser, newPassword);
	    	
	    	redirectAttributes.addFlashAttribute("successMessage", "Password aggiornata.");
    	}
    	
    	return "redirect:profile";
    }
    
    @RequestMapping(value = "/change-profile-picture", method = RequestMethod.POST)
    public String changProfilePicture(
    		@RequestParam MultipartFile file,
    		RedirectAttributes redirectAttributes){
    	
    	User currentUser = userService.findByEmail(securityService.findLoggedInUsername());
    	
    	// validating image
    	byte[] image = parseImage(file);
		
		if (image == null){
			// invalid image
			redirectAttributes.addFlashAttribute("exceptionMessage", "Immagine non valida.");
            
        	return "redirect:profile";
		}
		else{
			// valid or empty image
			if (image.length > 0)
	        	currentUser.setImage(image);
			else
				currentUser.setImage(null);
		}
    	
        userService.saveUser(currentUser);
        
        redirectAttributes.addFlashAttribute("successMessage", "Immagine del profilo aggiornata.");
        
        return "redirect:profile";
    }
    
    private VerificationToken getDatabaseToken(String token){
    	// retrieve token from database
    	VerificationToken databaseToken = userService.getVerificationToken(token);
    	
    	if ( databaseToken == null)
    		return null;
    	
    	if ( databaseToken.isExpired() ){
    		userService.removeVerificationToken(databaseToken);
    		
    		return null;
    	}
    	
    	return databaseToken;
    }
    
    private byte[] parseImage(MultipartFile multipartFile){
    	String mimeType = null;
    	byte[] image = null;
    	
    	// file is empty
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
}