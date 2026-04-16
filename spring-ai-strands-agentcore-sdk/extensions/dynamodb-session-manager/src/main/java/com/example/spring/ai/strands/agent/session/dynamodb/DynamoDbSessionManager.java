package com.example.spring.ai.strands.agent.session.dynamodb;

import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.session.SessionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * DynamoDB-backed {@link SessionManager} that stores the full message list as a JSON attribute.
 *
 * <p>Table layout:
 * <ul>
 *   <li>Partition key: {@code sessionId} (String)</li>
 *   <li>Attribute: {@code messagesJson} (String)</li>
 * </ul>
 *
 * <p>Note: this implementation is intentionally simple and portable; for very large sessions,
 * consider chunking or using S3.
 *
 * @author Vaquar Khan
 */
public class DynamoDbSessionManager implements SessionManager {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbSessionManager.class);
    private static final TypeReference<List<ExecutionMessage>> MESSAGE_LIST_TYPE = new TypeReference<>() {};

    private final DynamoDbClient dynamoDb;
    private final ObjectMapper objectMapper;
    private final String tableName;
    private final String partitionKeyName;
    private final String messagesAttributeName;

    public DynamoDbSessionManager(DynamoDbClient dynamoDb, ObjectMapper objectMapper, String tableName) {
        this(dynamoDb, objectMapper, tableName, "sessionId", "messagesJson");
    }

    public DynamoDbSessionManager(
            DynamoDbClient dynamoDb,
            ObjectMapper objectMapper,
            String tableName,
            String partitionKeyName,
            String messagesAttributeName) {
        this.dynamoDb = Objects.requireNonNull(dynamoDb, "dynamoDb");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.tableName = Objects.requireNonNull(tableName, "tableName");
        this.partitionKeyName = Objects.requireNonNull(partitionKeyName, "partitionKeyName");
        this.messagesAttributeName = Objects.requireNonNull(messagesAttributeName, "messagesAttributeName");
    }

    @Override
    public void save(String sessionId, List<ExecutionMessage> messages) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(messages, "messages");

        try {
            String json = objectMapper.writeValueAsString(messages);
            PutItemRequest req = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            partitionKeyName, AttributeValue.fromS(sessionId),
                            messagesAttributeName, AttributeValue.fromS(json)))
                    .build();
            dynamoDb.putItem(req);
        } catch (Exception e) {
            log.error("Failed to save DynamoDB session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to save DynamoDB session: " + sessionId, e);
        }
    }

    @Override
    public List<ExecutionMessage> load(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");

        GetItemRequest req = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(partitionKeyName, AttributeValue.fromS(sessionId)))
                .consistentRead(true)
                .build();

        var resp = dynamoDb.getItem(req);
        if (!resp.hasItem() || resp.item().isEmpty()) {
            return List.of();
        }

        AttributeValue value = resp.item().get(messagesAttributeName);
        if (value == null || value.s() == null || value.s().isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(value.s(), MESSAGE_LIST_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse DynamoDB session {} messagesJson: {}", sessionId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public void delete(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");

        DeleteItemRequest req = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(partitionKeyName, AttributeValue.fromS(sessionId)))
                .build();
        dynamoDb.deleteItem(req);
    }

    @Override
    public boolean exists(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        GetItemRequest req = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(partitionKeyName, AttributeValue.fromS(sessionId)))
                .projectionExpression(partitionKeyName)
                .consistentRead(false)
                .build();
        var resp = dynamoDb.getItem(req);
        return resp.hasItem() && !resp.item().isEmpty();
    }
}

