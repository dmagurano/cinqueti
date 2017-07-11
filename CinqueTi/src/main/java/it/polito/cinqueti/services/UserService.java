package it.polito.cinqueti.services;

import java.util.List;

import org.springframework.data.domain.Page;

import it.polito.cinqueti.entities.User;

public interface UserService {
	void registerNewUserAccount(User user) throws Exception;
	
	User findLoggedInUser();
	
	void updateUserNewNickname(User user, String nickname);
	
	void updateUserNewPassword(User user, String password);
	
	boolean checkPassword(User user, String password);

    User findByEmail(String email);
    
    List<User> findAll();
    
    Page<User> findAll(Integer page, Integer per_page);

}
