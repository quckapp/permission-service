package com.quckapp.permission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quckapp.permission.exception.GlobalExceptionHandler;
import com.quckapp.permission.service.CasbinPolicySyncService;
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
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Mock
    private CasbinPolicySyncService casbinPolicySyncService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminController controller = new AdminController(casbinPolicySyncService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Casbin Policy Management Tests")
    class CasbinPolicyManagementTests {

        @Test
        @DisplayName("should reload Casbin policies")
        void shouldReloadCasbinPolicies() throws Exception {
            doNothing().when(casbinPolicySyncService).reloadPolicies();

            mockMvc.perform(post("/api/admin/casbin/reload"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Casbin policies reloaded"));

            verify(casbinPolicySyncService).reloadPolicies();
        }

        @Test
        @DisplayName("should sync Casbin policies")
        void shouldSyncCasbinPolicies() throws Exception {
            doNothing().when(casbinPolicySyncService).syncAllPolicies();

            mockMvc.perform(post("/api/admin/casbin/sync"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Casbin policies synced"));

            verify(casbinPolicySyncService).syncAllPolicies();
        }

        @Test
        @DisplayName("should get Casbin policies")
        void shouldGetCasbinPolicies() throws Exception {
            List<List<String>> policies = List.of(
                    List.of("role1", "workspace1", "message", "read")
            );
            List<List<String>> groupings = List.of(
                    List.of("user1", "role1", "workspace1")
            );

            when(casbinPolicySyncService.getAllPolicies()).thenReturn(policies);
            when(casbinPolicySyncService.getAllGroupingPolicies()).thenReturn(groupings);

            mockMvc.perform(get("/api/admin/casbin/policies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.policies").isArray())
                    .andExpect(jsonPath("$.data.groupings").isArray());

            verify(casbinPolicySyncService).getAllPolicies();
            verify(casbinPolicySyncService).getAllGroupingPolicies();
        }
    }
}
