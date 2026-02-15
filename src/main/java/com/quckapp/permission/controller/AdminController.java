package com.quckapp.permission.controller;

import com.quckapp.permission.dto.PermissionDtos.ApiResponse;
import com.quckapp.permission.service.CasbinPolicySyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative APIs for Casbin policy management")
public class AdminController {

    private final CasbinPolicySyncService casbinPolicySyncService;

    @PostMapping("/casbin/reload")
    @Operation(summary = "Reload Casbin policies from database")
    public ResponseEntity<ApiResponse<Void>> reloadCasbinPolicies() {
        casbinPolicySyncService.reloadPolicies();
        return ResponseEntity.ok(ApiResponse.success("Casbin policies reloaded", null));
    }

    @PostMapping("/casbin/sync")
    @Operation(summary = "Full sync of Casbin policies from database")
    public ResponseEntity<ApiResponse<Void>> syncCasbinPolicies() {
        casbinPolicySyncService.syncAllPolicies();
        return ResponseEntity.ok(ApiResponse.success("Casbin policies synced", null));
    }

    @GetMapping("/casbin/policies")
    @Operation(summary = "Get all Casbin policies (debug)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCasbinPolicies() {
        Map<String, Object> policies = new HashMap<>();
        policies.put("policies", casbinPolicySyncService.getAllPolicies());
        policies.put("groupings", casbinPolicySyncService.getAllGroupingPolicies());
        return ResponseEntity.ok(ApiResponse.success(policies));
    }
}
