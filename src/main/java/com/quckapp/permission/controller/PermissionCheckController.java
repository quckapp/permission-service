package com.quckapp.permission.controller;

import com.quckapp.permission.dto.PermissionDtos.*;
import com.quckapp.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permissions/check")
@RequiredArgsConstructor
@Tag(name = "Permission Check", description = "Permission checking APIs")
public class PermissionCheckController {

    private final PermissionService permissionService;

    @PostMapping
    @Operation(summary = "Check if user has permission")
    public ResponseEntity<ApiResponse<PermissionCheckResponse>> checkPermission(
            @Valid @RequestBody CheckPermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.checkPermission(request)));
    }
}
