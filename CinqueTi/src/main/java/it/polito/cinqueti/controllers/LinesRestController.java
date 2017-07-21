package it.polito.cinqueti.controllers;

import java.util.ArrayList;
import java.util.List;

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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import it.polito.cinqueti.entities.BusLine;
import it.polito.cinqueti.entities.Message;
import it.polito.cinqueti.entities.Topic;
import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.errorhandlers.ResourceNotFoundException;
import it.polito.cinqueti.services.LineService;
import it.polito.cinqueti.services.MessageService;
import it.polito.cinqueti.services.UserService;


@RestController
public class LinesRestController {
	
	@Autowired
	private LineService lineService;
	
	
	// read both topics and the default elementsPerPage from the application.properties file
	@Value("#{'${topics}'.split(',')}")
	private List<String> topics;
	@Value("#{'${rest.elementsPerPage}'}")
	private Integer elementsPerPage;
	
	// method used to retrieve the paginated list of lines
	@RequestMapping(value="/rest/lines", method=RequestMethod.GET)
	public List<BusLine> getLines()
	{	
		List<BusLine> l = lineService.findAll();
		List<BusLine> bl = new ArrayList<BusLine>();
		int i = 0;
		for(BusLine lin: l){
			bl.add(lin);
			i++;
			if(i == 1)
				break;
		}
		return bl;
	}
	
	
	// method used to retrieve the single line details
	@RequestMapping(value="/rest/lines/{id}", method=RequestMethod.GET)
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
	
	// method used to retrieve the best path from source to destination
	@RequestMapping(value="/rest/path/", method=RequestMethod.GET)
	public HttpEntity<PagedResources<Message>> getTopicMessages(
			@PathVariable String src,
			@PathVariable String dst)
	{
		return null;
		
	}
}
