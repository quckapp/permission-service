package com.quckapp.permission.controller;

import com.quckapp.permission.dto.PermissionDtos.*;
import com.quckapp.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Permission definition APIs")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "Get all permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getAllPermissions()));
    }

    @GetMapping("/resource/{resource}")
    @Operation(summary = "Get permissions by resource")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissionsByResource(
            @PathVariable String resource) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getPermissionsByResource(resource)));
    }
}
