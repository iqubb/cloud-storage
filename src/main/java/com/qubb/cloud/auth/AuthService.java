package com.qubb.cloud.auth;

import com.qubb.cloud.exception.InvalidUserCredentialsException;
import com.qubb.cloud.exception.UserNotAuthorizedException;
import com.qubb.cloud.exception.UsernameAlreadyTakenException;
import com.qubb.cloud.user.User;
import com.qubb.cloud.user.UserDetailsImpl;
import com.qubb.cloud.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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

    public UsernameResponse authenticate(UserCredentials request, HttpServletRequest httpRequest) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
            HttpSession session = httpRequest.getSession(true); // Создаём сессию явно
            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext()); // Явное сохранение контекста

        } catch (Exception exception) {
            throw new InvalidUserCredentialsException(exception.getMessage());
        }
        return UsernameResponse.builder()
                .username(request.username())
                .build();
    }

    public void logout(UserDetailsImpl userDetails, HttpSession session) {
        var user = userDetails.user();
        if (user == null) {
            throw new UserNotAuthorizedException("User not authorized");
        }
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
