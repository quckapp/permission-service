package com.quckapp.permission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quckapp.permission.TestDataFactory;
import com.quckapp.permission.dto.PermissionDtos.*;
import com.quckapp.permission.exception.GlobalExceptionHandler;
import com.quckapp.permission.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionController Tests")
class PermissionControllerTest {

    @Mock
    private PermissionService permissionService;

    private MockMvc mockMvc;

    private PermissionResponse testPermissionResponse;

    @BeforeEach
    void setUp() {
        PermissionController controller = new PermissionController(permissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testPermissionResponse = TestDataFactory.createPermissionResponse();
    }

    @Nested
    @DisplayName("Permission Listing Tests")
    class PermissionListingTests {

        @Test
        @DisplayName("should get all permissions")
        void shouldGetAllPermissions() throws Exception {
            when(permissionService.getAllPermissions())
                    .thenReturn(List.of(testPermissionResponse));

            mockMvc.perform(get("/api/permissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].resource").value("message"));

            verify(permissionService).getAllPermissions();
        }

        @Test
        @DisplayName("should return empty list when no permissions")
        void shouldReturnEmptyListWhenNoPermissions() throws Exception {
            when(permissionService.getAllPermissions()).thenReturn(List.of());

            mockMvc.perform(get("/api/permissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(permissionService).getAllPermissions();
        }

        @Test
        @DisplayName("should get permissions by resource")
        void shouldGetPermissionsByResource() throws Exception {
            when(permissionService.getPermissionsByResource("message"))
                    .thenReturn(List.of(testPermissionResponse));

            mockMvc.perform(get("/api/permissions/resource/{resource}", "message"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].resource").value("message"));

            verify(permissionService).getPermissionsByResource("message");
        }

        @Test
        @DisplayName("should return empty list for unknown resource")
        void shouldReturnEmptyListForUnknownResource() throws Exception {
            when(permissionService.getPermissionsByResource("unknown"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/permissions/resource/{resource}", "unknown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(permissionService).getPermissionsByResource("unknown");
        }
    }
}
