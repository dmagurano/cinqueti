package it.polito.cinqueti.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
	public void registerNewUser(User user) throws Exception {
		if (emailExist(user.getEmail()))
            throw new Exception("There is already an account with email adress: " +  user.getEmail());
    	
    	List<String> roles = new ArrayList<String>();
    	roles.add("ROLE_USER");
    	
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setAuthorities(roles);
        userRepository.save(user);
	}
    
    private boolean emailExist(String email) {
        User user = this.findByEmail(email);
        if (user != null) {
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
	public VerificationToken getVerificationToken(String verificationToken) {
		return verificationTokenRepository.findByToken(verificationToken);
	}

	@Override
	public User getUser(String verificationToken) {
		return verificationTokenRepository.findByToken(verificationToken).getUser();
	}

	@Override
	public void saveVerificationToken(User user, String token) {
		VerificationToken verificationToken = new VerificationToken(token, user);
		
		verificationTokenRepository.save(verificationToken);
	}

	@Override
	public void removeVerificationToken(String token) {
		verificationTokenRepository.deleteByToken(token);
	}

	@Override
	public void clearVerificationToken(User user, String token) {
		userRepository.delete(user);

		verificationTokenRepository.deleteByToken(token);
	}
	
	
}