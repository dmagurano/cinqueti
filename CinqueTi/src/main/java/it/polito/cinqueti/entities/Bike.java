package it.polito.cinqueti.entities;

public class Bike {

	private boolean owned;
	
	private boolean sharing;
	
	public Bike(boolean owned, boolean sharing){
		this.owned = owned;
		this.sharing = sharing;
	}

	public Bike() {
	}

	public boolean isOwned() {
		return owned;
	}

	public void setOwned(boolean owned) {
		this.owned = owned;
	}

	public boolean isSharing() {
		return sharing;
	}

	public void setSharing(boolean sharing) {
		this.sharing = sharing;
	}
	
	

}
