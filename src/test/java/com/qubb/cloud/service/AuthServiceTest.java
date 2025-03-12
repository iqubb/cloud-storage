package com.qubb.cloud.service;

import com.qubb.cloud.auth.AuthService;
import com.qubb.cloud.auth.UserCredentials;
import com.qubb.cloud.user.User;
import com.qubb.cloud.user.UserRepository;
import com.qubb.cloud.util.DataUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authServiceUnderTest;

    @Test
    @DisplayName("Test register user functionality")
    public void givenNewUser_whenRegisterUser_thenUserIsSaved() {

        //Given
        var userToSave = DataUtil.getKikwiTransient();
        BDDMockito.given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        BDDMockito.given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
        BDDMockito.given(userRepository.save(any(User.class))).willReturn(userToSave);

        //When
        var response = authServiceUnderTest
                .register(UserCredentials.builder()
                                .username(userToSave.getUsername())
                                .password(userToSave.getPassword())
                                .build()
                );

        //Then
        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo(userToSave.getUsername());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword"); // Проверяем зашифрованный пароль
    }
}
