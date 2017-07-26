package it.polito.cinqueti.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.polito.cinqueti.entities.BusStop;
import it.polito.cinqueti.repositories.BusStopRepository;

@Service
public class BusStopServiceImpl implements BusStopService{

	@Autowired
	private BusStopRepository busStopRepo;

	@Override
	public BusStop findById(String id) {
		return busStopRepo.findById(id);
	}

	@Override
	public List<BusStop> findAll() {
		
		return busStopRepo.findAll();
	}
	
	
	
}
