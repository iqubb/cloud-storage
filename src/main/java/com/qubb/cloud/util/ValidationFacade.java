package com.qubb.cloud.util;

import com.qubb.cloud.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidationFacade {

    private final RequestValidator requestValidator;
    private final ResourceValidator resourceValidator;

    public void validateRequest(UserDetailsImpl user, String... paths) {
        requestValidator.validateRequest(user, paths);
    }

    public void checkTargetParentExists(String targetObject) {
        resourceValidator.checkTargetParentExists(targetObject);
    }

    public boolean isSourceResourceExists(String objectName) {
        return resourceValidator.isSourceResourceExists(objectName);
    }
}
