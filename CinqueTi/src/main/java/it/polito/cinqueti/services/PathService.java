package it.polito.cinqueti.services;

import java.util.List;

import it.polito.cinqueti.entities.Edge;

public interface PathService {

	public List<Edge> getPath(Double sLat, Double sLng, Double dLat, Double dLng); 
}

