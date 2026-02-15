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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionCheckController Tests")
class PermissionCheckControllerTest {

    @Mock
    private PermissionService permissionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        PermissionCheckController controller = new PermissionCheckController(permissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Check Permission Tests")
    class CheckPermissionTests {

        @Test
        @DisplayName("should check permission and return allowed")
        void shouldCheckPermissionAndReturnAllowed() throws Exception {
            CheckPermissionRequest request = TestDataFactory.checkPermissionRequest("message", "read");
            PermissionCheckResponse response = TestDataFactory.createPermissionCheckResponse(true);

            when(permissionService.checkPermission(any(CheckPermissionRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/permissions/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.allowed").value(true));

            verify(permissionService).checkPermission(any(CheckPermissionRequest.class));
        }

        @Test
        @DisplayName("should check permission and return denied")
        void shouldCheckPermissionAndReturnDenied() throws Exception {
            CheckPermissionRequest request = TestDataFactory.checkPermissionRequest("admin", "manage");
            PermissionCheckResponse response = TestDataFactory.createPermissionCheckResponse(false);

            when(permissionService.checkPermission(any(CheckPermissionRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/permissions/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.allowed").value(false));
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            CheckPermissionRequest request = CheckPermissionRequest.builder()
                    .userId(null)
                    .workspaceId(null)
                    .resource("")
                    .action("")
                    .build();

            mockMvc.perform(post("/api/permissions/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
