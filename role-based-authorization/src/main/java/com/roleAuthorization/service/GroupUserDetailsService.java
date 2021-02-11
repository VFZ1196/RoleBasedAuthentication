package com.roleAuthorization.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.roleAuthorization.model.User;
import com.roleAuthorization.repository.UserRepository;
@Service
public class GroupUserDetailsService implements UserDetailsService{
	
	@Autowired
	private UserRepository repository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<User> user = repository.findByUserName(username);
		return user.map(GroupUserDetails::new)
				.orElseThrow(()->new UsernameNotFoundException(username+ "does not exist"));
	}

}
