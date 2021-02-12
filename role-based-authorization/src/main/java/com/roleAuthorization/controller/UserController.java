package com.roleAuthorization.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.roleAuthorization.common.UserConstant;
import com.roleAuthorization.model.JwtRequest;
import com.roleAuthorization.model.JwtResponse;
import com.roleAuthorization.model.User;
import com.roleAuthorization.repository.UserRepository;
import com.roleAuthorization.service.GroupUserDetailsService;
import com.roleAuthorization.utility.JwtUtility;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository repository;
	
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private GroupUserDetailsService groupUserDetailsService;
	/*
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
*/
	@GetMapping()
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public List<User> loadUser() {
		return repository.findAll();
	}

	@GetMapping("/test")
	@PreAuthorize("hasAuthority('ROLE_USER')")
	public String testUserAccess() {
		return "User can only access this.";
	}
/*
	@PostMapping("/join")
	public String joinGroup(@RequestBody User user) {
		user.setRoles(UserConstant.DEFAULT_ROLE);
		String encryptedPwd = passwordEncoder.encode(user.getPassword());
		user.setPassword(encryptedPwd);
		repository.save(user);
		return "Hi " + user.getUserName() + " Welcome to group ";
	}
*/

	@PostMapping("/authenticate")
	public JwtResponse authenticate(@RequestBody JwtRequest jwtRequest) throws Exception {
		 try {
	            authenticationManager.authenticate(
	                    new UsernamePasswordAuthenticationToken(
	                            jwtRequest.getUsername(),
	                            jwtRequest.getPassword()
	                    )
	            );
	        } catch (BadCredentialsException e) {
	            throw new Exception("INVALID_CREDENTIALS", e);
	        }

	        final UserDetails userDetails
	                = groupUserDetailsService.loadUserByUsername(jwtRequest.getUsername());

	        final String token =
	                JwtUtility.generateToken(userDetails);

	        return  new JwtResponse(token);
	    }
	
	@GetMapping("/access/{userId}/{userRole}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')")
	public String giveAccessToUser(@PathVariable int userId, @PathVariable String userRole, Principal principal) {
		User user = repository.findById(userId).get();
		List<String> activeRoles = getRolesByLoggedInUser(principal);
		String newRole = "";
		if (activeRoles.contains(userRole)) {
			newRole = user.getRoles() + "," + userRole;
			user.setRoles(newRole);
		}
		repository.save(user);
		return "Hi " + user.getUserName() + " New Role assign to you by " + principal.getName();

	}

	private List<String> getRolesByLoggedInUser(Principal principal) {
		String roles = getLoggedInUser(principal).getRoles();
		List<String> assignRoles = Arrays.stream(roles.split(",")).collect(Collectors.toList());

		if (assignRoles.contains("ROLE_ADMIN")) {
			return Arrays.stream(UserConstant.ADMIN_ACCESS).collect(Collectors.toList());
		}

		if (assignRoles.contains("ROLE_MODERATOR")) {
			return Arrays.stream(UserConstant.MODERATOR_ACCESS).collect(Collectors.toList());

		}
		return Collections.emptyList();
	}

	private User getLoggedInUser(Principal principal) {
		return repository.findByUserName(principal.getName()).get();
	}

}
