package it.polito.cinqueti.entities;

import java.io.Serializable;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="buslinestop")
public class BusLineStop implements Serializable {
	
	@Column(nullable=false)
	private Short seqencenumber;

	@Id	
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name="lineId")
	
	private BusLine busLine;
	
	@Id
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name="stopId")
	private BusStop busStop;
	
	@JsonIgnore
	public BusLine getBusLine() {
		return busLine;
	}

	public void setBusLine(BusLine busLine) {
		this.busLine = busLine;
	}

	
	public BusStop getBusStop() {
		return busStop;
	}

	public void setBusStop(BusStop busStop) {
		this.busStop = busStop;
	}

	public Short getSeqenceNumber() {
		return seqencenumber;
	}

	public void setSeqenceNumber(Short seqenceNumber) {
		this.seqencenumber = seqenceNumber;
	}
}
