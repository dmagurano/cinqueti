package it.polito.cinqueti.interceptors;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ThymeleafLayoutInterceptor extends HandlerInterceptorAdapter {
    
    @Value("#{'${topics}'.split(',')}")
	private List<String> topics;
    
    private boolean isRedirectOrForward(String viewName) {
        return viewName.startsWith("redirect:") || viewName.startsWith("forward:");
    }
    
    @Override
    public void postHandle(
    		HttpServletRequest request, 
    		HttpServletResponse response, 
    		Object handler, 
    		ModelAndView modelAndView) throws Exception {
    	
        if (modelAndView == null || !modelAndView.hasView())
            return;
        
        String originalViewName = modelAndView.getViewName();
        
        if (isRedirectOrForward(originalViewName))
            return;
      
    	// here add all attributes in common in each page
    	modelAndView.getModelMap().addAttribute("topics", topics);
    }    
}