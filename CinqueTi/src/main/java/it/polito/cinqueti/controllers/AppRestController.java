package it.polito.cinqueti.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.ServletContextAware;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import it.polito.cinqueti.entities.Message;
import it.polito.cinqueti.entities.Topic;
import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.errorhandlers.ResourceNotFoundException;
import it.polito.cinqueti.services.MessageService;
import it.polito.cinqueti.services.UserService;


@RestController
public class AppRestController implements ServletContextAware {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private MessageService messageService;
	
	ServletContext servletContext;
	
	// read both topics and the default elementsPerPage from the application.properties file
	@Value("#{'${topics}'.split(',')}")
	private List<String> topics;
	@Value("#{'${rest.elementsPerPage}'}")
	private Integer elementsPerPage;
	
	// read the alert types from application.properties file
	@Value("#{'${alerts.types}'.split(',')}")
	private List<String> alertTypes;
	
	private byte[] defaultPicture = null;
	
	// method used to retrieve the (paginated and anonymized) list of users
	@RequestMapping(value="/rest/users", method=RequestMethod.GET)
	public HttpEntity<PagedResources<User>> getUsers(
			@RequestParam(required=false) Integer page, 
			@RequestParam(required=false) Integer size,
			PagedResourcesAssembler assembler)
	{
		// if any param is missing, set it to the default value
		Page<User> res;
		if (page == null)
			page = new Integer(0);
		if (size == null)
			size = elementsPerPage;
		// ask the service to find the paged list of users
		res = userService.findAll(page, size);
		if (res == null)
			return null;
		// return the result. The conversion to json will be done automatically. 
		// The User class is annotated with the JsonIgnore annotation on the attributes that should be excluded (like username, password etc)
		// The assembler is required in order to generate the "self", "next", "first" REST urls. 
		// This solution allows to navigate between the page with the given urls
		return new ResponseEntity<>(assembler.toResource(res), HttpStatus.OK);
	}
	
	// method used to retrieve the list of available topics
	@RequestMapping(value="/rest/topics", method=RequestMethod.GET)
	public List<Topic> getTopics()
	{	
		List<Topic> topicList = new ArrayList<Topic>();
		// we have no topic service, so we decided to simply add an attribute here
		for(String topic : topics){
			Topic t = new Topic(topic);
			// let's produce the rest url to navigate to the topic messages
			t.add(linkTo(methodOn(AppRestController.class).getTopics()).slash(topic).slash("messages").withSelfRel());
			topicList.add(t);
		}
	
		return topicList;
	}
	
	// method used to retrieve the (paginated) list of messages of the given topic
	@RequestMapping(value="/rest/topics/{id}/messages", method=RequestMethod.GET)
	public HttpEntity<PagedResources<Message>> getTopicMessages(
			@PathVariable String id,
			@RequestParam(required=false) Integer page, 
			@RequestParam(required=false) Integer size,
			PagedResourcesAssembler assembler)
	{
		// id = the selected topic
		// if the topic does not exists return a 404 error
		if(!topics.contains(id))
			throw new ResourceNotFoundException(id, "topic");
		
		Page<Message> res;
		// if any param is missing, set it to the default value
		if (page == null)
			page = new Integer(0);
		if (size == null)
			size = elementsPerPage;
		// ask the service to find the paged list of messages
		res = messageService.findByTopic(id, page, size);
		if (res == null)
			return null;
		// The assembler is required in order to generate the "self", "next", "first" REST urls. 
		// This solution allows to navigate between the page with the given urls
		return new ResponseEntity<>(assembler.toResource(res), HttpStatus.OK);
	}
	
	// TODO move to a better controller
	@RequestMapping(value="/rest/users/{nickname}/image", method=RequestMethod.GET)
	public byte[] getUserImage(@PathVariable(required=true) String nickname)
	{
		
		try{
			return userService.findByNickname(nickname).getImage();
		}
		catch (NullPointerException e)
		{
			if(defaultPicture == null)
			{
				InputStream inputStream = servletContext.getResourceAsStream("/static/assets/default-profile.jpg");
				try {
					IOUtils.readFully(inputStream, defaultPicture);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			return defaultPicture;
		}
	}
	
	// TODO move to a better controller
	@RequestMapping(value="/rest/alerttypes", method=RequestMethod.GET)
	public List<String> getAlertTypes()
	{
		return alertTypes;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
}
