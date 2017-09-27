package it.polito.cinqueti.entities;

/**
 * Questa classe non rappresenta l'arco di un grafo,
 * ma una tratta di un percorso.
 * */
public class Edge {
	
	/**
	 * Id della fermata (BusStop) di partenza
	 * */
	private String idSource;
	
	/**
	 * Id della fermata (BusStop) di arrivo
	 * */
	private String idDestination;
	
	private double latSrc;
	private double lonSrc;
	private double latDst;
	private double lonDst;
	
	/**
	 * Modo di spostamento
	 * 0 -> bus
	 * 1 -> a piedi
	 * */
	private boolean mode;
	
	/**
	 * Costo dell'arco
	 * */
	private int cost;
	
	private String edgeLine;
	
	
	public String getIdSource() {
		return idSource;
	}
	public void setIdSource(String idSource) {
		this.idSource = idSource;
	}
	public String getIdDestination() {
		return idDestination;
	}
	public void setIdDestination(String idDestination) {
		this.idDestination = idDestination;
	}
	public boolean isMode() {
		return mode;
	}
	public void setMode(boolean mode) {
		this.mode = mode;
	}
	public int getCost() {
		return cost;
	}
	public void setCost(int cost) {
		this.cost = cost;
	}
	public String getEdgeLine() {
		return edgeLine;
	}
	public void setEdgeLine(String edgeLine) {
		this.edgeLine = edgeLine;
	}
	public double getLatSrc() {
		return latSrc;
	}
	public void setLatSrc(double latSrc) {
		this.latSrc = latSrc;
	}
	public double getLonSrc() {
		return lonSrc;
	}
	public void setLonSrc(double lonSrc) {
		this.lonSrc = lonSrc;
	}
	public double getLatDst() {
		return latDst;
	}
	public void setLatDst(double latDst) {
		this.latDst = latDst;
	}
	public double getLonDst() {
		return lonDst;
	}
	public void setLonDst(double lonDst) {
		this.lonDst = lonDst;
	}
}
