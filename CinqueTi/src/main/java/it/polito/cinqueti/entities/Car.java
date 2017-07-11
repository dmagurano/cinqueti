package it.polito.cinqueti.entities;

public class Car {
	//TODO check errors into reg html
	private Integer registrationYear;
	private String fuelType;
	
	public Car() {
		
	}
	
	public Car(Integer registrationYear, String fuelType) {
		this.registrationYear = registrationYear;
		this.fuelType = fuelType;
	}
	
	public Integer getRegistrationYear() {
		return registrationYear;
	}

	public void setRegistrationYear(Integer registrationYear) {
		this.registrationYear = registrationYear;
	}

	public String getFuelType() {
		return fuelType;
	}

	public void setFuelType(String fuelType) {
		this.fuelType = fuelType;
	}

}
