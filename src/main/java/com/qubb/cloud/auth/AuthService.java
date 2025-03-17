package com.qubb.cloud.auth;

import com.qubb.cloud.exceptions.InvalidUserCredentialsException;
import com.qubb.cloud.exceptions.UsernameAlreadyTakenException;
import com.qubb.cloud.user.User;
import com.qubb.cloud.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UsernameResponse register(UserCredentials request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyTakenException("User " + request.username() + " already registered");
        }
        var user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .build();
        userRepository.save(user);
        return UsernameResponse.builder()
                .username(user.getUsername())
                .build();
    }

    public UsernameResponse authenticate(UserCredentials request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            throw new InvalidUserCredentialsException(e.getMessage());
        }
        return UsernameResponse.builder()
                .username(request.username())
                .build();
    }

    public void logout(HttpSession session) {
        if (session != null && !session.isNew()) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
