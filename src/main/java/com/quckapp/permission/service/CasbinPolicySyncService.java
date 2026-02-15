package com.quckapp.permission.service;

import com.quckapp.permission.domain.entity.Permission;
import com.quckapp.permission.domain.entity.Role;
import com.quckapp.permission.domain.entity.UserRole;
import com.quckapp.permission.domain.repository.RoleRepository;
import com.quckapp.permission.domain.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.SyncedEnforcer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasbinPolicySyncService {

    private final SyncedEnforcer enforcer;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void syncPoliciesOnStartup() {
        log.info("Syncing Casbin policies on application startup...");
        try {
            syncAllPolicies();
            log.info("Casbin policies synced successfully");
        } catch (Exception e) {
            log.error("Failed to sync Casbin policies on startup", e);
        }
    }

    public void syncAllPolicies() {
        // Clear existing policies
        enforcer.clearPolicy();

        // Sync all roles and their permissions
        List<Role> roles = roleRepository.findAll();
        for (Role role : roles) {
            Role roleWithPerms = roleRepository.findByIdWithPermissions(role.getId()).orElse(role);
            syncRolePermissions(roleWithPerms);
        }

        // Sync all user-role assignments
        List<UserRole> userRoles = userRoleRepository.findAll();
        for (UserRole userRole : userRoles) {
            addUserRoleAssignment(userRole.getUserId(), userRole.getRoleId(), userRole.getWorkspaceId());
        }

        log.info("Synced {} roles and {} user-role assignments", roles.size(), userRoles.size());
    }

    public void syncRolePermissions(Role role) {
        String workspaceId = role.getWorkspaceId().toString();
        String roleId = role.getId().toString();

        // Add policies for each permission
        for (Permission permission : role.getPermissions()) {
            String resource = permission.getResource();
            String action = permission.getAction();

            // Add policy: role has permission on resource/action in workspace
            boolean added = enforcer.addPolicy(roleId, workspaceId, resource, action);
            if (added) {
                log.debug("Added policy: role={}, workspace={}, resource={}, action={}",
                    roleId, workspaceId, resource, action);
            }
        }
    }

    public void addUserRoleAssignment(UUID userId, UUID roleId, UUID workspaceId) {
        // Add grouping policy: user has role in workspace
        boolean added = enforcer.addGroupingPolicy(
            userId.toString(),
            roleId.toString(),
            workspaceId.toString()
        );
        if (added) {
            log.debug("Added grouping: user={}, role={}, workspace={}", userId, roleId, workspaceId);
        }
    }

    public void removeUserRoleAssignment(UUID userId, UUID roleId, UUID workspaceId) {
        boolean removed = enforcer.removeGroupingPolicy(
            userId.toString(),
            roleId.toString(),
            workspaceId.toString()
        );
        if (removed) {
            log.debug("Removed grouping: user={}, role={}, workspace={}", userId, roleId, workspaceId);
        }
    }

    public void removeRolePolicies(UUID roleId, UUID workspaceId) {
        String roleIdStr = roleId.toString();
        String workspaceIdStr = workspaceId.toString();

        // Remove all policies for this role
        enforcer.removeFilteredPolicy(0, roleIdStr, workspaceIdStr);

        // Remove all groupings for this role
        enforcer.removeFilteredGroupingPolicy(1, roleIdStr, workspaceIdStr);

        log.debug("Removed all policies for role={} in workspace={}", roleId, workspaceId);
    }

    public boolean checkPermission(UUID userId, UUID workspaceId, String resource, String action) {
        return enforcer.enforce(
            userId.toString(),
            workspaceId.toString(),
            resource,
            action
        );
    }

    @Async
    public void reloadPolicies() {
        log.info("Reloading Casbin policies...");
        enforcer.loadPolicy();
        log.info("Casbin policies reloaded");
    }

    public List<List<String>> getAllPolicies() {
        return enforcer.getPolicy();
    }

    public List<List<String>> getAllGroupingPolicies() {
        return enforcer.getGroupingPolicy();
    }
}
