package io.github.vaquarkhan.strands.support;

import java.util.Map;

/**
 * @author Vaquar Khan
 */

public interface AgentCoreContext {

    String sessionId();

    String userId();

    Map<String, String> headers();
}
