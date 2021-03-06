package it.polito.cinqueti.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DecodedAddress {
	
	@JsonProperty("address")
	private String display_name;
	@JsonProperty("score")
	private Float importance;
	@JsonProperty("attributes")
	private DecodedAddressDetails address;

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
	public DecodedAddressDetails getAddress() {
		return address;
	}
	public void setAddress(DecodedAddressDetails address) {
		this.address = address;
	}
	public DecodedAddress() {}
	
}
