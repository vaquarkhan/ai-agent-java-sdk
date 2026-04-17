package com.example.spring.ai.agent.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * @author Vaquar Khan
 */
@ConfigurationProperties(prefix = "ai.agent")
@Validated
public class AiAgentProperties {

    private boolean enabled = true;

    @NotBlank
    private String modelProvider;

    @NotBlank
    @JsonIgnore
    private String modelId;

    @JsonIgnore
    private String systemPrompt;

    @JsonIgnore
    private Resource systemPromptResource;

    @Min(1)
    private int maxIterations = 25;

    @Valid
    private ToolDiscovery toolDiscovery = new ToolDiscovery();

    @Valid
    private Security security = new Security();

    @AssertTrue(message = "ai.agent.system-prompt and ai.agent.system-prompt-resource are mutually exclusive")
    public boolean isSystemPromptConfigurationValid() {
        return systemPrompt == null || systemPrompt.isBlank() || systemPromptResource == null;
    }

    @AssertTrue(message = "ai.agent.system-prompt-resource cannot use http:// or https://")
    public boolean isSystemPromptResourceSchemeAllowed() {
        if (systemPromptResource == null) {
            return true;
        }
        String description = systemPromptResource.getDescription();
        if (description == null) {
            return true;
        }
        String normalized = description.toLowerCase(Locale.ROOT);
        return !normalized.contains("http://") && !normalized.contains("https://");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModelProvider() {
        return modelProvider;
    }

    public void setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Resource getSystemPromptResource() {
        return systemPromptResource;
    }

    public void setSystemPromptResource(Resource systemPromptResource) {
        this.systemPromptResource = systemPromptResource;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public ToolDiscovery getToolDiscovery() {
        return toolDiscovery;
    }

    public void setToolDiscovery(ToolDiscovery toolDiscovery) {
        this.toolDiscovery = toolDiscovery;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public static class ToolDiscovery {
        private boolean enabled = true;
        private List<String> includePatterns = new ArrayList<>();
        private List<String> excludePatterns = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getIncludePatterns() {
            return includePatterns;
        }

        public void setIncludePatterns(List<String> includePatterns) {
            this.includePatterns = includePatterns == null ? new ArrayList<>() : new ArrayList<>(includePatterns);
        }

        public List<String> getExcludePatterns() {
            return excludePatterns;
        }

        public void setExcludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns == null ? new ArrayList<>() : new ArrayList<>(excludePatterns);
        }
    }

    public static class Security {
        @Min(1)
        private int maxToolArgumentBytes = 65536;

        @Min(1)
        private int toolTimeoutSeconds = 60;

        @Min(0)
        private int toolRateLimit = 0;

        private boolean sanitizeToolOutput = false;

        @Min(1)
        private int traceMaxOutputLength = 1024;

        private boolean traceIncludeToolData = false;

        public int getMaxToolArgumentBytes() {
            return maxToolArgumentBytes;
        }

        public void setMaxToolArgumentBytes(int maxToolArgumentBytes) {
            this.maxToolArgumentBytes = maxToolArgumentBytes;
        }

        public int getToolTimeoutSeconds() {
            return toolTimeoutSeconds;
        }

        public void setToolTimeoutSeconds(int toolTimeoutSeconds) {
            this.toolTimeoutSeconds = toolTimeoutSeconds;
        }

        public int getToolRateLimit() {
            return toolRateLimit;
        }

        public void setToolRateLimit(int toolRateLimit) {
            this.toolRateLimit = toolRateLimit;
        }

        public boolean isSanitizeToolOutput() {
            return sanitizeToolOutput;
        }

        public void setSanitizeToolOutput(boolean sanitizeToolOutput) {
            this.sanitizeToolOutput = sanitizeToolOutput;
        }

        public int getTraceMaxOutputLength() {
            return traceMaxOutputLength;
        }

        public void setTraceMaxOutputLength(int traceMaxOutputLength) {
            this.traceMaxOutputLength = traceMaxOutputLength;
        }

        public boolean isTraceIncludeToolData() {
            return traceIncludeToolData;
        }

        public void setTraceIncludeToolData(boolean traceIncludeToolData) {
            this.traceIncludeToolData = traceIncludeToolData;
        }
    }
}
