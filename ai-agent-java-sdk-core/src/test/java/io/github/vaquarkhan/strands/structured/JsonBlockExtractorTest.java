package io.github.vaquarkhan.strands.structured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JsonBlockExtractorTest {

    @Test
    void extractsJsonObjectFromProse() {
        String text = "Sure, here you go:\n{\"a\":1,\"b\":\"x\"}\nThanks!";
        assertThat(JsonBlockExtractor.extractFirstJsonBlock(text)).isEqualTo("{\"a\":1,\"b\":\"x\"}");
    }

    @Test
    void extractsJsonArrayFromMarkdownFence() {
        String text = "```json\n[{\"a\":1},{\"a\":2}]\n```";
        assertThat(JsonBlockExtractor.extractFirstJsonBlock(text)).isEqualTo("[{\"a\":1},{\"a\":2}]");
    }

    @Test
    void throwsWhenNoJsonFound() {
        assertThatThrownBy(() -> JsonBlockExtractor.extractFirstJsonBlock("nope"))
                .isInstanceOf(StructuredOutputParseException.class);
    }
}

