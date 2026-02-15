package com.quckapp.permission;

import com.quckapp.permission.domain.entity.Permission;
import com.quckapp.permission.domain.entity.Role;
import com.quckapp.permission.domain.entity.UserRole;
import com.quckapp.permission.dto.PermissionDtos.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestDataFactory {

    public static final UUID WORKSPACE_ID = UUID.randomUUID();
    public static final UUID USER_ID = UUID.randomUUID();
    public static final UUID ROLE_ID = UUID.randomUUID();
    public static final UUID PERMISSION_ID = UUID.randomUUID();
    public static final UUID CHANNEL_ID = UUID.randomUUID();
    public static final UUID GRANTED_BY = UUID.randomUUID();

    public static Permission createPermission() {
        return createPermission("message", "read", "Read messages");
    }

    public static Permission createPermission(String resource, String action, String description) {
        return Permission.builder()
                .id(UUID.randomUUID())
                .resource(resource)
                .action(action)
                .description(description)
                .build();
    }

    public static Set<Permission> createPermissionSet() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(createPermission("message", "read", "Read messages"));
        permissions.add(createPermission("message", "create", "Create messages"));
        permissions.add(createPermission("channel", "read", "Read channels"));
        return permissions;
    }

    public static Role createRole() {
        return createRole(WORKSPACE_ID, "Member", "Standard member role", false, 10);
    }

    public static Role createRole(UUID workspaceId, String name, String description, boolean isSystem, int priority) {
        return Role.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name(name)
                .description(description)
                .isSystem(isSystem)
                .priority(priority)
                .permissions(new HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Role createRoleWithPermissions() {
        Role role = createRole();
        role.setPermissions(createPermissionSet());
        return role;
    }

    public static Role createSystemRole() {
        return createRole(WORKSPACE_ID, "Admin", "System admin role", true, 100);
    }

    public static UserRole createUserRole() {
        return createUserRole(USER_ID, ROLE_ID, WORKSPACE_ID);
    }

    public static UserRole createUserRole(UUID userId, UUID roleId, UUID workspaceId) {
        return UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .workspaceId(workspaceId)
                .channelId(null)
                .grantedBy(GRANTED_BY)
                .grantedAt(Instant.now())
                .build();
    }

    public static UserRole createUserRoleWithChannel(UUID channelId) {
        return UserRole.builder()
                .userId(USER_ID)
                .roleId(ROLE_ID)
                .workspaceId(WORKSPACE_ID)
                .channelId(channelId)
                .grantedBy(GRANTED_BY)
                .grantedAt(Instant.now())
                .build();
    }

    public static CreateRoleRequest createRoleRequest() {
        return CreateRoleRequest.builder()
                .workspaceId(WORKSPACE_ID)
                .name("Editor")
                .description("Can edit content")
                .priority(50)
                .permissionIds(null)
                .build();
    }

    public static CreateRoleRequest createRoleRequest(UUID workspaceId, String name) {
        return CreateRoleRequest.builder()
                .workspaceId(workspaceId)
                .name(name)
                .description("Test role")
                .priority(10)
                .permissionIds(null)
                .build();
    }

    public static UpdateRoleRequest updateRoleRequest() {
        return UpdateRoleRequest.builder()
                .name("Updated Role")
                .description("Updated description")
                .priority(20)
                .build();
    }

    public static AssignRoleRequest assignRoleRequest() {
        return AssignRoleRequest.builder()
                .userId(USER_ID)
                .roleId(ROLE_ID)
                .workspaceId(WORKSPACE_ID)
                .channelId(null)
                .build();
    }

    public static AssignRoleRequest assignRoleRequest(UUID userId, UUID roleId, UUID workspaceId) {
        return AssignRoleRequest.builder()
                .userId(userId)
                .roleId(roleId)
                .workspaceId(workspaceId)
                .channelId(null)
                .build();
    }

    public static CheckPermissionRequest checkPermissionRequest() {
        return CheckPermissionRequest.builder()
                .userId(USER_ID)
                .workspaceId(WORKSPACE_ID)
                .resource("message")
                .action("read")
                .channelId(null)
                .build();
    }

    public static CheckPermissionRequest checkPermissionRequest(String resource, String action) {
        return CheckPermissionRequest.builder()
                .userId(USER_ID)
                .workspaceId(WORKSPACE_ID)
                .resource(resource)
                .action(action)
                .channelId(null)
                .build();
    }

    public static RoleResponse createRoleResponse() {
        return RoleResponse.builder()
                .id(ROLE_ID)
                .workspaceId(WORKSPACE_ID)
                .name("Member")
                .description("Standard member role")
                .isSystem(false)
                .priority(10)
                .permissions(new HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static PermissionResponse createPermissionResponse() {
        return PermissionResponse.builder()
                .id(PERMISSION_ID)
                .resource("message")
                .action("read")
                .description("Read messages")
                .build();
    }

    public static UserRoleResponse createUserRoleResponse() {
        return UserRoleResponse.builder()
                .userId(USER_ID)
                .roleId(ROLE_ID)
                .roleName("Member")
                .workspaceId(WORKSPACE_ID)
                .channelId(null)
                .grantedBy(GRANTED_BY)
                .grantedAt(Instant.now())
                .build();
    }

    public static UserPermissionsResponse createUserPermissionsResponse() {
        return UserPermissionsResponse.builder()
                .userId(USER_ID)
                .workspaceId(WORKSPACE_ID)
                .roles(java.util.List.of(createRoleResponse()))
                .permissions(Set.of("message:read", "message:create", "channel:read"))
                .build();
    }

    public static PermissionCheckResponse createPermissionCheckResponse(boolean allowed) {
        return PermissionCheckResponse.builder()
                .allowed(allowed)
                .reason(allowed ? "Permission granted" : "Permission denied")
                .build();
    }
}
