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
		/*List<BusLine> l = lineService.findAll();
		List<BusLine> bl = new ArrayList<BusLine>();
		int i = 0;
		for(BusLine lin: l){
			bl.add(lin);
			i++;
			if(i == 40)
				break;
		}*/
		return lineService.findAll();
	}
	
	
	// method used to retrieve the single line details
	@RequestMapping(value="/rest/lines/{id}", method=RequestMethod.GET)
	public  BusLine getLineDetails(@PathVariable String id)
	{	
		//Control if the resource with the passed id is present
		
		BusLine line = lineService.findOne(id);
		if(line == null)
			throw new ResourceNotFoundException(id, "line");
			
		return line;
	}
	
	// method used to retrieve the best path from source to destination
	@RequestMapping(value="/rest/path/", method=RequestMethod.GET)
	public HttpEntity<PagedResources<Message>> getTopicMessages(
			@RequestParam("src") String src,
			@RequestParam("dst") String dst)
	{
		return null;
		
	}
}