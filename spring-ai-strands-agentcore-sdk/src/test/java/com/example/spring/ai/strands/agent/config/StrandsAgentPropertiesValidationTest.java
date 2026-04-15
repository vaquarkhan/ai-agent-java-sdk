package com.example.spring.ai.strands.agent.config;

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

class StrandsAgentPropertiesValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void missingModelProviderFails() {
        StrandsAgentProperties properties = validProperties();
        properties.setModelProvider("");
        assertTrue(!validator.validate(properties).isEmpty());
    }

    @Test
    void missingModelIdFails() {
        StrandsAgentProperties properties = validProperties();
        properties.setModelId("");
        assertTrue(!validator.validate(properties).isEmpty());
    }

    @Test
    void nonPositiveMaxIterationsFails() {
        StrandsAgentProperties properties = validProperties();
        properties.setMaxIterations(0);
        assertTrue(!validator.validate(properties).isEmpty());
    }

    @Test
    void mutuallyExclusivePromptConfigurationFails() {
        StrandsAgentProperties properties = validProperties();
        properties.setSystemPrompt("inline");
        properties.setSystemPromptResource(new ByteArrayResource("x".getBytes()));
        assertThrows(IllegalArgumentException.class, () -> StrandsAgentPropertiesValidator.validateOrThrow(properties));
    }

    @Test
    void httpPromptResourceRejected() throws Exception {
        StrandsAgentProperties properties = validProperties();
        properties.setSystemPromptResource(new UrlResource("https://evil.local/prompt.txt"));
        assertThrows(IllegalArgumentException.class, () -> StrandsAgentPropertiesValidator.validateOrThrow(properties));
    }

    @Test
    void defaultsAreApplied() {
        StrandsAgentProperties properties = new StrandsAgentProperties();
        assertTrue(properties.isEnabled());
        assertEquals(25, properties.getMaxIterations());
        assertTrue(properties.getToolDiscovery().isEnabled());
    }

    private StrandsAgentProperties validProperties() {
        StrandsAgentProperties properties = new StrandsAgentProperties();
        properties.setModelProvider("openai");
        properties.setModelId("gpt-test");
        return properties;
    }
}
