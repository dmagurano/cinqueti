package it.polito.cinqueti.entities;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import javax.persistence.Lob;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.polito.cinqueti.validator.PasswordMatches;
import it.polito.cinqueti.validator.ValidEmail;

@PasswordMatches(groups = {User.FirstPhaseValidation.class})
public class User implements UserDetails {
	
	private static final long serialVersionUID = 1L;
	
	public interface FirstPhaseValidation{
		// first validation group
		// email, password
	}
	
	public interface SecondPhaseValidation{
		// second validation group
		// personal information
	}
	
	public interface ThirdPhaseValidation{
		// third validation group
		// personal transport information
	}
	
	@Id
	private String id;
	
	@ValidEmail(groups = {FirstPhaseValidation.class})
	@NotNull(groups = {FirstPhaseValidation.class})
	@NotEmpty(groups = {FirstPhaseValidation.class})
	@Indexed(unique = true)
	private String email;
	
	@NotNull(groups = {FirstPhaseValidation.class})
    @NotEmpty(groups = {FirstPhaseValidation.class})
    @Size(min = 8, max= 36, groups = {FirstPhaseValidation.class})
	private String password;
	
	@NotNull(groups = {FirstPhaseValidation.class})
    @NotEmpty(groups = {FirstPhaseValidation.class})
	private String confirmedPassword;
	
	private List<Role> roles;
	
	@NotNull(groups = {SecondPhaseValidation.class})
    @NotEmpty(groups = {SecondPhaseValidation.class})
	private String nickname;
	
	@NotNull(groups = {SecondPhaseValidation.class})
    @NotEmpty(groups = {SecondPhaseValidation.class})
	@Pattern( groups = SecondPhaseValidation.class, regexp = "^(M|F|other)$" )
	private String gender;
	
	// an Integer cannot be @NotEmpty as it is not a string, collection, map or array
	@NotNull(groups = {SecondPhaseValidation.class})
	@Range(min=10, max=110, groups={SecondPhaseValidation.class})
	private Integer age;
	
	@NotNull(groups = {SecondPhaseValidation.class})
    @NotEmpty(groups = {SecondPhaseValidation.class})
	private String education;
	
	@NotNull(groups = {SecondPhaseValidation.class})
    @NotEmpty(groups = {SecondPhaseValidation.class})
	private String job;

	@NotNull(groups = {ThirdPhaseValidation.class})
	@NotEmpty(groups = {ThirdPhaseValidation.class})
	private String carSharing;
	
	@NotNull(groups = {ThirdPhaseValidation.class})
    @NotEmpty(groups = {ThirdPhaseValidation.class})
	private String pubTransport;

	@NotNull(groups = {ThirdPhaseValidation.class})
	@Valid
	private Bike bikeUsage;
	
	//optional
	private String carFuel;
	//optional
	private Integer carYear;
	
	@Lob
	private byte[] image;
	
	private boolean enabled;

	//used to convert byte[] in base64 string otherwise the image won't be showed in thymeleaf
	public String generateBase64Image()
	{
	    return Base64.getEncoder().encodeToString((this.getImage()));
	}
	
	public User() {
		setEnabled(false);
		bikeUsage = new Bike();
	}

	public User(String email, String nickname) {
		this.setEnabled(false);
		this.email = email;
		this.nickname = nickname;
	}
	@JsonIgnore
	@Override
	public Collection<Role> getAuthorities() {
		return this.roles;
	}
	
	@JsonIgnore
	@Override
	public String getPassword() {
		return this.password;
	}
	
	@JsonIgnore
	@Override
	public String getUsername() {
		return this.email;
	}
	
	public void setUsername(String email) {
		setEmail(email);
	}
	
	@JsonIgnore
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@JsonIgnore
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@JsonIgnore
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@JsonIgnore
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@JsonIgnore
	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getEducation() {
		return education;
	}

	public void setEducation(String education) {
		this.education = education;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public String getCarSharing() {
		return carSharing;
	}

	public void setCarSharing(String carSharing) {
		this.carSharing = carSharing;
	}

	public Bike getBikeUsage() {
		return bikeUsage;
	}

	public void setBikeUsage(Bike bikeUsage) {
		this.bikeUsage = bikeUsage;
	}

	public String getPubTransport() {
		return pubTransport;
	}

	public void setPubTransport(String pubTransport) {
		this.pubTransport = pubTransport;
	}


	public void setPassword(String password) {
		this.password = password;
	}
	
	@JsonIgnore
	@Transient
    public String getConfirmedPassword() {
        return confirmedPassword;
    }

    public void setConfirmedPassword(String passwordConfirm) {
        this.confirmedPassword = passwordConfirm;
    }

	public void setAuthorities(List<String> roles) {
		this.roles = new ArrayList<Role>();
		for (String role : roles) {
			this.roles.add( new Role(role) );
		}
	}

	@JsonIgnore
	public byte[] getImage() {
		return image;
	}

	public void setImage(byte[] image) {
		this.image = image;
	}

	@JsonIgnore
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public String getCarFuel() {
		return carFuel;
	}

	public void setCarFuel(String carFuel) {
		this.carFuel = carFuel;
	}

	public Integer getCarYear() {
		return carYear;
	}

	public void setCarYear(Integer carYear) {
		this.carYear = carYear;
	}
}
