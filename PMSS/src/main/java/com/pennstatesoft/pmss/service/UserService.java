package com.pennstatesoft.pmss.service;

import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        System.out.println("USER ROLE: " + user.getRole());
        System.out.println("USER TYPE: " + user.getClass());

        System.out.println("LOGIN EMAIL: " + email);

        user = userRepository.findByEmail(email);

        System.out.println("DATABASE USER: " + user);

        System.out.println("HASH FROM DATABASE: " + user.getPasswordHash());

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

}