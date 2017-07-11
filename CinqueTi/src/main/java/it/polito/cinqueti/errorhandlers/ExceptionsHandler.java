package it.polito.cinqueti.errorhandlers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionsHandler {

	
	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(value=HttpStatus.NOT_FOUND)
	@ResponseBody
	public ResponseEntity<Object> resourceNotFound(HttpServletRequest req, ResourceNotFoundException ex) {
		String errorURL = req.getRequestURL().toString();
	    String errorMessage = ex.toString() + " " + errorURL + ". Caused by resource " + ex.getException();
	    
	     
	    return new ResponseEntity<Object>(errorMessage,HttpStatus.NOT_FOUND);
	}
}

 

