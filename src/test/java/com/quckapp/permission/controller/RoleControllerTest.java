package com.quckapp.permission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quckapp.permission.TestDataFactory;
import com.quckapp.permission.dto.PermissionDtos.*;
import com.quckapp.permission.exception.DuplicateResourceException;
import com.quckapp.permission.exception.GlobalExceptionHandler;
import com.quckapp.permission.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController Tests")
class RoleControllerTest {

    @Mock
    private PermissionService permissionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID workspaceId;
    private UUID roleId;
    private RoleResponse testRoleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        RoleController controller = new RoleController(permissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        workspaceId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        testRoleResponse = TestDataFactory.createRoleResponse();
        testRoleResponse.setId(roleId);
        testRoleResponse.setWorkspaceId(workspaceId);
    }

    @Nested
    @DisplayName("Create Role Tests")
    class CreateRoleTests {

        @Test
        @DisplayName("should create role successfully")
        void shouldCreateRoleSuccessfully() throws Exception {
            CreateRoleRequest request = TestDataFactory.createRoleRequest(workspaceId, "Editor");

            when(permissionService.createRole(any(CreateRoleRequest.class))).thenReturn(testRoleResponse);

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Role created"))
                    .andExpect(jsonPath("$.data.id").value(roleId.toString()));

            verify(permissionService).createRole(any(CreateRoleRequest.class));
        }

        @Test
        @DisplayName("should return 409 when role already exists")
        void shouldReturn409WhenRoleExists() throws Exception {
            CreateRoleRequest request = TestDataFactory.createRoleRequest(workspaceId, "Editor");

            when(permissionService.createRole(any(CreateRoleRequest.class)))
                    .thenThrow(new DuplicateResourceException("Role already exists"));

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            CreateRoleRequest request = CreateRoleRequest.builder()
                    .workspaceId(null)
                    .name("")
                    .build();

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Role Tests")
    class GetRoleTests {

        @Test
        @DisplayName("should get role by ID")
        void shouldGetRoleById() throws Exception {
            when(permissionService.getRoleById(roleId)).thenReturn(testRoleResponse);

            mockMvc.perform(get("/api/roles/{id}", roleId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(roleId.toString()))
                    .andExpect(jsonPath("$.data.name").value("Member"));

            verify(permissionService).getRoleById(roleId);
        }

        @Test
        @DisplayName("should return 404 when role not found")
        void shouldReturn404WhenRoleNotFound() throws Exception {
            when(permissionService.getRoleById(roleId))
                    .thenThrow(new ResourceNotFoundException("Role not found"));

            mockMvc.perform(get("/api/roles/{id}", roleId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should get roles by workspace")
        void shouldGetRolesByWorkspace() throws Exception {
            when(permissionService.getRolesByWorkspace(workspaceId))
                    .thenReturn(List.of(testRoleResponse));

            mockMvc.perform(get("/api/roles/workspace/{workspaceId}", workspaceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(roleId.toString()));

            verify(permissionService).getRolesByWorkspace(workspaceId);
        }
    }

    @Nested
    @DisplayName("Update Role Tests")
    class UpdateRoleTests {

        @Test
        @DisplayName("should update role successfully")
        void shouldUpdateRoleSuccessfully() throws Exception {
            UpdateRoleRequest request = UpdateRoleRequest.builder()
                    .name("Updated Role")
                    .description("Updated description")
                    .build();

            when(permissionService.updateRole(eq(roleId), any(UpdateRoleRequest.class)))
                    .thenReturn(testRoleResponse);

            mockMvc.perform(put("/api/roles/{id}", roleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Role updated"));

            verify(permissionService).updateRole(eq(roleId), any(UpdateRoleRequest.class));
        }

        @Test
        @DisplayName("should return 404 when role not found for update")
        void shouldReturn404WhenRoleNotFoundForUpdate() throws Exception {
            UpdateRoleRequest request = UpdateRoleRequest.builder().name("Updated").build();

            when(permissionService.updateRole(eq(roleId), any(UpdateRoleRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Role not found"));

            mockMvc.perform(put("/api/roles/{id}", roleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete Role Tests")
    class DeleteRoleTests {

        @Test
        @DisplayName("should delete role successfully")
        void shouldDeleteRoleSuccessfully() throws Exception {
            doNothing().when(permissionService).deleteRole(roleId);

            mockMvc.perform(delete("/api/roles/{id}", roleId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Role deleted"));

            verify(permissionService).deleteRole(roleId);
        }

        @Test
        @DisplayName("should return 404 when role not found for deletion")
        void shouldReturn404WhenRoleNotFoundForDeletion() throws Exception {
            doThrow(new ResourceNotFoundException("Role not found"))
                    .when(permissionService).deleteRole(roleId);

            mockMvc.perform(delete("/api/roles/{id}", roleId))
                    .andExpect(status().isNotFound());
        }
    }
}
