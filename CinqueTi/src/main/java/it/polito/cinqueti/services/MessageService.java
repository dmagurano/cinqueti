package it.polito.cinqueti.services;

import org.springframework.data.domain.Page;

import it.polito.cinqueti.entities.Message;

public interface MessageService {

	public Page<Message> findByTopic(String topic, int page, int size);

}
