package it.polito.cinqueti.repositories;

import it.polito.cinqueti.entities.Rate;

public interface AlertRepositoryCustom {
	public void updateUserRate(String alertId, Rate rate);
}
