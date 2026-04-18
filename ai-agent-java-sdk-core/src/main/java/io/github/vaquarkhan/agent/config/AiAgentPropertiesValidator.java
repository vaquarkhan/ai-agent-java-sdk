package io.github.vaquarkhan.agent.config;

import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * @author Vaquar Khan
 */

public final class AiAgentPropertiesValidator {

    private AiAgentPropertiesValidator() {}

    public static void validateOrThrow(AiAgentProperties properties) {
        boolean hasInlinePrompt = properties.getSystemPrompt() != null && !properties.getSystemPrompt().isBlank();
        Resource resource = properties.getSystemPromptResource();
        boolean hasPromptResource = resource != null;
        if (hasInlinePrompt && hasPromptResource) {
            throw new IllegalArgumentException(
                    "ai.agent.system-prompt and ai.agent.system-prompt-resource are mutually exclusive");
        }
        if (hasPromptResource) {
            if (resource instanceof UrlResource urlResource) {
                String protocol = urlResource.getURL().getProtocol();
                if ("http".equals(protocol) || "https".equals(protocol)) {
                    throw new IllegalArgumentException(
                            "ai.agent.system-prompt-resource must not use http(s) URLs");
                }
            }
            String description = resource.getDescription();
            if (description != null) {
                String normalized = description.toLowerCase(Locale.ROOT);
                if (normalized.contains("http://") || normalized.contains("https://")) {
                    throw new IllegalArgumentException(
                            "ai.agent.system-prompt-resource must not use http(s) URLs");
                }
            }
        }
    }
}
