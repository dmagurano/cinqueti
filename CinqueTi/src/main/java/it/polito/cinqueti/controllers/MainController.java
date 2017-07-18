package it.polito.cinqueti.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class MainController {
    
    @Value("#{'${topics}'.split(',')}")
	private List<String> topics;

    @RequestMapping(value = {"/", "/welcome"}, method = RequestMethod.GET)
    public String welcome(Model model) {
    	model.addAttribute("topics", topics);
    	
        return "welcome";
    }
    
    @RequestMapping(value = {"/chat"}, method = RequestMethod.GET)
    public String chat(Model model) {
    	return "chat";
    }
    
    // TEST
    @RequestMapping(value = {"/base"}, method = RequestMethod.GET)
    public String base(Model model) {
        return "base";
    }
    
    // TEST
    @RequestMapping(value = {"/base-welcome"}, method = RequestMethod.GET)
    public String baseWelcome(Model model) {
    	model.addAttribute("topics", topics);
    	
    	model.addAttribute("view", "base-welcome");
    	
        return "base";
    }
    
    // TEST
    @RequestMapping(value = {"/base-login"}, method = RequestMethod.GET)
    public String baseLogin(Model model) {
    	model.addAttribute("topics", topics);
    	
    	model.addAttribute("view", "base-login");
    	
        return "base";
    }
    
}