package io.github.vaquarkhan.strands.structured;

/**
 * Thrown when a model response cannot be parsed into the requested structured output type.
 *
 * @author Vaquar Khan
 */
public class StructuredOutputParseException extends RuntimeException {

    public StructuredOutputParseException(String message) {
        super(message);
    }

    public StructuredOutputParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

