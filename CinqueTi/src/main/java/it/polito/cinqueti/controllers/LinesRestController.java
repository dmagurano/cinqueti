package it.polito.cinqueti.controllers;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import it.polito.cinqueti.entities.BusLine;
import it.polito.cinqueti.entities.BusLineStop;
import it.polito.cinqueti.entities.BusStop;
import it.polito.cinqueti.entities.Edge;
import it.polito.cinqueti.errorhandlers.ResourceNotFoundException;
import it.polito.cinqueti.services.BusStopService;
import it.polito.cinqueti.services.LineService;
import it.polito.cinqueti.services.PathService;



@RestController
public class LinesRestController {
	
	@Autowired
	private LineService lineService;
	
	@Autowired
	private BusStopService busService;
	
	@Autowired
	private PathService pathService;
	
	
	// read the default elementsPerPage from the application.properties file
	@Value("#{'${rest.elementsPerPage}'}")
	private Integer elementsPerPage;
	
	
	// method used to retrieve the paginated list of lines.
	//Parameters: page = page's number; per_page = page's size 
	@RequestMapping(value="/rest/lines", method=RequestMethod.GET)
	public List<BusLine> getLines(
			@RequestParam(required=false) Integer page, 
			@RequestParam(required=false) Integer per_page
			)
	{
		// if any parameter is missing, set it to the default value
		Page<BusLine> res;
		if (page == null)
			page = new Integer(0);
		if (per_page == null)
			per_page = elementsPerPage;
		
		// ask to the service to find the paged list of users
		res = lineService.findAll(page, per_page);
		if (res == null)
			return null;
		
		return res.getContent();
	}
	
	//method used to get all bus stops
	@RequestMapping(value="/rest/stops", method=RequestMethod.GET)
	public List<BusStop> getStps()
	{	
		
		return busService.findAll();
	}
	
	
	// method used to retrieve the single line details
	@RequestMapping(value="/rest/lines/{id}", method=RequestMethod.GET)
	public  BusLine getLineDetails(@PathVariable String id)
	{	
		//Control if the resource with the passed id is present, if not
		//present throw a NotFoundException
		
		BusLine line = lineService.findOne(id);
		if(line == null)
			throw new ResourceNotFoundException(id, "line");
			
		return line;
	}
	
	//used for getting bus lines that pass from that bus stop. 
	//Return only the ids of lines
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
		
		return pathService.getPath(srcLat, srcLong, dstLat, dstLong);
		
	}
		
}
