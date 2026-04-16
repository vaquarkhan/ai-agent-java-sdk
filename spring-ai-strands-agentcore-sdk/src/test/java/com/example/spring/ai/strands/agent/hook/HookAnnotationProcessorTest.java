package com.example.spring.ai.strands.agent.hook;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vaquar Khan
 */
class HookAnnotationProcessorTest {

    @Test
    void scanClassWithOnHookMethodRegistersHook() {
        HookRegistry registry = new HookRegistry();
        HookAnnotationProcessor processor = new HookAnnotationProcessor(registry);

        int registered = processor.process(new SingleHookBean());

        assertEquals(1, registered);
        assertEquals(1, registry.hookCount(StrandsHookEvent.BeforeModelCall.class));
    }

    @Test
    void multipleOnHookMethodsOnSameClass() {
        HookRegistry registry = new HookRegistry();
        HookAnnotationProcessor processor = new HookAnnotationProcessor(registry);

        int registered = processor.process(new MultiHookBean());

        assertEquals(2, registered);
        assertEquals(1, registry.hookCount(StrandsHookEvent.BeforeModelCall.class));
        assertEquals(1, registry.hookCount(StrandsHookEvent.AfterToolCall.class));
    }

    @Test
    void methodWithWrongParameterTypeIsSkipped() {
        HookRegistry registry = new HookRegistry();
        HookAnnotationProcessor processor = new HookAnnotationProcessor(registry);

        int registered = processor.process(new WrongParamBean());

        assertEquals(0, registered);
        assertEquals(0, registry.hookCount(StrandsHookEvent.BeforeModelCall.class));
    }

    @Test
    void methodWithNoParametersIsSkipped() {
        HookRegistry registry = new HookRegistry();
        HookAnnotationProcessor processor = new HookAnnotationProcessor(registry);

        int registered = processor.process(new NoParamBean());

        assertEquals(0, registered);
    }

    @Test
    void methodWithTooManyParametersIsSkipped() {
        HookRegistry registry = new HookRegistry();
        HookAnnotationProcessor processor = new HookAnnotationProcessor(registry);

        int registered = processor.process(new TooManyParamsBean());

        assertEquals(0, registered);
    }

    @Test
    void nullBeanReturnsZero() {
        HookRegistry registry = new HookRegistry();
        HookAnnotationProcessor processor = new HookAnnotationProcessor(registry);

        assertEquals(0, processor.process(null));
    }

    @Test
    void classWithNoAnnotationsReturnsZero() {
        HookRegistry registry = new HookRegistry();
        HookAnnotationProcessor processor = new HookAnnotationProcessor(registry);

        assertEquals(0, processor.process(new NoAnnotationBean()));
    }

    @Test
    void registeredHookIsInvokedOnDispatch() {
        HookRegistry registry = new HookRegistry();
        HookAnnotationProcessor processor = new HookAnnotationProcessor(registry);

        CapturingHookBean bean = new CapturingHookBean();
        processor.process(bean);

        registry.dispatch(new StrandsHookEvent.BeforeModelCall(1, List.of()));
        assertEquals(1, bean.events.size());
    }

    // --- Test beans ---

    static class SingleHookBean {
        @OnHook(StrandsHookEvent.BeforeModelCall.class)
        public void onBeforeModel(StrandsHookEvent event) {
            // no-op
        }
    }

    static class MultiHookBean {
        @OnHook(StrandsHookEvent.BeforeModelCall.class)
        public void onBeforeModel(StrandsHookEvent event) {
            // no-op
        }

        @OnHook(StrandsHookEvent.AfterToolCall.class)
        public void onAfterTool(StrandsHookEvent event) {
            // no-op
        }
    }

    static class WrongParamBean {
        @OnHook(StrandsHookEvent.BeforeModelCall.class)
        public void onBeforeModel(String notAnEvent) {
            // wrong parameter type
        }
    }

    static class NoParamBean {
        @OnHook(StrandsHookEvent.BeforeModelCall.class)
        public void onBeforeModel() {
            // no parameters
        }
    }

    static class TooManyParamsBean {
        @OnHook(StrandsHookEvent.BeforeModelCall.class)
        public void onBeforeModel(StrandsHookEvent event, String extra) {
            // too many parameters
        }
    }

    static class NoAnnotationBean {
        public void someMethod(StrandsHookEvent event) {
            // no annotation
        }
    }

    static class CapturingHookBean {
        final List<StrandsHookEvent> events = new ArrayList<>();

        @OnHook(StrandsHookEvent.BeforeModelCall.class)
        public void onBeforeModel(StrandsHookEvent event) {
            events.add(event);
        }
    }
}
