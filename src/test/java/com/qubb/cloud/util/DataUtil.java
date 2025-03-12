package com.qubb.cloud.util;

import com.qubb.cloud.auth.UserCredentials;
import com.qubb.cloud.auth.UsernameResponse;
import com.qubb.cloud.user.User;

public class DataUtil {

    public static User getKikwiTransient() {
        return User.builder()
                .username("kikwi")
                .password("password")
                .build();
    }

    public static User getKikwiPersisted() {
        return User.builder()
                .id(1)
                .username("kikwi")
                .password("password")
                .build();
    }

    public static UserCredentials getKikwiRegisterRequest() {
        return UserCredentials.builder()
                .username("kikwi")
                .password("password")
                .build();
    }

    public static UsernameResponse getKikwiRegisterResponse() {
        return UsernameResponse.builder()
                .username("kikwi")
                .build();
    }

    public static UserCredentials getKikwiAuthenticationRequest() {
        return UserCredentials.builder()
                .username("kikwi")
                .password("password")
                .build();
    }

}
