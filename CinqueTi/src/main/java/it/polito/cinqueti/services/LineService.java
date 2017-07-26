package it.polito.cinqueti.services;

import java.util.ArrayList;
import java.util.List;

import it.polito.cinqueti.entities.BusLine;
import it.polito.cinqueti.entities.OSMDecodedAddress;

public interface LineService {

	List<BusLine> findAll();
	BusLine findOne(String id);
	List<OSMDecodedAddress> getAddressInformation(String query);
	List<OSMDecodedAddress> filterOSMResults(ArrayList<OSMDecodedAddress> results, String query);
}
