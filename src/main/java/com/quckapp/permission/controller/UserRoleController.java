package com.quckapp.permission.controller;

import com.quckapp.permission.dto.PermissionDtos.*;
import com.quckapp.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
@Tag(name = "User Roles", description = "User role assignment APIs")
public class UserRoleController {

    private final PermissionService permissionService;

    @PostMapping
    @Operation(summary = "Assign role to user")
    public ResponseEntity<ApiResponse<UserRoleResponse>> assignRole(
            @Valid @RequestBody AssignRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID grantedBy) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Role assigned", permissionService.assignRole(request, grantedBy)));
    }

    @DeleteMapping("/user/{userId}/role/{roleId}/workspace/{workspaceId}")
    @Operation(summary = "Revoke role from user")
    public ResponseEntity<ApiResponse<Void>> revokeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId,
            @PathVariable UUID workspaceId) {
        permissionService.revokeRole(userId, roleId, workspaceId);
        return ResponseEntity.ok(ApiResponse.success("Role revoked", null));
    }

    @GetMapping("/user/{userId}/workspace/{workspaceId}")
    @Operation(summary = "Get user roles and permissions in workspace")
    public ResponseEntity<ApiResponse<UserPermissionsResponse>> getUserPermissions(
            @PathVariable UUID userId,
            @PathVariable UUID workspaceId) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getUserPermissions(userId, workspaceId)));
    }
}
