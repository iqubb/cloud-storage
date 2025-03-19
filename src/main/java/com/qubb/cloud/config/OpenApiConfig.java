package com.qubb.cloud.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "Cloud Storage API",
                version = "1.0.0",
                description = """
            ## Cloud Storage Service API Documentation
            
            Key features:
            - File management operations
            - User authentication via session cookies
            - Redis-backed session storage
            
            After successful login:
            1. Client receives JSESSIONID cookie
            2. Use this cookie for authenticated requests
            3. Session timeout: 30 minutes
            
            Support:
            - Technical issues: support@qubb.cloud
            - API questions: api-team@qubb.cloud
            """
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "Local development environment"
                ),
        }
)
@SecurityScheme(
        name = "sessionCookie",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "JSESSIONID",
        description = """
        Session cookie authentication.
        First authenticate via /api/auth/sign-in endpoint,
        then include received JSESSIONID cookie in subsequent requests.
        """
)
public class OpenApiConfig {
}