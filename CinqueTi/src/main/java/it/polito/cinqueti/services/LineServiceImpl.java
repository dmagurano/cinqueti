package it.polito.cinqueti.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import it.polito.cinqueti.entities.BusLine;
import it.polito.cinqueti.entities.OSMDecodedAddress;
import it.polito.cinqueti.entities.OSMDecodedAddressDetails;
import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.repositories.LineRepository;

@Service
public class LineServiceImpl implements LineService {

	@Autowired
    private LineRepository lineRepository;
	
	@Value("#{'${towns}'.split(',')}")
	private List<String> towns;
	
	@Override
	public List<BusLine> findAll() {
		
		return lineRepository.findAll();
	}

	@Override
	public BusLine findOne(String id) {
		
		return lineRepository.findOne(id);
	}

	@Override
	public List<OSMDecodedAddress> filterOSMResults(ArrayList<OSMDecodedAddress> results, String query) {
	
		ArrayList<OSMDecodedAddress> filtered = new ArrayList<OSMDecodedAddress>();
		for (OSMDecodedAddress res: results) {
			OSMDecodedAddressDetails details = res.getAddress();
			// skip the addresses that are not in Turin county
			if (details.getCounty().compareTo("TO") != 0)
				continue;
			// check if the string contains a town (and eventually save it)
			String town = null;
			for(String t: towns) {
				if (query.toLowerCase().contains(t))
				{
					town = t;
					break;
				}
			}
			
			String loc = details.getLocation().toLowerCase();
			//if town is not null we don't need to add results from turin
			if (town != null) {
				if (loc.contains(town) || town.contains(loc))
					filtered.add(res);
				continue;
			}
			// check if the result location is a valid city
			if (loc.equals("torino") || towns.contains(loc))
				filtered.add(res);
		}
		
		return filtered;
	}

	@Override
	public List<OSMDecodedAddress> getAddressInformation(String query) {
		RestTemplate restTemplate = new RestTemplate();
		OSMDecodedAddress[] resultsArray = restTemplate.getForObject(
				"https://nominatim.openstreetmap.org/search?q=" + query + " torino&county=to&addressdetails=1&format=json", 
				OSMDecodedAddress[].class);
		ArrayList<OSMDecodedAddress> results = new ArrayList<OSMDecodedAddress>(Arrays.asList(resultsArray));
		return filterOSMResults(results, query);
	}

	@Override
	public Page<BusLine> findAll(Integer page, Integer per_page) {
			// build a pageable object, used to specify the query paging params
				PageRequest pageReq = new PageRequest(page,per_page);
				// execute the query
				Page<BusLine> pageRes = lineRepository.findAll(pageReq);
				// if the user asks for a page X that is greater than the total number of pages in the db, return empty response
				if (page > pageRes.getTotalPages())
					return null;
				return pageRes;
	}

}
