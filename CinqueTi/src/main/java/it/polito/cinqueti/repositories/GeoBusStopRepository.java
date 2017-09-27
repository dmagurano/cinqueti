package it.polito.cinqueti.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.vividsolutions.jts.geom.Geometry;
import it.polito.cinqueti.entities.GeoBusStop;

public interface GeoBusStopRepository extends CrudRepository<GeoBusStop, String> {

	//Get bus stops around the source. Ratio is the radius
	@Query("SELECT gbs FROM GeoBusStop gbs WHERE ST_DWithin(location,?1,?2) = true")
	List<GeoBusStop> getStopAroundSrc(Geometry src,Integer ratio);
	
	//Get bus stops around the destination
	@Query("SELECT gbs FROM GeoBusStop gbs WHERE ST_DWithin(location,?1,?2) = true")
	List<GeoBusStop> getStopAroundDestination(Geometry destination,Integer ratio);
	
}

