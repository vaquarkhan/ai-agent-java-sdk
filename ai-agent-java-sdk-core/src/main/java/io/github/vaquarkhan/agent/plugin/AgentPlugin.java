package io.github.vaquarkhan.agent.plugin;

import io.github.vaquarkhan.agent.AiAgent;

/**
 * Extension point for composable agent plugins.
 *
 * <p>A plugin bundles initialization logic that can register hooks, add tools,
 * or configure the agent after construction. Plugins are initialized via
 * {@link AiAgent#initPlugins()} after the agent is fully wired.
 *
 * <p>This mirrors the Python Strands SDK {@code Plugin} base class, adapted
 * for the Spring ecosystem where plugins are typically Spring beans.
 *
 * @author Vaquar Khan
 */
public interface AgentPlugin {

    /**
     * Initialize this plugin with the given agent.
     *
     * <p>Implementations can register hooks via {@code agent.getHookRegistry()},
     * configure session managers, or perform other setup.
     *
     * @param agent the fully constructed agent instance
     */
    void init(AiAgent agent);

    /**
     * Returns a human-readable name for this plugin.
     *
     * @return the plugin name
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
