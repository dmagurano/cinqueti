package it.polito.cinqueti.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import it.polito.cinqueti.entities.BusLine;
import it.polito.cinqueti.entities.OSMDecodedAddress;

public interface LineService {

	List<BusLine> findAll();
	Page<BusLine> findAll(Integer page, Integer per_page);
	BusLine findOne(String id);
	List<OSMDecodedAddress> getAddressInformation(String query);
	List<OSMDecodedAddress> filterOSMResults(ArrayList<OSMDecodedAddress> results, String query);
}
