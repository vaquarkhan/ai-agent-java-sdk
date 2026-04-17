package com.example.spring.ai.agent.config;

/**
 * @author Vaquar Khan
 */
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.UrlResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAgentPropertiesValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void missingModelProviderFails() {
        AiAgentProperties properties = validProperties();
        properties.setModelProvider("");
        assertTrue(!validator.validate(properties).isEmpty());
    }

    @Test
    void missingModelIdFails() {
        AiAgentProperties properties = validProperties();
        properties.setModelId("");
        assertTrue(!validator.validate(properties).isEmpty());
    }

    @Test
    void nonPositiveMaxIterationsFails() {
        AiAgentProperties properties = validProperties();
        properties.setMaxIterations(0);
        assertTrue(!validator.validate(properties).isEmpty());
    }

    @Test
    void mutuallyExclusivePromptConfigurationFails() {
        AiAgentProperties properties = validProperties();
        properties.setSystemPrompt("inline");
        properties.setSystemPromptResource(new ByteArrayResource("x".getBytes()));
        assertThrows(IllegalArgumentException.class, () -> AiAgentPropertiesValidator.validateOrThrow(properties));
    }

    @Test
    void httpPromptResourceRejected() throws Exception {
        AiAgentProperties properties = validProperties();
        properties.setSystemPromptResource(new UrlResource("https://evil.local/prompt.txt"));
        assertThrows(IllegalArgumentException.class, () -> AiAgentPropertiesValidator.validateOrThrow(properties));
    }

    @Test
    void defaultsAreApplied() {
        AiAgentProperties properties = new AiAgentProperties();
        assertTrue(properties.isEnabled());
        assertEquals(25, properties.getMaxIterations());
        assertTrue(properties.getToolDiscovery().isEnabled());
    }

    private AiAgentProperties validProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt-test");
        return properties;
    }
}
