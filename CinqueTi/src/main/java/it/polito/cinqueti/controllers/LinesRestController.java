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
import it.polito.cinqueti.entities.BusLineStop;
import it.polito.cinqueti.entities.BusStop;
import it.polito.cinqueti.entities.DecodedAddress;
import it.polito.cinqueti.entities.Edge;
import it.polito.cinqueti.entities.Message;
import it.polito.cinqueti.entities.MinPath;
import it.polito.cinqueti.entities.Topic;
import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.errorhandlers.ResourceNotFoundException;
import it.polito.cinqueti.entities.OSMDecodedAddress;
import it.polito.cinqueti.services.BusStopService;
import it.polito.cinqueti.services.LineService;
import it.polito.cinqueti.services.MessageService;
import it.polito.cinqueti.services.PathService;
import it.polito.cinqueti.services.UserService;


@RestController
public class LinesRestController {
	
	@Autowired
	private LineService lineService;
	
	@Autowired
	private BusStopService busService;
	
	@Autowired
	private PathService pathService;
	
	
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
	
	@RequestMapping(value="/rest/stops", method=RequestMethod.GET)
	public List<BusStop> getStps()
	{	
		
		return busService.findAll();
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
	
	//used for getting bus lines of stop. Return only the ids of lines
	@RequestMapping(value="/rest/stops/{id}", method=RequestMethod.GET)
	public  List<String> getLinesForStop(@PathVariable String id)
	{	
		//Control if the resource with the passed id is present
		
		List<BusLineStop> busStopLines = busService.findById(id).getBusLines();
		if(busStopLines == null)
			throw new ResourceNotFoundException(id, "stop lines");
		
		List<String> lines = new ArrayList<String>();
		for(BusLineStop bls : busStopLines){
			
			//Do not consider duplicates(forward,backward). Example line 4
			//{stop: 248 sequence number 21, stop: 248 sequence number 61}
			if(!lines.contains(bls.getBusLine().getLine()))
				lines.add(bls.getBusLine().getLine());
		}
			
		return lines;
	}
	
	
	// method used to retrieve the best path from source to destination
	@RequestMapping(value="/rest/path", method=RequestMethod.GET)
	public List<Edge> getPath(
			@RequestParam("srcLat") Double srcLat,
			@RequestParam("srcLong") Double srcLong,
			@RequestParam("dstLat") Double dstLat,
			@RequestParam("dstLong") Double dstLong)
	{
		//return pathService.getPath(45.067397, 7.646947, 45.062079, 7.678479);
		
		return pathService.getPath(srcLat, srcLong, dstLat, dstLong);
		
	}
	
	@RequestMapping(value="/rest/address", method=RequestMethod.GET)
	public List<DecodedAddress> getAddresses(@RequestParam String address)
	{
		return lineService.getAddressInformation(address);
	}
}
