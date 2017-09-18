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
			if (userService.emailIsTaken(user.getEmail())){
				redirectAttributes.addFlashAttribute("exceptionMessage", "Indirizzo email già in uso. Cambiare indirizzo email.");
	        	
	        	return "redirect:register-first-phase";
			}
			
			token = UUID.randomUUID().toString();
			
    		userService.saveVerificationToken(user, token);
    		
    		if (mailService.sendConfirmationEmail(user.getEmail(), token) == false){
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
    		VerificationToken databaseToken = getDatabaseToken(token);
    		
    		if (databaseToken == null){
    			redirectAttributes.addFlashAttribute("exceptionMessage", "Il tuo token non è valido. Registrati.");
    		
    			return "redirect:bad-verification";
    		}
    		
	    	User databaseUser = databaseToken.getUser();
	    	
	    	model.addAttribute("user", databaseUser);
	    }
    	
    	model.addAttribute("token", token);

    	fillSecondPhaseModel(model);
    	
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
    	else if ( !educationLevels.contains(user.getEducation())){
    		bindingResult.rejectValue("education", "user.registration.invalidEducation");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
            
        	return "redirect:register-second-phase";
    	}
    	else if ( !jobs.contains(user.getJob())){
    		bindingResult.rejectValue("job", "user.registration.invalidJob");
            
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
    		
    		databaseToken.setUser(databaseUser);
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
	    	User databaseUser = getDatabaseToken(token).getUser();
	    	
	    	if (databaseUser == null){
	    		model.addAttribute("exceptionMessage", "Il tuo token non è valido. Registrati.");
	    		
	    		return "bad-verification";
	    	}
	    	
	    	model.addAttribute("user", databaseUser);
	    }
    	
    	model.addAttribute("token", token);

    	fillThirdPhaseModel(model);
    	
    	return "register-third-phase";
    }
    
    @RequestMapping(value = "/register-third-phase", method = RequestMethod.POST)
    public String postRegisterThirdPhase(
    		@RequestParam(required = true) String token,
    		@Validated(User.ThirdPhaseValidation.class) User user,
    		BindingResult bindingResult,
    		RedirectAttributes redirectAttributes) {
    	
    	// TODO
    	// error on car attributes -> uncheck -> error again on car attributes
    	
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
    	else if( user.getOwnCar().getRegistrationYear() != null && !fuelTypes.contains(user.getOwnCar().getFuelType())){
            bindingResult.rejectValue("ownCar.fuelType", "user.registration.invalidFuel");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
            
        	return "redirect:register-third-phase";
    	}
    	else if( user.getOwnCar().getFuelType() != null && user.getOwnCar().getRegistrationYear() == null ){
            bindingResult.rejectValue("ownCar.registrationYear", "NotNull");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
            
        	return "redirect:register-third-phase";
    	}
    	else if ( !carSharingServices.contains(user.getCarSharing()) ){
            bindingResult.rejectValue("carSharing", "user.registration.invalidCarSharing");
            
    		redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", bindingResult);
    		redirectAttributes.addAttribute("token", token);
            
        	return "redirect:register-third-phase";
    	}
    	else if ( !pubTransports.contains(user.getPubTransport()) ){
            bindingResult.rejectValue("pubTransport", "user.registration.invalidPubTransport");
            
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
        	
        	userService.saveUser(databaseUser);
            userService.removeVerificationToken(databaseToken);
        	
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
            model.addAttribute("error", "Your username and password is invalid.");

        // not used, as after logout user is redirected to home page
        //if (logout != null)
        //    model.addAttribute("message", "You have been logged out successfully.");

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
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Il nickname deve essere di almeno 1 carattere.");
    	}
    	else if (newNickname.compareTo(currentUser.getNickname()) == 0){
    		redirectAttributes.addFlashAttribute("exceptionMessage", "Scegliere un nickname diverso da quello già in uso.");
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
    	
    	byte[] image = parseImage(file);
		
		if (image == null){
			redirectAttributes.addFlashAttribute("exceptionMessage", "Immagine non valida.");
            
        	return "redirect:profile";
		}
		else{
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
    	model.addAttribute("passTypes", pubTransports);
    	model.addAttribute("carSharingServices", carSharingServices);
    }
}