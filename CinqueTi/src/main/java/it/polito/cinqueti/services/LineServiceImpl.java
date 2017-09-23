package it.polito.cinqueti.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.polito.cinqueti.entities.ArcGisQueryResult;
import it.polito.cinqueti.entities.BusLine;
import it.polito.cinqueti.entities.DecodedAddress;
import it.polito.cinqueti.entities.DecodedAddressDetails;
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
	public List<DecodedAddress> getAddressInformation(String query) {
		RestTemplate restTemplate = new RestTemplate();
		String address = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates?SingleLine="
				+query 
				+ "&category=&outFields=ADDRESS,CITY,X,Y&forStorage=false&f=json&searchExtent=7.465761,44.948028,7.875002,45.163394&sourceCountry=ITA";
		// since ArcGis supports only "plaintext" as Mime Type, we need to convert it to string
		String json = restTemplate.getForObject(address, String.class);
		
		ObjectMapper mapper = new ObjectMapper();
		ArcGisQueryResult res;
		try {
		    //convert JSON string to object
		   res = mapper.readValue(json, new TypeReference<ArcGisQueryResult>(){});
		   return filterResults(res.getCandidatesAsList(), query);
		} catch (Exception e) {
			return null; 
		}
	}

	private List<DecodedAddress> filterResults(ArrayList<DecodedAddress> results, String query) {
		ArrayList<DecodedAddress> filtered = new ArrayList<DecodedAddress>();
		for (DecodedAddress res: results) {
			DecodedAddressDetails details = res.getAddress();
			
			String loc = details.getCity().toLowerCase();
			if(loc.equals("torino"))
			{
				filtered.add(0,res);
				continue;
			}
			boolean isInQuery = query.toLowerCase().contains(loc);
			boolean isInTowns = towns.contains(loc);
			if (!isInQuery && !isInTowns)
				continue;
			filtered.add(res);
		}
		
		return filtered;
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
