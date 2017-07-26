package it.polito.cinqueti.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OSMDecodedAddress {
	private String place_id;
	private Double lat;
	private Double lon;
	private String display_name;
	private Float importance;
	private OSMDecodedAddressDetails address;
	
	public String getPlace_id() {
		return place_id;
	}
	public void setPlace_id(String place_id) {
		this.place_id = place_id;
	}
	public Double getLat() {
		return lat;
	}
	public void setLat(Double lat) {
		this.lat = lat;
	}
	public Double getLon() {
		return lon;
	}
	public void setLon(Double lon) {
		this.lon = lon;
	}
	public String getDisplay_name() {
		return display_name;
	}
	public void setDisplay_name(String display_name) {
		this.display_name = display_name;
	}
	public Float getImportance() {
		return importance;
	}
	public void setImportance(Float importance) {
		this.importance = importance;
	}
	public OSMDecodedAddressDetails getAddress() {
		return address;
	}
	public void setAddress(OSMDecodedAddressDetails address) {
		this.address = address;
	}
	
	

	
	

}
