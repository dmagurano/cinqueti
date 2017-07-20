package it.polito.cinqueti.services;

import java.util.List;

import org.springframework.data.domain.Page;

import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.entities.VerificationToken;

public interface UserService {
	void registerUserFirstPhase(User user) throws Exception;
	
	void registerUserSecondPhase(User user) throws Exception;
	
	User findLoggedInUser();
	
	void updateUserNewNickname(User user, String nickname);
	
	void updateUserNewPassword(User user, String password);
	
	boolean checkPassword(User user, String password);

    User findByEmail(String email);
    
    List<User> findAll();
    
    Page<User> findAll(Integer page, Integer per_page);
    
    User getUser(String verificationToken);
    
    void saveRegisteredUser(User user, String token);
    
    void saveVerificationToken(User user, String token);
    
    VerificationToken getVerificationToken(String verificationToken);
    
    void removeVerificationToken(User user, String token);

}
