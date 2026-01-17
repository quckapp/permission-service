package com.quckapp.permission.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for QuckApp Permission Service.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "QuckApp Permission Service API",
        version = "1.0.0",
        description = """
            ## RBAC Permission Management Service

            The QuckApp Permission Service provides role-based access control (RBAC)
            capabilities for managing permissions across the QuckApp ecosystem.

            ### Features
            - **Role Management** - Create, update, delete roles with permissions
            - **Permission Management** - Define and query available permissions
            - **User Role Assignment** - Assign/revoke roles to/from users
            - **Permission Checking** - Verify user permissions in real-time
            - **Workspace Scoping** - Permissions scoped to specific workspaces

            ### Authentication
            This service is typically called by other microservices. Authentication is handled
            via API Key or internal service mesh authentication.

            ### Integration
            - Works with Casbin for policy enforcement
            - Integrates with Auth Service for user identity
            - Publishes permission events to Kafka
            """,
        contact = @Contact(
            name = "QuckApp Team",
            email = "support@quckapp.com",
            url = "https://quckapp.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "/", description = "Permission Service Base Path"),
        @Server(url = "http://localhost:8083", description = "Local Development"),
        @Server(url = "https://api.quckapp.com/permissions", description = "Production")
    },
    tags = {
        @Tag(name = "Roles", description = "Role management operations - create, read, update, delete roles"),
        @Tag(name = "Permissions", description = "Permission definitions and queries"),
        @Tag(name = "User Roles", description = "User role assignment and revocation"),
        @Tag(name = "Permission Check", description = "Real-time permission verification")
    }
)
// Security schemes are configured programmatically in customOpenAPI() bean
public class OpenApiConfig {

    /**
     * Configure security schemes programmatically.
     * This ensures the Authorize button appears in Swagger UI.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                            JWT Bearer token authentication.

                            Obtain a token from the Auth Service by calling `/v1/login`.
                            Include the token in the Authorization header:
                            `Authorization: Bearer <token>`
                            """))
                .addSecuritySchemes("apiKey",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                        .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("""
                            API Key authentication for internal services.

                            Used for service-to-service communication within the QuckApp ecosystem.
                            Include the key in the X-API-Key header:
                            `X-API-Key: <your-api-key>`
                            """)));
    }
}
