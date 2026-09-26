package com.medpulse.security;

import com.medpulse.model.User;
import com.medpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Email cannot be empty");
        }
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseGet(() -> {
                    String name = cleanEmail.contains("@") ? cleanEmail.substring(0, cleanEmail.indexOf('@')) : cleanEmail;
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    User autoProvisioned = User.builder()
                            .email(cleanEmail)
                            .fullName(name)
                            .role(com.medpulse.model.Role.ROLE_PATIENT)
                            .build();
                    return userRepository.save(autoProvisioned);
                });

        return UserDetailsImpl.build(user);
    }
}
