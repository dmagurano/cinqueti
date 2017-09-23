package it.polito.cinqueti.entities;

import java.util.ArrayList;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArcGisQueryResult {
	private DecodedAddress[] candidates;

	public DecodedAddress[] getCandidates() {
		return candidates;
	}
	
	@JsonIgnore
	public ArrayList<DecodedAddress> getCandidatesAsList() {
		return new ArrayList<DecodedAddress>(Arrays.asList(candidates));
	}

	public void setCandidates(DecodedAddress[] candidates) {
		this.candidates = candidates;
	}

	public ArcGisQueryResult(DecodedAddress[] candidates) {
		super();
		this.candidates = candidates;
	}
	
	public ArcGisQueryResult() {  }
}
