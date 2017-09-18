package it.polito.cinqueti.entities;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

public class Car {
	
	@Range(min=1900, max=2017, groups=User.ThirdPhaseValidation.class)
	private Integer registrationYear;
	
	@NotEmpty(groups=User.ThirdPhaseValidation.class)
	@NotNull(groups=User.ThirdPhaseValidation.class)
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
