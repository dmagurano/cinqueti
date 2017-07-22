package it.polito.cinqueti;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import it.polito.cinqueti.interceptors.ThymeleafLayoutInterceptor;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {
	
	@Autowired
	ThymeleafLayoutInterceptor thymeleafLayoutInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(thymeleafLayoutInterceptor)
		//.addPathPatterns("/**")
		;
	}
}