package com.quckapp.permission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quckapp.permission.TestDataFactory;
import com.quckapp.permission.dto.PermissionDtos.*;
import com.quckapp.permission.exception.DuplicateResourceException;
import com.quckapp.permission.exception.GlobalExceptionHandler;
import com.quckapp.permission.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleController Tests")
class UserRoleControllerTest {

    @Mock
    private PermissionService permissionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID workspaceId;
    private UUID roleId;
    private UUID userId;
    private UserRoleResponse testUserRoleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        UserRoleController controller = new UserRoleController(permissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        workspaceId = UUID.randomUUID();
        roleId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUserRoleResponse = TestDataFactory.createUserRoleResponse();
        testUserRoleResponse.setUserId(userId);
        testUserRoleResponse.setRoleId(roleId);
        testUserRoleResponse.setWorkspaceId(workspaceId);
    }

    @Nested
    @DisplayName("Assign Role Tests")
    class AssignRoleTests {

        @Test
        @DisplayName("should assign role to user")
        void shouldAssignRoleToUser() throws Exception {
            AssignRoleRequest request = TestDataFactory.assignRoleRequest(userId, roleId, workspaceId);

            when(permissionService.assignRole(any(AssignRoleRequest.class), any()))
                    .thenReturn(testUserRoleResponse);

            mockMvc.perform(post("/api/user-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Role assigned"))
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()));

            verify(permissionService).assignRole(any(AssignRoleRequest.class), any());
        }

        @Test
        @DisplayName("should assign role with X-User-Id header")
        void shouldAssignRoleWithHeader() throws Exception {
            UUID grantedBy = UUID.randomUUID();
            AssignRoleRequest request = TestDataFactory.assignRoleRequest(userId, roleId, workspaceId);

            when(permissionService.assignRole(any(AssignRoleRequest.class), eq(grantedBy)))
                    .thenReturn(testUserRoleResponse);

            mockMvc.perform(post("/api/user-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", grantedBy.toString())
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(permissionService).assignRole(any(AssignRoleRequest.class), eq(grantedBy));
        }

        @Test
        @DisplayName("should return 409 when role already assigned")
        void shouldReturn409WhenRoleAlreadyAssigned() throws Exception {
            AssignRoleRequest request = TestDataFactory.assignRoleRequest(userId, roleId, workspaceId);

            when(permissionService.assignRole(any(AssignRoleRequest.class), any()))
                    .thenThrow(new DuplicateResourceException("User already has this role"));

            mockMvc.perform(post("/api/user-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Revoke Role Tests")
    class RevokeRoleTests {

        @Test
        @DisplayName("should revoke role from user")
        void shouldRevokeRoleFromUser() throws Exception {
            doNothing().when(permissionService).revokeRole(userId, roleId, workspaceId);

            mockMvc.perform(delete("/api/user-roles/user/{userId}/role/{roleId}/workspace/{workspaceId}",
                            userId, roleId, workspaceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Role revoked"));

            verify(permissionService).revokeRole(userId, roleId, workspaceId);
        }
    }

    @Nested
    @DisplayName("Get User Permissions Tests")
    class GetUserPermissionsTests {

        @Test
        @DisplayName("should get user permissions in workspace")
        void shouldGetUserPermissions() throws Exception {
            UserPermissionsResponse response = TestDataFactory.createUserPermissionsResponse();
            response.setUserId(userId);
            response.setWorkspaceId(workspaceId);

            when(permissionService.getUserPermissions(userId, workspaceId)).thenReturn(response);

            mockMvc.perform(get("/api/user-roles/user/{userId}/workspace/{workspaceId}",
                            userId, workspaceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.data.permissions").isArray());

            verify(permissionService).getUserPermissions(userId, workspaceId);
        }
    }
}
