package it.polito.cinqueti.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.polito.cinqueti.entities.BusLine;

@Repository
public interface LineRepository extends CrudRepository<BusLine, String>{

	public List<BusLine> findAll();
}
