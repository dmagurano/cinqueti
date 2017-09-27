package it.polito.cinqueti.services;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.client.model.Filters;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import it.polito.cinqueti.entities.Edge;
import it.polito.cinqueti.entities.GeoBusStop;
import it.polito.cinqueti.repositories.GeoBusStopRepository;
import it.polito.cinqueti.repositories.MinPathRepository;

@Service
public class PathServiceImpl implements PathService {

	@Autowired
	private MinPathRepository minPathRepository;
	
	@Autowired
	private GeoBusStopRepository geoBusStopRepository;
	
	private int ratio = 250; //used to search around 250 meters bus stops
	
	@Override
	public List<Edge> getPath(Double sLat, Double sLng, Double dLat, Double dLng) {
		
		//0) check the validity of the passed parameters
		if (sLng < -180.0 || sLng > 180.0 || sLat < -90.0 || sLat > 90.0 
				|| dLng < -180.0 || dLng > 180.0 || dLat < -90.0 || dLat > 90.0)
			return null;
		
		//1) search the stops around the source point
		String wktPoint = new String("POINT(" + sLng + " " + sLat + ")");
		Geometry point = wktToGeometry(wktPoint);
		List<GeoBusStop> src_gstops = geoBusStopRepository.getStopAroundSrc(point, ratio);
		
		//2) search the stops around the destination point
		wktPoint = new String("POINT(" + dLng + " " + dLat + ")");
 		point = wktToGeometry(wktPoint);
 		List<GeoBusStop> dst_gstops = geoBusStopRepository.getStopAroundDestination(point, ratio);
		
 		//3) search the best path and return all the edges of this path
 		
 		List<Bson> sFilters = new ArrayList<Bson>();
 	    for (GeoBusStop s: src_gstops)
 	    	sFilters.add(Filters.eq("idSource", "" + s.getId()));
 	    // add the dst conditions
 	    List<Bson> dFilters = new ArrayList<Bson>();
 	    for (GeoBusStop d: dst_gstops)
 	    	dFilters.add(Filters.eq("idDestination", "" + d.getId()));
 	   List<Edge> edges = minPathRepository.calculateMinPath(sFilters, dFilters).getEdges();
 	    
		return edges;
		
	}
	
	//Function for convertion
	private Geometry wktToGeometry(String wktPoint) {
        WKTReader fromText = new WKTReader();
        Geometry geom = null;
        try {
            geom = fromText.read(wktPoint);
        } catch (ParseException e) {
            throw new RuntimeException("Not a WKT string:" + wktPoint);
        }
        return geom;
    }

}
