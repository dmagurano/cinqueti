package it.polito.cinqueti;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import it.polito.cinqueti.services.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private UserDetailsServiceImpl userDetailsService;
	 
	@Override
	protected void configure(AuthenticationManagerBuilder auth)
	  throws Exception {
	    auth.authenticationProvider(authenticationProvider());
	}
	 
	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
	    DaoAuthenticationProvider authProvider
	      = new DaoAuthenticationProvider();
	    authProvider.setUserDetailsService(userDetailsService);
	    authProvider.setPasswordEncoder(encoder());
	    return authProvider;
	}
	 
	@Bean
	public BCryptPasswordEncoder encoder() {
	    return new BCryptPasswordEncoder(11);
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		//url
		 http
         .authorizeRequests()
//             .antMatchers("/","/css/**","/fonts/**","/js/**","/assets/**", "/register", "/welcome","/webjars/**", "/rest/**").permitAll()
         	 .antMatchers("/profile/**","/chat/**").authenticated()
             .anyRequest().permitAll()
             .and()
         .formLogin()
             .loginPage("/login")
             .permitAll()
             .and()
         .logout()
             .permitAll();
		 http.requiresChannel().anyRequest().requiresSecure();
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		
	}
	
	
	
}
