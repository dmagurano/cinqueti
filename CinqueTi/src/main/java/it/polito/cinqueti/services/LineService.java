package it.polito.cinqueti.services;

import java.util.ArrayList;
import java.util.List;

import it.polito.cinqueti.entities.BusLine;
import it.polito.cinqueti.entities.DecodedAddress;
import it.polito.cinqueti.entities.OSMDecodedAddress;

public interface LineService {

	List<BusLine> findAll();
	BusLine findOne(String id);
	List<DecodedAddress> getAddressInformation(String query);
}
