package it.polito.cinqueti.errorhandlers;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Resource not found")
public class ResourceNotFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	private String id;
	private String type;
	
	public ResourceNotFoundException(String id, String type) {
		this.id = id; this.type = type;
	}

	public ResourceNotFoundException(String id) {
		this.id = id;
	}	

	public String getException() {
		if(type != null)
			return "resource " + id + " of type " + type;
		return "resource " + id;
	}
}
