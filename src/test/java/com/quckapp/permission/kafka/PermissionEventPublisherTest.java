package com.quckapp.permission.kafka;

import com.quckapp.permission.TestDataFactory;
import com.quckapp.permission.domain.entity.Role;
import com.quckapp.permission.domain.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionEventPublisher Tests")
class PermissionEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PermissionEventPublisher eventPublisher;

    private Role testRole;
    private UserRole testUserRole;
    private UUID workspaceId;
    private UUID roleId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        eventPublisher = new PermissionEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(eventPublisher, "permissionEventsTopic", "quckapp.permissions.events");

        workspaceId = UUID.randomUUID();
        roleId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testRole = TestDataFactory.createRole(workspaceId, "Member", "Test role", false, 10);
        testRole.setId(roleId);

        testUserRole = TestDataFactory.createUserRole(userId, roleId, workspaceId);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockKafkaSendSuccess() {
        CompletableFuture future = new CompletableFuture();
        future.complete(null);
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockKafkaSendFailure() {
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);
    }

    @Nested
    @DisplayName("publishRoleCreated Tests")
    class PublishRoleCreatedTests {

        @Test
        @DisplayName("should publish ROLE_CREATED event with correct data")
        void shouldPublishRoleCreatedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishRoleCreated(testRole);

            verify(kafkaTemplate).send(eq("quckapp.permissions.events"), eq(workspaceId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            assertThat(event.get("eventType")).isEqualTo("ROLE_CREATED");
            assertThat(event.get("workspaceId")).isEqualTo(workspaceId.toString());
            assertThat(event.get("source")).isEqualTo("permission-service");
            assertThat(event.get("timestamp")).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("roleId")).isEqualTo(roleId.toString());
            assertThat(data.get("roleName")).isEqualTo("Member");
            assertThat(data.get("description")).isEqualTo("Test role");
            assertThat(data.get("isSystem")).isEqualTo(false);
            assertThat(data.get("priority")).isEqualTo(10);
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> eventPublisher.publishRoleCreated(testRole))
                    .doesNotThrowAnyException();

            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("publishRoleUpdated Tests")
    class PublishRoleUpdatedTests {

        @Test
        @DisplayName("should publish ROLE_UPDATED event with correct data")
        void shouldPublishRoleUpdatedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishRoleUpdated(testRole);

            verify(kafkaTemplate).send(eq("quckapp.permissions.events"), eq(workspaceId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            assertThat(event.get("eventType")).isEqualTo("ROLE_UPDATED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("roleId")).isEqualTo(roleId.toString());
            assertThat(data.get("roleName")).isEqualTo("Member");
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> eventPublisher.publishRoleUpdated(testRole))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishRoleDeleted Tests")
    class PublishRoleDeletedTests {

        @Test
        @DisplayName("should publish ROLE_DELETED event with correct data")
        void shouldPublishRoleDeletedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishRoleDeleted(roleId, workspaceId, "Member");

            verify(kafkaTemplate).send(eq("quckapp.permissions.events"), eq(workspaceId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            assertThat(event.get("eventType")).isEqualTo("ROLE_DELETED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("roleId")).isEqualTo(roleId.toString());
            assertThat(data.get("workspaceId")).isEqualTo(workspaceId.toString());
            assertThat(data.get("roleName")).isEqualTo("Member");
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> eventPublisher.publishRoleDeleted(roleId, workspaceId, "Member"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishUserRoleAssigned Tests")
    class PublishUserRoleAssignedTests {

        @Test
        @DisplayName("should publish USER_ROLE_ASSIGNED event with correct data")
        void shouldPublishUserRoleAssignedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishUserRoleAssigned(testUserRole, "Member");

            verify(kafkaTemplate).send(eq("quckapp.permissions.events"), eq(workspaceId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            assertThat(event.get("eventType")).isEqualTo("USER_ROLE_ASSIGNED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("userId")).isEqualTo(userId.toString());
            assertThat(data.get("roleId")).isEqualTo(roleId.toString());
            assertThat(data.get("roleName")).isEqualTo("Member");
            assertThat(data.get("workspaceId")).isEqualTo(workspaceId.toString());
        }

        @Test
        @DisplayName("should include channelId when present")
        void shouldIncludeChannelIdWhenPresent() {
            mockKafkaSendSuccess();
            UUID channelId = UUID.randomUUID();
            UserRole userRoleWithChannel = TestDataFactory.createUserRoleWithChannel(channelId);
            userRoleWithChannel.setUserId(userId);
            userRoleWithChannel.setRoleId(roleId);
            userRoleWithChannel.setWorkspaceId(workspaceId);

            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishUserRoleAssigned(userRoleWithChannel, "Member");

            verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventCaptor.getValue().get("data");
            assertThat(data.get("channelId")).isEqualTo(channelId.toString());
        }

        @Test
        @DisplayName("should include grantedBy when present")
        void shouldIncludeGrantedByWhenPresent() {
            mockKafkaSendSuccess();
            UUID grantedBy = UUID.randomUUID();
            testUserRole.setGrantedBy(grantedBy);

            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishUserRoleAssigned(testUserRole, "Member");

            verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventCaptor.getValue().get("data");
            assertThat(data.get("grantedBy")).isEqualTo(grantedBy.toString());
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> eventPublisher.publishUserRoleAssigned(testUserRole, "Member"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishUserRoleRevoked Tests")
    class PublishUserRoleRevokedTests {

        @Test
        @DisplayName("should publish USER_ROLE_REVOKED event with correct data")
        void shouldPublishUserRoleRevokedEvent() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishUserRoleRevoked(userId, roleId, workspaceId);

            verify(kafkaTemplate).send(eq("quckapp.permissions.events"), eq(workspaceId.toString()), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            assertThat(event.get("eventType")).isEqualTo("USER_ROLE_REVOKED");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            assertThat(data.get("userId")).isEqualTo(userId.toString());
            assertThat(data.get("roleId")).isEqualTo(roleId.toString());
            assertThat(data.get("workspaceId")).isEqualTo(workspaceId.toString());
        }

        @Test
        @DisplayName("should handle send failure gracefully")
        void shouldHandleSendFailureGracefully() {
            mockKafkaSendFailure();

            assertThatCode(() -> eventPublisher.publishUserRoleRevoked(userId, roleId, workspaceId))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Event Structure Tests")
    class EventStructureTests {

        @Test
        @DisplayName("should include timestamp in all events")
        void shouldIncludeTimestampInAllEvents() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishRoleCreated(testRole);

            verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            assertThat(event.get("timestamp")).isNotNull();
            assertThat(event.get("timestamp").toString()).isNotEmpty();
        }

        @Test
        @DisplayName("should include source in all events")
        void shouldIncludeSourceInAllEvents() {
            mockKafkaSendSuccess();
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

            eventPublisher.publishRoleUpdated(testRole);

            verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
            Map<String, Object> event = eventCaptor.getValue();

            assertThat(event.get("source")).isEqualTo("permission-service");
        }

        @Test
        @DisplayName("should use workspaceId as Kafka key")
        void shouldUseWorkspaceIdAsKafkaKey() {
            mockKafkaSendSuccess();

            eventPublisher.publishRoleDeleted(roleId, workspaceId, "Member");

            verify(kafkaTemplate).send(eq("quckapp.permissions.events"), eq(workspaceId.toString()), any());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle KafkaTemplate exception gracefully")
        void shouldHandleKafkaTemplateExceptionGracefully() {
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willThrow(new RuntimeException("Kafka unavailable"));

            assertThatCode(() -> eventPublisher.publishRoleCreated(testRole))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle multiple consecutive failures")
        void shouldHandleMultipleConsecutiveFailures() {
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willThrow(new RuntimeException("Kafka unavailable"));

            assertThatCode(() -> {
                eventPublisher.publishRoleCreated(testRole);
                eventPublisher.publishRoleUpdated(testRole);
                eventPublisher.publishRoleDeleted(roleId, workspaceId, "Member");
            }).doesNotThrowAnyException();

            verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
        }
    }
}
