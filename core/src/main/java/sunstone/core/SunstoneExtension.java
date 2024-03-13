package sunstone.core;


import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.util.StringUtils;
import sunstone.annotation.AbstractSetupTask;
import sunstone.annotation.Setup;
import sunstone.annotation.SunstoneCloudDeployAnnotation;
import sunstone.annotation.CloudResourceIdentificationAnnotation;
import sunstone.annotation.Deployment;
import sunstone.core.api.SunstoneArchiveDeployer;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.spi.SunstoneArchiveDeployerProvider;
import sunstone.core.spi.SunstoneCloudDeployerProvider;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static sunstone.core.SunstoneStore.get;

/**
 * Extension handles cloud deployment annotations, serves and orchestrate/delegate all the work,
 * initialize resources, ...
 * <p>
 * Uses {@link AutoCloseable} LIFO queue (a.k.a. Stack) for registering resources that needs to be cleaned/closed -
 * closing clients, cleaning Cloud resources ... Class that creates
 * a resource that needs to be cleaned/closed is also responsible for registering it.
 */
public class SunstoneExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor {
    static ServiceLoader<SunstoneArchiveDeployerProvider> sunstoneArchiveDeployerProviderLoader = ServiceLoader.load(SunstoneArchiveDeployerProvider.class);
    static ServiceLoader<SunstoneCloudDeployerProvider> sunstoneCloudDeployerProviderLoader = ServiceLoader.load(SunstoneCloudDeployerProvider.class);
    static ServiceLoader<SunstoneResourceInjectorProvider> sunstoneResourceInjectorProviderLoader = ServiceLoader.load(SunstoneResourceInjectorProvider.class);

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        handleCloudDeployAnnotations(ctx);
        if (ctx.getRequiredTestClass().getAnnotationsByType(Setup.class).length > 0) {
            handleSetup(ctx);
        }
        performDeploymentOperation(ctx);
        injectStaticResources(ctx, ctx.getRequiredTestClass());
    }

    static void handleSetup(ExtensionContext ctx) throws IllegalArgumentSunstoneException {
        SunstoneStore store = new SunstoneStore(ctx);
        if (ctx.getRequiredTestClass().getAnnotationsByType(Setup.class).length != 1) {
            throw new IllegalArgumentSunstoneException("Only one setup task may be defined.");
        }
        Setup setup = ctx.getRequiredTestClass().getAnnotationsByType(Setup.class)[0];
        for (Class<? extends AbstractSetupTask> setupTask : setup.value()) {
            injectStaticResources(ctx, setupTask);
            Optional<Constructor<?>> constructor = Arrays.stream(setupTask.getDeclaredConstructors())
                    .filter(c -> c.getParameterCount() == 0)
                    .findAny();
            constructor.orElseThrow(() -> new IllegalArgumentSunstoneException("Setup task must have a constructor with 0 parameters"));
            constructor.get().setAccessible(true);
            AbstractSetupTask abstractSetupTask = null;
            try {
                abstractSetupTask = (AbstractSetupTask) constructor.get().newInstance();
                injectInstanceResources(ctx, abstractSetupTask);
                store.addClosable((AutoCloseable) abstractSetupTask::teardown);
                abstractSetupTask.setup();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected static void handleCloudDeployAnnotations(ExtensionContext ctx) {
        Annotation[] annotations = ctx.getRequiredTestClass().getAnnotations();
        Arrays.stream(ctx.getRequiredTestClass().getAnnotations())
                .filter(ann -> AnnotationUtils.isAnnotatedBy(ann.annotationType(), SunstoneCloudDeployAnnotation.class))
                .forEachOrdered(ann -> {
                    Optional<SunstoneCloudDeployer> deployer = getDeployer(ann);
                    deployer.orElseThrow(() -> new RuntimeException("Unable to load a service via SPI that handles " + ann.annotationType() + " annotation."));
                    try {
                        deployer.get().deployAndRegisterForUndeploy(ctx);
                    } catch (SunstoneException e) {
                        throw new RuntimeException("Unable to deploy " + ann, e);
                    }

                });
    }

    static <A extends Annotation> Optional<SunstoneCloudDeployer> getDeployer(Annotation annotation) {
        for (SunstoneCloudDeployerProvider sunstoneCloudDeployerProvider : sunstoneCloudDeployerProviderLoader) {
            Optional<SunstoneCloudDeployer> deployer = sunstoneCloudDeployerProvider.create(annotation);
            if (deployer.isPresent()) {
                return deployer;
            }
        }
        return Optional.empty();
    }

    static Optional<SunstoneResourceInjector> getSunstoneResourceInjector(Field field) {
        for (SunstoneResourceInjectorProvider provider : sunstoneResourceInjectorProviderLoader) {
            Optional<SunstoneResourceInjector> injector = provider.create(field);
            if (injector.isPresent()) {
                return injector;
            }
        }
        return Optional.empty();
    }

    static Optional<Annotation> getAndCheckInjectionAnnotation(Field field) throws IllegalArgumentSunstoneException {
        List<Annotation> injectionAnnotations = Arrays.stream(field.getAnnotations())
                .filter(ann -> AnnotationUtils.isAnnotatedBy(ann.annotationType(), CloudResourceIdentificationAnnotation.class))
                .collect(Collectors.toList());
        if (injectionAnnotations.size() > 1) {
            throw new IllegalArgumentSunstoneException(format("More than one annotation (in)direrectly annotated by %s found on %s %s in %s class",
                    CloudResourceIdentificationAnnotation.class, field.getType().getName(), field.getName(), field.getDeclaringClass()));
        }
        if (injectionAnnotations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(injectionAnnotations.get(0));
    }
    static List<Field> getAllFieldsList(final Class<?> cls, Predicate<Field> filter) {
        if (cls == null) {
            throw new IllegalArgumentException("The class must not be null.");
        }
        final List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            Arrays.stream(declaredFields).filter(filter).forEach(allFields::add);
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

    static void handleInjection(ExtensionContext ctx, Object instance, Field field) {
        SunstoneStore store = SunstoneStore.get(ctx);
        try {
            Optional<Annotation> injectionAnnotation = getAndCheckInjectionAnnotation(field);
            if (injectionAnnotation.isPresent()) {
                SunstoneResourceInjector injector = getSunstoneResourceInjector(field).orElseThrow(() -> new RuntimeException(format("Unable to load a service via SPI that can inject into %s %s in %s class", field.getType().getName(), field.getName(), field.getDeclaringClass().getName())));
                Object injectObject = injector.getResource(ctx);
                store.addClosable((AutoCloseable) () -> injector.closeResource(injectObject));
                field.setAccessible(true);
                field.set(instance, injectObject);
            }
        } catch (SunstoneException e) {
            throw new RuntimeException(format("Unable to inject %s %s in %s", field.getType().getName(), field.getName(), field.getDeclaringClass()), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static Optional<SunstoneArchiveDeployer> getArchiveDeployer(Method method) {
        for (SunstoneArchiveDeployerProvider sunstoneCloudDeployerProvider : sunstoneArchiveDeployerProviderLoader) {
            Optional<SunstoneArchiveDeployer> deployer = sunstoneCloudDeployerProvider.create(method);
            if (deployer.isPresent()) {
                return deployer;
            }
        }
        return Optional.empty();
    }
    static void performDeploymentOperation(ExtensionContext ctx) throws SunstoneException {
        List<Method> annotatedMethods = AnnotationSupport.findAnnotatedMethods(ctx.getRequiredTestClass(), Deployment.class, HierarchyTraversalMode.TOP_DOWN);
        SunstoneStore store = SunstoneStore.get(ctx);

        try {
            for (Method method : annotatedMethods) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentSunstoneException("Deployment method must be static");
                }
                if (method.getParameterCount() != 0) {
                    throw new IllegalArgumentSunstoneException("Deployment method must have 0 parameters");
                }
                Deployment annotation = method.getAnnotation(Deployment.class);
                String deploymentName = StringUtils.isBlank(annotation.name()) ? "" : SunstoneConfigResolver.resolveExpressionToString(annotation.name());

                method.setAccessible(true);
                Object invoke = method.invoke(null);
                if (invoke == null) {
                    throw new RuntimeException(format("%s in %s returned null", method.getName(), method.getDeclaringClass().getName()));
                }
                SunstoneArchiveDeployer archiveDeployer = getArchiveDeployer(method).orElseThrow(() -> new SunstoneException("todo"));
                archiveDeployer.deploy(deploymentName, invoke, ctx);
                store.addClosable((AutoCloseable) () -> archiveDeployer.undeploy(deploymentName, ctx));

            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static void injectInstanceResources(ExtensionContext ctx, Object instance) {
        for (Field field : getAllFieldsList(instance.getClass(), field -> !Modifier.isStatic(field.getModifiers()))) {
            handleInjection(ctx, instance, field);
        }
    }

    static void injectStaticResources(ExtensionContext ctx, Class<?> clazz) {
        for (Field field : getAllFieldsList(clazz, field -> Modifier.isStatic(field.getModifiers()))) {
            handleInjection(ctx, null, field);
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext ctx) throws Exception {
        injectInstanceResources(ctx, testInstance);
    }

    @Override
    public void afterAll(ExtensionContext ctx) throws Exception {
        Object ref = new Object() {
            Exception e = null;
        };
        Throwable thr = null;
        Iterator<Closeable> iterator = get(ctx).getClosablesOrCreate().iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().close();
            } catch (Throwable t) {
                if (thr == null) {
                    thr = t;
                } else {
                    t.printStackTrace();
                }
            }
        }
        if (thr != null) {
            throw new RuntimeException(thr);
        }
    }
}
