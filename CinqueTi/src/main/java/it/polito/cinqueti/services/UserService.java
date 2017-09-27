package it.polito.cinqueti.services;

import java.util.List;

import org.springframework.data.domain.Page;

import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.entities.VerificationToken;

public interface UserService {
	
	boolean emailIsTaken(String email);
	
	void saveUser(User user);
	
	User findLoggedInUser();
	
	void updateUserNewNickname(User user, String nickname);
	
	void updateUserNewPassword(User user, String password);
	
	boolean checkPassword(User user, String password);

    User findByEmail(String email);

    User findByNickname(String nickname);
    
    List<User> findAll();
    
    Page<User> findAll(Integer page, Integer per_page);
    
    VerificationToken getVerificationToken(String token);
    
    void saveVerificationToken(User user, String token);
    
    void updateVerificationToken(VerificationToken verificationToken);
    
    void removeVerificationToken(VerificationToken verificationToken);
}
