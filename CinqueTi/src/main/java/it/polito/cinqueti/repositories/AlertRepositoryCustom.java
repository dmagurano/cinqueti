package it.polito.cinqueti.repositories;

import java.util.Date;

import it.polito.cinqueti.entities.Rate;

public interface AlertRepositoryCustom {
	public void updateUserRate(String alertId, Rate rate);
	public void updateReferenceTs(String alertId, Date ts);
}
