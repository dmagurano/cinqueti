package it.polito.cinqueti.entities;


import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="busstop")
public class BusStop {
	
	@Id
	private String id;
	
	@Column(nullable=false)
	private String name;
	
	@Column(nullable=false)
	private double lat;
	
	@Column(nullable=false)
	private double lng;
		
	@OneToMany(cascade = CascadeType.ALL, mappedBy="busStop")
	private List<BusLineStop> busLines = new ArrayList<BusLineStop>();
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public double getLat() {
		return lat;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public double getLng() {
		return lng;
	}
	
	public void setLng(double lng) {
		this.lng = lng;
	}
	
	@JsonIgnore
	public List<BusLineStop> getBusLines() {
		return busLines;
	}
	
	public void setBusLines(ArrayList<BusLineStop> busLines) {
		this.busLines = busLines;
	}
	
	@Override
	public String toString(){
		return id + " " + name + " [" + lat + " - " + lng + "]";
	}
}

