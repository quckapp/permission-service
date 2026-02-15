package com.quckapp.permission.service;

import com.quckapp.permission.TestDataFactory;
import com.quckapp.permission.domain.entity.Permission;
import com.quckapp.permission.domain.entity.Role;
import com.quckapp.permission.domain.entity.UserRole;
import com.quckapp.permission.domain.repository.PermissionRepository;
import com.quckapp.permission.domain.repository.RoleRepository;
import com.quckapp.permission.domain.repository.UserRoleRepository;
import com.quckapp.permission.dto.PermissionDtos.*;
import com.quckapp.permission.exception.DuplicateResourceException;
import com.quckapp.permission.exception.ResourceNotFoundException;
import com.quckapp.permission.kafka.PermissionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Tests")
class PermissionServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PermissionEventPublisher eventPublisher;

    @Mock
    private CasbinPolicySyncService casbinPolicySyncService;

    private PermissionService permissionService;

    private UUID workspaceId;
    private UUID userId;
    private UUID roleId;
    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(
                roleRepository,
                permissionRepository,
                userRoleRepository,
                eventPublisher,
                casbinPolicySyncService
        );

        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();
        testRole = TestDataFactory.createRole(workspaceId, "Member", "Test role", false, 10);
        testRole.setId(roleId);
        testPermission = TestDataFactory.createPermission();
    }

    @Nested
    @DisplayName("Create Role Tests")
    class CreateRoleTests {

        @Test
        @DisplayName("should create role successfully")
        void shouldCreateRoleSuccessfully() {
            CreateRoleRequest request = TestDataFactory.createRoleRequest(workspaceId, "Editor");

            when(roleRepository.existsByWorkspaceIdAndName(workspaceId, "Editor")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
                Role role = invocation.getArgument(0);
                role.setId(UUID.randomUUID());
                return role;
            });

            RoleResponse result = permissionService.createRole(request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Editor");
            assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);

            verify(roleRepository).save(any(Role.class));
            verify(casbinPolicySyncService).syncRolePermissions(any(Role.class));
            verify(eventPublisher).publishRoleCreated(any(Role.class));
        }

        @Test
        @DisplayName("should create role with permissions")
        void shouldCreateRoleWithPermissions() {
            Set<UUID> permissionIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
            CreateRoleRequest request = CreateRoleRequest.builder()
                    .workspaceId(workspaceId)
                    .name("Editor")
                    .description("Can edit")
                    .priority(50)
                    .permissionIds(permissionIds)
                    .build();

            List<Permission> permissions = List.of(
                    TestDataFactory.createPermission("message", "create", "Create messages"),
                    TestDataFactory.createPermission("message", "update", "Update messages")
            );

            when(roleRepository.existsByWorkspaceIdAndName(workspaceId, "Editor")).thenReturn(false);
            when(permissionRepository.findByIdIn(anyList())).thenReturn(permissions);
            when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
                Role role = invocation.getArgument(0);
                role.setId(UUID.randomUUID());
                return role;
            });

            RoleResponse result = permissionService.createRole(request);

            assertThat(result).isNotNull();
            verify(permissionRepository).findByIdIn(anyList());
        }

        @Test
        @DisplayName("should throw exception when role already exists")
        void shouldThrowWhenRoleExists() {
            CreateRoleRequest request = TestDataFactory.createRoleRequest(workspaceId, "Member");

            when(roleRepository.existsByWorkspaceIdAndName(workspaceId, "Member")).thenReturn(true);

            assertThatThrownBy(() -> permissionService.createRole(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Role already exists");

            verify(roleRepository, never()).save(any());
            verify(eventPublisher, never()).publishRoleCreated(any());
        }
    }

    @Nested
    @DisplayName("Get Role Tests")
    class GetRoleTests {

        @Test
        @DisplayName("should get role by ID")
        void shouldGetRoleById() {
            when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.of(testRole));

            RoleResponse result = permissionService.getRoleById(roleId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(roleId);
            assertThat(result.getName()).isEqualTo("Member");
        }

        @Test
        @DisplayName("should throw exception when role not found")
        void shouldThrowWhenRoleNotFound() {
            when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> permissionService.getRoleById(roleId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role not found");
        }

        @Test
        @DisplayName("should get roles by workspace")
        void shouldGetRolesByWorkspace() {
            Role role1 = TestDataFactory.createRole(workspaceId, "Admin", "Admin role", true, 100);
            Role role2 = TestDataFactory.createRole(workspaceId, "Member", "Member role", false, 10);

            when(roleRepository.findByWorkspaceIdWithPermissions(workspaceId))
                    .thenReturn(List.of(role1, role2));

            List<RoleResponse> result = permissionService.getRolesByWorkspace(workspaceId);

            assertThat(result).hasSize(2);
            verify(roleRepository).findByWorkspaceIdWithPermissions(workspaceId);
        }
    }

    @Nested
    @DisplayName("Update Role Tests")
    class UpdateRoleTests {

        @Test
        @DisplayName("should update role successfully")
        void shouldUpdateRoleSuccessfully() {
            UpdateRoleRequest request = UpdateRoleRequest.builder()
                    .name("Updated Name")
                    .description("Updated description")
                    .priority(20)
                    .build();

            when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);

            RoleResponse result = permissionService.updateRole(roleId, request);

            assertThat(result).isNotNull();
            assertThat(testRole.getName()).isEqualTo("Updated Name");
            assertThat(testRole.getDescription()).isEqualTo("Updated description");
            assertThat(testRole.getPriority()).isEqualTo(20);

            verify(casbinPolicySyncService).removeRolePolicies(roleId, workspaceId);
            verify(casbinPolicySyncService).syncRolePermissions(testRole);
            verify(eventPublisher).publishRoleUpdated(testRole);
        }

        @Test
        @DisplayName("should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            String originalDescription = testRole.getDescription();
            int originalPriority = testRole.getPriority();

            UpdateRoleRequest request = UpdateRoleRequest.builder()
                    .name("New Name Only")
                    .build();

            when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);

            permissionService.updateRole(roleId, request);

            assertThat(testRole.getName()).isEqualTo("New Name Only");
            assertThat(testRole.getDescription()).isEqualTo(originalDescription);
            assertThat(testRole.getPriority()).isEqualTo(originalPriority);
        }

        @Test
        @DisplayName("should throw exception when updating system role")
        void shouldThrowWhenUpdatingSystemRole() {
            Role systemRole = TestDataFactory.createSystemRole();
            UpdateRoleRequest request = UpdateRoleRequest.builder().name("New Name").build();

            when(roleRepository.findByIdWithPermissions(any())).thenReturn(Optional.of(systemRole));

            assertThatThrownBy(() -> permissionService.updateRole(systemRole.getId(), request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot modify system role");

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when role not found for update")
        void shouldThrowWhenRoleNotFoundForUpdate() {
            UpdateRoleRequest request = UpdateRoleRequest.builder().name("New Name").build();

            when(roleRepository.findByIdWithPermissions(roleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> permissionService.updateRole(roleId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(roleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Role Tests")
    class DeleteRoleTests {

        @Test
        @DisplayName("should delete role successfully")
        void shouldDeleteRoleSuccessfully() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));

            permissionService.deleteRole(roleId);

            verify(casbinPolicySyncService).removeRolePolicies(roleId, workspaceId);
            verify(roleRepository).delete(testRole);
            verify(eventPublisher).publishRoleDeleted(roleId, workspaceId, "Member");
        }

        @Test
        @DisplayName("should throw exception when deleting system role")
        void shouldThrowWhenDeletingSystemRole() {
            Role systemRole = TestDataFactory.createSystemRole();

            when(roleRepository.findById(any())).thenReturn(Optional.of(systemRole));

            assertThatThrownBy(() -> permissionService.deleteRole(systemRole.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete system role");

            verify(roleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw exception when role not found for delete")
        void shouldThrowWhenRoleNotFoundForDelete() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> permissionService.deleteRole(roleId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(roleRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Permission Operations Tests")
    class PermissionOperationsTests {

        @Test
        @DisplayName("should get all permissions")
        void shouldGetAllPermissions() {
            List<Permission> permissions = List.of(
                    TestDataFactory.createPermission("message", "read", "Read messages"),
                    TestDataFactory.createPermission("message", "create", "Create messages")
            );

            when(permissionRepository.findAll()).thenReturn(permissions);

            List<PermissionResponse> result = permissionService.getAllPermissions();

            assertThat(result).hasSize(2);
            verify(permissionRepository).findAll();
        }

        @Test
        @DisplayName("should get permissions by resource")
        void shouldGetPermissionsByResource() {
            List<Permission> permissions = List.of(
                    TestDataFactory.createPermission("message", "read", "Read messages"),
                    TestDataFactory.createPermission("message", "create", "Create messages")
            );

            when(permissionRepository.findByResource("message")).thenReturn(permissions);

            List<PermissionResponse> result = permissionService.getPermissionsByResource("message");

            assertThat(result).hasSize(2);
            verify(permissionRepository).findByResource("message");
        }
    }

    @Nested
    @DisplayName("User Role Assignment Tests")
    class UserRoleAssignmentTests {

        @Test
        @DisplayName("should assign role successfully")
        void shouldAssignRoleSuccessfully() {
            AssignRoleRequest request = TestDataFactory.assignRoleRequest(userId, roleId, workspaceId);
            UUID grantedBy = UUID.randomUUID();

            when(userRoleRepository.existsByUserIdAndRoleIdAndWorkspaceId(userId, roleId, workspaceId))
                    .thenReturn(false);
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserRoleResponse result = permissionService.assignRole(request, grantedBy);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getRoleId()).isEqualTo(roleId);
            assertThat(result.getRoleName()).isEqualTo("Member");

            verify(casbinPolicySyncService).addUserRoleAssignment(userId, roleId, workspaceId);
            verify(eventPublisher).publishUserRoleAssigned(any(UserRole.class), eq("Member"));
        }

        @Test
        @DisplayName("should assign role with channel scope")
        void shouldAssignRoleWithChannelScope() {
            UUID channelId = UUID.randomUUID();
            AssignRoleRequest request = AssignRoleRequest.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .workspaceId(workspaceId)
                    .channelId(channelId)
                    .build();

            when(userRoleRepository.existsByUserIdAndRoleIdAndWorkspaceId(userId, roleId, workspaceId))
                    .thenReturn(false);
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserRoleResponse result = permissionService.assignRole(request, null);

            assertThat(result.getChannelId()).isEqualTo(channelId);
        }

        @Test
        @DisplayName("should throw exception when role already assigned")
        void shouldThrowWhenRoleAlreadyAssigned() {
            AssignRoleRequest request = TestDataFactory.assignRoleRequest(userId, roleId, workspaceId);

            when(userRoleRepository.existsByUserIdAndRoleIdAndWorkspaceId(userId, roleId, workspaceId))
                    .thenReturn(true);

            assertThatThrownBy(() -> permissionService.assignRole(request, null))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("User already has this role");

            verify(userRoleRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when role not found for assignment")
        void shouldThrowWhenRoleNotFoundForAssignment() {
            AssignRoleRequest request = TestDataFactory.assignRoleRequest(userId, roleId, workspaceId);

            when(userRoleRepository.existsByUserIdAndRoleIdAndWorkspaceId(userId, roleId, workspaceId))
                    .thenReturn(false);
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> permissionService.assignRole(request, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role not found");
        }
    }

    @Nested
    @DisplayName("Revoke Role Tests")
    class RevokeRoleTests {

        @Test
        @DisplayName("should revoke role successfully")
        void shouldRevokeRoleSuccessfully() {
            permissionService.revokeRole(userId, roleId, workspaceId);

            verify(userRoleRepository).deleteByUserIdAndRoleIdAndWorkspaceId(userId, roleId, workspaceId);
            verify(casbinPolicySyncService).removeUserRoleAssignment(userId, roleId, workspaceId);
            verify(eventPublisher).publishUserRoleRevoked(userId, roleId, workspaceId);
        }
    }

    @Nested
    @DisplayName("Get User Permissions Tests")
    class GetUserPermissionsTests {

        @Test
        @DisplayName("should get user permissions")
        void shouldGetUserPermissions() {
            Role roleWithPerms = TestDataFactory.createRoleWithPermissions();
            UserRole userRole = TestDataFactory.createUserRole(userId, roleWithPerms.getId(), workspaceId);
            userRole.setRole(roleWithPerms);

            when(userRoleRepository.findByUserIdAndWorkspaceIdWithRoleAndPermissions(userId, workspaceId))
                    .thenReturn(List.of(userRole));

            UserPermissionsResponse result = permissionService.getUserPermissions(userId, workspaceId);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(result.getRoles()).hasSize(1);
            assertThat(result.getPermissions()).isNotEmpty();
        }

        @Test
        @DisplayName("should return empty permissions when user has no roles")
        void shouldReturnEmptyPermissionsWhenNoRoles() {
            when(userRoleRepository.findByUserIdAndWorkspaceIdWithRoleAndPermissions(userId, workspaceId))
                    .thenReturn(List.of());

            UserPermissionsResponse result = permissionService.getUserPermissions(userId, workspaceId);

            assertThat(result.getRoles()).isEmpty();
            assertThat(result.getPermissions()).isEmpty();
        }

        @Test
        @DisplayName("should aggregate permissions from multiple roles")
        void shouldAggregatePermissionsFromMultipleRoles() {
            Role role1 = TestDataFactory.createRole(workspaceId, "Role1", "First role", false, 10);
            role1.setPermissions(Set.of(TestDataFactory.createPermission("message", "read", "Read")));

            Role role2 = TestDataFactory.createRole(workspaceId, "Role2", "Second role", false, 20);
            role2.setPermissions(Set.of(TestDataFactory.createPermission("channel", "read", "Read")));

            UserRole userRole1 = TestDataFactory.createUserRole(userId, role1.getId(), workspaceId);
            userRole1.setRole(role1);

            UserRole userRole2 = TestDataFactory.createUserRole(userId, role2.getId(), workspaceId);
            userRole2.setRole(role2);

            when(userRoleRepository.findByUserIdAndWorkspaceIdWithRoleAndPermissions(userId, workspaceId))
                    .thenReturn(List.of(userRole1, userRole2));

            UserPermissionsResponse result = permissionService.getUserPermissions(userId, workspaceId);

            assertThat(result.getRoles()).hasSize(2);
            assertThat(result.getPermissions()).contains("message:read", "channel:read");
        }
    }

    @Nested
    @DisplayName("Check Permission Tests")
    class CheckPermissionTests {

        @Test
        @DisplayName("should return allowed when user has permission")
        void shouldReturnAllowedWhenUserHasPermission() {
            CheckPermissionRequest request = CheckPermissionRequest.builder()
                    .userId(userId)
                    .workspaceId(workspaceId)
                    .resource("message")
                    .action("read")
                    .build();

            when(casbinPolicySyncService.checkPermission(userId, workspaceId, "message", "read"))
                    .thenReturn(true);

            PermissionCheckResponse result = permissionService.checkPermission(request);

            assertThat(result.isAllowed()).isTrue();
            assertThat(result.getReason()).contains("granted");
        }

        @Test
        @DisplayName("should check wildcard permission as fallback")
        void shouldCheckWildcardPermissionAsFallback() {
            CheckPermissionRequest request = CheckPermissionRequest.builder()
                    .userId(userId)
                    .workspaceId(workspaceId)
                    .resource("message")
                    .action("delete")
                    .build();

            when(casbinPolicySyncService.checkPermission(userId, workspaceId, "message", "delete"))
                    .thenReturn(false);
            when(casbinPolicySyncService.checkPermission(userId, workspaceId, "message", "*"))
                    .thenReturn(true);

            PermissionCheckResponse result = permissionService.checkPermission(request);

            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("should return denied when user lacks permission")
        void shouldReturnDeniedWhenUserLacksPermission() {
            CheckPermissionRequest request = CheckPermissionRequest.builder()
                    .userId(userId)
                    .workspaceId(workspaceId)
                    .resource("admin")
                    .action("manage")
                    .build();

            when(casbinPolicySyncService.checkPermission(any(), any(), any(), any()))
                    .thenReturn(false);

            PermissionCheckResponse result = permissionService.checkPermission(request);

            assertThat(result.isAllowed()).isFalse();
            assertThat(result.getReason()).contains("denied");
        }
    }
}
