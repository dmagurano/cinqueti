package it.polito.cinqueti.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.polito.cinqueti.entities.BusLine;
import it.polito.cinqueti.repositories.LineRepository;

@Service
public class LineServiceImpl implements LineService {

	@Autowired
    private LineRepository lineRepository;
	
	@Override
	public List<BusLine> findAll() {
		
		return lineRepository.findAll();
	}

}
