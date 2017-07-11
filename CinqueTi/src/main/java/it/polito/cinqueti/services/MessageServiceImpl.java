package it.polito.cinqueti.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import it.polito.cinqueti.entities.Message;
import it.polito.cinqueti.repositories.MessageRepository;

@Service
public class MessageServiceImpl implements MessageService{

	@Autowired
	MessageRepository messageRepository;
	
	@Override
	public Page<Message> findByTopic(String topic, int page, int size) {
		// build a pageable object (PageRequest), used to specify the query paging params and execute the query
		Page<Message> pageRes = messageRepository.findByTopic(topic, new PageRequest(page, size));
		// if the user asks for a page X that is greater than the total number of pages in the db, return empty response
		if (page > pageRes.getTotalPages())
			return null;
		return pageRes;
	}

}
