package it.polito.cinqueti.entities;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Topic extends ResourceSupport {

	private String topicName;
	
	@JsonCreator
	public Topic(@JsonProperty("topic_name") String topicName) {
		this.topicName = topicName;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}
}
