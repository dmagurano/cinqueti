package it.polito.cinqueti.repositories;

import java.util.List;

import org.bson.conversions.Bson;

import it.polito.cinqueti.entities.MinPath;


public interface MinPathRepositoryCustom {

	public MinPath calculateMinPath(List<Bson> sFilters, List<Bson> dFilters);
}

