package it.polito.cinqueti.repositories;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import it.polito.cinqueti.entities.BusStop;
import it.polito.cinqueti.entities.Edge;
import it.polito.cinqueti.entities.MinPath;



public class MinPathRepositoryImpl implements MinPathRepositoryCustom {

	@Autowired
	private MongoDatabase db;
	
	@Autowired
	private BusStopRepository busStopRepository;

 
	
	@Override
	public MinPath calculateMinPath(List<Bson> sFilters,List<Bson> dFilters) {
		
		MongoCollection<Document> mc = db.getCollection("MinPaths");
		
		Bson sort = Sorts.ascending("totalCost");
		// compose the final filter
		Bson resFilter = Filters.and(Filters.or(sFilters), Filters.or(dFilters));
		
		
		Document best_doc = mc.find(resFilter).sort(sort).limit(1).into(new ArrayList<Document>()).get(0);
	
		MinPath minP = new MinPath();
		minP.setIdSource(best_doc.getString("idSource"));
		minP.setIdDestination(best_doc.getString("idDestination"));
		
		List<Document> edges_docs = (List<Document>) best_doc.get("edges");
 	    List<Edge> edges = new ArrayList<Edge>();
 	    for(Document d: edges_docs)
 	    {
 	    	
 	    	String idDst = d.getString("idDestination");
 	    	String idSrc = d.getString("idSource");
 	    	
 	    	BusStop bsSrc = busStopRepository.findById(idSrc);
 	    	BusStop bsDst = busStopRepository.findById(idDst);
 	    	
 	        Edge edge = new Edge();
 	    	edge.setCost(d.getInteger("cost"));
 	    	edge.setEdgeLine(d.getString("line"));
 	    	edge.setIdDestination(idDst);
 	    	edge.setIdSource(idSrc);
 	    	edge.setLatSrc(bsSrc.getLat());
 	    	edge.setLonSrc(bsSrc.getLng());
 	    	edge.setLatDst(bsDst.getLat());
 	    	edge.setLonDst(bsDst.getLng());
 	    	edge.setMode(d.getBoolean("mode"));
 	    	edges.add(edge);
 	    }
		
 	    minP.setEdges(edges);
 	    
		return minP;
		
	
	}

}
