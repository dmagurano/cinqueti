package it.polito.cinqueti.controllers;

import java.io.IOException;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.services.SecurityService;
import it.polito.cinqueti.services.UserService;


@Controller
public class TestController {
    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;
    
    @Value("${user.minPasswordLength}")
	Integer minPasswordLength;
    
    @Value("#{'${topics}'.split(',')}")
	private List<String> topics;
    
    @Value("#{'${user.educationLevels}'.split(',')}")
	private List<String> educationLevels;
    
    @Value("#{'${user.jobs}'.split(',')}")
	private List<String> jobs;
    
    @Value("#{'${user.carSharingServices}'.split(',')}")
	private List<String> carSharingServices;
    
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model, String error, String logout) {
        if (error != null)
            model.addAttribute("error", "Your username and password is invalid.");

        if (logout != null)
            model.addAttribute("message", "You have been logged out successfully.");

        return "login";
    }

    @RequestMapping(value = {"/", "/welcome"}, method = RequestMethod.GET)
    public String welcome(Model model) {
    	model.addAttribute("topics", topics);
        return "welcome";
    }
    
    @RequestMapping(value = {"/register"}, method = RequestMethod.GET)
    public String test(Model model) {
    	model.addAttribute("user", new User());
    	model.addAttribute("educationLevels", educationLevels);
    	model.addAttribute("jobs", jobs);
    	model.addAttribute("carSharingServices", carSharingServices);
        return "register";
    }
    
    @RequestMapping(value = {"/chat"}, method = RequestMethod.GET)
    public String chat(Model model) {
    	return "chat";
    }
    
    @RequestMapping(value = {"/profile"}, method = RequestMethod.GET)
    public String profile(Model model) {
    	
    	User currentUser = userService.findByEmail(securityService.findLoggedInUsername());
//    	User test = new User();
//    	test.setAge(11);
//    	test.setBikeUsage(new Bike(true,false));
//    	test.setCarSharing("Enjoy");
//    	test.setEducation("primary school");
//    	test.setEmail("pippo@a.it");
//    	test.setGender("male");
//    	test.setJob("no job");
//    	test.setNickname("Mr_Test");
//    	test.setOwnCar(new Car(1999,"diesel"));
//    	test.setPassword("ads");
//    	test.setPasswordConfirm("ads");
//    	test.setPubTransport("daily");
    	
//    	if (currentUser.getImage().length == 0)
//    		currentUser.setImage(null);
    		
    	model.addAttribute("user", currentUser);
        return "profile";
    }
    
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@ModelAttribute("user") @Valid User user, BindingResult result, @RequestParam MultipartFile file, Model model) {
    	
    	boolean registered = true;
    	
    	//Set image in class user to store it in db
    	try {
			user.setImage(file.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        if (!result.hasErrors()) {
            registered = createUserAccount(user, result);
        }
        if (registered == false) {
            result.rejectValue("email", "message.regError");
        }
        
        if (result.hasErrors()) {
        	model.addAttribute("educationLevels", educationLevels);
        	model.addAttribute("jobs", jobs);
        	model.addAttribute("carSharingServices", carSharingServices);
            return "register";
        }
        
        securityService.autologin(user.getUsername(), user.getPassword());

        return "redirect:profile";
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
    
    private boolean createUserAccount(User user, BindingResult result) {
        
        try {
            userService.registerNewUserAccount(user);
        } catch (Exception e) {
            return false;
        }    
        return true;
    }
}

