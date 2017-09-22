package it.polito.cinqueti.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.entities.VerificationToken;
import it.polito.cinqueti.repositories.UserRepository;
import it.polito.cinqueti.repositories.VerificationTokenRepository;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
   
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    
    @Autowired
    private SecurityService securityService;

	@Override
	public void saveUser(User user){
    	List<String> roles = new ArrayList<String>();
    	roles.add("ROLE_USER");
    	
        //user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setAuthorities(roles);
        
        userRepository.save(user);
	}
    
	@Override
    public boolean emailIsTaken(String email) {
        User user = this.findByEmail(email);
        
        if (user != null) {
            return true;
        }
        else{
        	user = new User();
        	user.setEmail(email);
        	
        	VerificationToken databaseToken = verificationTokenRepository.findByEmail(email);
        	
        	if (databaseToken != null)
        		return true;
        }
        
        return false;
    }
    
    @Override
    public User findLoggedInUser() {
    	return findByEmail(securityService.findLoggedInUsername());
    }
    
    @Override
    public User findByEmail(String username) {
    	return userRepository.findByEmail(username);
    }

	@Override
	public List<User> findAll() {
		return userRepository.findAll();
	}

	@Override
	public Page<User> findAll(Integer page, Integer per_page) {
		// build a pageable object, used to specify the query paging params
		PageRequest pageReq = new PageRequest(page,per_page);
		// execute the query
		Page<User> pageRes = userRepository.findAll(pageReq);
		// if the user asks for a page X that is greater than the total number of pages in the db, return empty response
		if (page > pageRes.getTotalPages())
			return null;
		return pageRes;
	}
	
	@Override
	public boolean checkPassword(User user, String password) {
		return bCryptPasswordEncoder.matches(password, user.getPassword());
	}

	@Override
	public void updateUserNewNickname(User user, String nickname) {
		user.setNickname(nickname);
		userRepository.save(user);		
	}

	@Override
	public void updateUserNewPassword(User user, String password) {
		user.setPassword(bCryptPasswordEncoder.encode(password));
		userRepository.save(user);
	}

	@Override
	public VerificationToken getVerificationToken(String token) {
		return verificationTokenRepository.findByToken(token);
	}

	/*
	@Override
	public User getUser(String verificationToken) {
		String userId = verificationTokenRepository.findByToken(verificationToken).getUser().getId();
		
		return userRepository.findById(userId);
	}
	*/

	@Override
	public void saveVerificationToken(User user, String token) {
		user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
		
		VerificationToken verificationToken = new VerificationToken(token, user);
		
		verificationTokenRepository.save(verificationToken);
	}

	@Override
	public void removeVerificationToken(VerificationToken token) {
		verificationTokenRepository.deleteByToken(token.getToken());
	}

	@Override
	public void updateVerificationToken(VerificationToken token) {
		verificationTokenRepository.save(token);
	}

	@Override
	public User findByNickname(String nickname) {
		return userRepository.findByNickname(nickname);
	}
}