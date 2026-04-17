package com.example.spring.ai.agent.approval;

/**
 * Decision returned by an {@link ApprovalManager} for a pending tool call.
 *
 * @author Vaquar Khan
 */
public record ApprovalDecision(boolean approved, String arguments, String denialOutput) {

    private static final String DEFAULT_DENIAL_OUTPUT = "{\"error\":\"approval_denied\"}";

    public static ApprovalDecision approve(String arguments) {
        return new ApprovalDecision(true, arguments, null);
    }

    public static ApprovalDecision deny(String denialOutput) {
        String output = (denialOutput == null || denialOutput.isBlank()) ? DEFAULT_DENIAL_OUTPUT : denialOutput;
        return new ApprovalDecision(false, null, output);
    }
}
