package com.github.mwiede.metrics.feign;

import static com.codahale.metrics.MetricRegistry.name;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;

import feign.InvocationHandlerFactory;
import feign.Target;

/**
 * A decorator class, which takes all methods given in {@link InvocationHandlerFactory#create(Target, Map)} and
 * initializes a metric for each annotations of {@link Timed}, {@link Metered} or {@link ExceptionMetered} into the
 * global {@link MetricRegistry}. Additionally, it triggers the metric during invocation of the {@link
 * feign.InvocationHandlerFactory.MethodHandler}s.
 * <p>
 * This class is inspired by {@link com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener}.
 */
public class FeignOutboundMetricsDecorator implements InvocationHandlerFactory {

    private final MetricRegistry metricRegistry;
    private final InvocationHandlerFactory original;

    private ConcurrentMap<Method, Timer> timers = new ConcurrentHashMap<>();
    private ConcurrentMap<Method, Meter> meters = new ConcurrentHashMap<>();
    private ConcurrentMap<Method, ExceptionMeterMetric> exceptionMeters = new ConcurrentHashMap<>();

    public FeignOutboundMetricsDecorator(final InvocationHandlerFactory original, final MetricRegistry metricRegistry) {
        this.original = original;
        this.metricRegistry = metricRegistry;
    }

    /**
     * A private class to maintain the metric for a method annotated with the
     * {@link ExceptionMetered} annotation, which needs to maintain both a meter
     * and a cause for which the meter should be updated.
     */
    private static class ExceptionMeterMetric {
        public final Meter meter;
        public final Class<? extends Throwable> cause;

        public ExceptionMeterMetric(final MetricRegistry registry, final Method method,
                final ExceptionMetered exceptionMetered) {
            final String name = chooseName(exceptionMetered.name(), exceptionMetered.absolute(), method,
                    ExceptionMetered.DEFAULT_NAME_SUFFIX);
            this.meter = registry.meter(name);
            this.cause = exceptionMetered.cause();
        }
    }

    /**
     * A decorator, which triggers certain metrics, if found.
     */
    private static class MethodHandlerDecorator implements MethodHandler {

        private final Method method;
        private final MethodHandler methodHandler;
        private final ConcurrentMap<Method, Meter> meters;
        private final ConcurrentMap<Method, Timer> timers;
        private final ConcurrentMap<Method, ExceptionMeterMetric> exceptionMeters;
        private Timer.Context context = null;

        public MethodHandlerDecorator(final Method method, final MethodHandler methodHandler,
                final ConcurrentMap<Method, Meter> meters,
                final ConcurrentMap<Method, ExceptionMeterMetric> exceptionMeters,
                final ConcurrentMap<Method, Timer> timers) {
            this.method = method;
            this.methodHandler = methodHandler;
            this.meters = meters;
            this.exceptionMeters = exceptionMeters;
            this.timers = timers;
        }

        @Override
        public Object invoke(Object[] argv) throws Throwable {
            try {

                final Meter meter = this.meters.get(method);
                if (meter != null) {
                    meter.mark();
                }

                final Timer timer = this.timers.get(method);
                if (timer != null) {
                    this.context = timer.time();
                }

                return methodHandler.invoke(argv);

            } catch (Exception e) {

                final FeignOutboundMetricsDecorator.ExceptionMeterMetric metric =
                        (method != null) ? this.exceptionMeters.get(method) : null;

                if (metric != null && (metric.cause.isAssignableFrom(e.getClass()) || (e.getCause() != null
                        && metric.cause.isAssignableFrom(e.getCause().getClass())))) {
                    metric.meter.mark();
                }

                throw e;
            } finally {
                if (this.context != null) {
                    this.context.close();
                }
            }
        }
    }

    @Override
    public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {

        for (Map.Entry<Method, MethodHandler> entry : dispatch.entrySet()) {

            registerMetricsForMethod(entry.getKey());

            entry.setValue(
                    new MethodHandlerDecorator(entry.getKey(), entry.getValue(), meters, exceptionMeters, timers));
        }

        return original.create(target, dispatch);
    }

    private void registerMetricsForMethod(final Method method) {

        final Timed classLevelTimed = getClassLevelAnnotation(method.getDeclaringClass(), Timed.class);
        final Metered classLevelMetered = getClassLevelAnnotation(method.getDeclaringClass(), Metered.class);
        final ExceptionMetered classLevelExceptionMetered =
                getClassLevelAnnotation(method.getDeclaringClass(), ExceptionMetered.class);

        registerTimedAnnotations(method, classLevelTimed);
        registerMeteredAnnotations(method, classLevelMetered);
        registerExceptionMeteredAnnotations(method, classLevelExceptionMetered);

    }

    private <T extends Annotation> T getClassLevelAnnotation(final Class clazz, final Class<T> annotationClazz) {
        return (T) clazz.getAnnotation(annotationClazz);
    }

    private void registerTimedAnnotations(final Method method, final Timed classLevelTimed) {
        if (classLevelTimed != null) {
            timers.putIfAbsent(method, timerMetric(this.metricRegistry, method, classLevelTimed));
            return;
        }

        final Timed annotation = method.getAnnotation(Timed.class);

        if (annotation != null) {
            timers.putIfAbsent(method, timerMetric(this.metricRegistry, method, annotation));
        }
    }

    private void registerMeteredAnnotations(final Method method, final Metered classLevelMetered) {
        if (classLevelMetered != null) {
            meters.putIfAbsent(method, meterMetric(metricRegistry, method, classLevelMetered));
            return;
        }
        final Metered annotation = method.getAnnotation(Metered.class);

        if (annotation != null) {
            meters.putIfAbsent(method, meterMetric(metricRegistry, method, annotation));
        }
    }

    private void registerExceptionMeteredAnnotations(final Method method,
            final ExceptionMetered classLevelExceptionMetered) {

        if (classLevelExceptionMetered != null) {
            exceptionMeters.putIfAbsent(method,
                    new FeignOutboundMetricsDecorator.ExceptionMeterMetric(metricRegistry, method,
                            classLevelExceptionMetered));
            return;
        }
        final ExceptionMetered annotation = method.getAnnotation(ExceptionMetered.class);

        if (annotation != null) {
            exceptionMeters.putIfAbsent(method,
                    new FeignOutboundMetricsDecorator.ExceptionMeterMetric(metricRegistry, method, annotation));
        }
    }

    private static Timer timerMetric(final MetricRegistry registry, final Method method, final Timed timed) {
        final String name = chooseName(timed.name(), timed.absolute(), method, "Timed");
        return registry.timer(name);
    }

    private static Meter meterMetric(final MetricRegistry registry, final Method method, final Metered metered) {
        final String name = chooseName(metered.name(), metered.absolute(), method, "Metered");
        return registry.meter(name);
    }

    protected static String chooseName(final String explicitName, final boolean absolute, final Method method,
            final String... suffixes) {
        if (explicitName != null && !explicitName.isEmpty()) {
            if (absolute) {
                return explicitName;
            }
            return name(method.getDeclaringClass(), explicitName);
        }

        return name(name(method.getDeclaringClass(), method.getName()), suffixes);
    }
}
