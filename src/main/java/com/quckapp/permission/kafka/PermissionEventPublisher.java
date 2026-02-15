package com.quckapp.permission.kafka;

import com.quckapp.permission.domain.entity.Role;
import com.quckapp.permission.domain.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.permission-events:quckapp.permissions.events}")
    private String permissionEventsTopic;

    @Async
    public void publishRoleCreated(Role role) {
        Map<String, Object> data = new HashMap<>();
        data.put("roleId", role.getId().toString());
        data.put("workspaceId", role.getWorkspaceId().toString());
        data.put("roleName", role.getName());
        data.put("description", role.getDescription());
        data.put("isSystem", role.isSystem());
        data.put("priority", role.getPriority());
        publishEvent("ROLE_CREATED", role.getWorkspaceId(), data);
    }

    @Async
    public void publishRoleUpdated(Role role) {
        Map<String, Object> data = new HashMap<>();
        data.put("roleId", role.getId().toString());
        data.put("workspaceId", role.getWorkspaceId().toString());
        data.put("roleName", role.getName());
        data.put("description", role.getDescription());
        data.put("priority", role.getPriority());
        publishEvent("ROLE_UPDATED", role.getWorkspaceId(), data);
    }

    @Async
    public void publishRoleDeleted(UUID roleId, UUID workspaceId, String roleName) {
        Map<String, Object> data = new HashMap<>();
        data.put("roleId", roleId.toString());
        data.put("workspaceId", workspaceId.toString());
        data.put("roleName", roleName);
        publishEvent("ROLE_DELETED", workspaceId, data);
    }

    @Async
    public void publishUserRoleAssigned(UserRole userRole, String roleName) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userRole.getUserId().toString());
        data.put("roleId", userRole.getRoleId().toString());
        data.put("roleName", roleName);
        data.put("workspaceId", userRole.getWorkspaceId().toString());
        if (userRole.getChannelId() != null) {
            data.put("channelId", userRole.getChannelId().toString());
        }
        if (userRole.getGrantedBy() != null) {
            data.put("grantedBy", userRole.getGrantedBy().toString());
        }
        publishEvent("USER_ROLE_ASSIGNED", userRole.getWorkspaceId(), data);
    }

    @Async
    public void publishUserRoleRevoked(UUID userId, UUID roleId, UUID workspaceId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId.toString());
        data.put("roleId", roleId.toString());
        data.put("workspaceId", workspaceId.toString());
        publishEvent("USER_ROLE_REVOKED", workspaceId, data);
    }

    private void publishEvent(String eventType, UUID workspaceId, Map<String, Object> data) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("workspaceId", workspaceId.toString());
            event.put("data", data);
            event.put("timestamp", Instant.now().toString());
            event.put("source", "permission-service");

            kafkaTemplate.send(permissionEventsTopic, workspaceId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event for workspace {}", eventType, workspaceId, ex);
                        } else {
                            log.debug("Published {} event for workspace {}", eventType, workspaceId);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing {} event", eventType, e);
        }
    }
}
