package it.polito.cinqueti.entities;

import org.springframework.security.core.GrantedAuthority;

public class Role implements GrantedAuthority {
	
	private static final long serialVersionUID = 1L;
	private String role;
	
	public Role (String role) {
		this.role = role;
	}

	@Override
	public String getAuthority() {
		return role;
	}
	
	
	
}
