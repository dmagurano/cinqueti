package it.polito.cinqueti.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.polito.cinqueti.entities.BusLine;

@Repository
public interface LineRepository extends CrudRepository<BusLine, String>{

	public List<BusLine> findAll();
	public BusLine findOne(String id);
	public Page<BusLine> findAll(Pageable p);
}
