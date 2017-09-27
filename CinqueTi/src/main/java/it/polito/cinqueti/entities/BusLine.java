package it.polito.cinqueti.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
@Table(name="busline")
public class BusLine{

	

	@Id
	private String line;
	
	@Column
	private String description;
		
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "busLine")
	private List<BusLineStop> busStops = new ArrayList<BusLineStop>();
	/*
		Hibernate requires that persistent collection-valued fields be declared as an interface type. The actual interface might be java.util.Set, java.util.Collection, java.util.List, java.util.Map, java.util.SortedSet, java.util.SortedMap or anything you like ("anything you like" means you will have to write an implementation of org.hibernate.usertype.UserCollectionType.)
	 */
	
	

	
	public String getLine() {
		try{
			return line;
		} catch (Exception e){
			e.printStackTrace();
			return line;
		}
	}
	
	public void setLine(String line) {
		this.line = line;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<BusLineStop> getBusStops() {
		return busStops;
	}
	
	public void setBusStops(ArrayList<BusLineStop> busstops) {
		this.busStops = busstops;
	}
	
	@Override
	public String toString(){
		return line + " " + description;
	}
}
