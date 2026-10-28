package com.campusbid.backend.security;

import com.campusbid.backend.model.User;
import com.campusbid.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(emailOrUsername.trim().toLowerCase())
                .or(() -> userRepository.findByStudentId(emailOrUsername.trim()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + emailOrUsername));

        return UserPrincipal.create(user);
    }
}
