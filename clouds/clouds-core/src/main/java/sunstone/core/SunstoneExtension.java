package sunstone.core;


import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import sunstone.api.SunstoneInjectionAnnotation;
import sunstone.api.WithAwsCfTemplate;
import sunstone.api.WithAzureArmTemplate;
import sunstone.core.api.SunstoneCloudDeployer;
import sunstone.core.api.SunstoneResourceInjector;
import sunstone.core.exceptions.IllegalArgumentSunstoneException;
import sunstone.core.exceptions.SunstoneException;
import sunstone.core.spi.SunstoneCloudDeployerProvider;
import sunstone.core.spi.SunstoneResourceInjectorProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static sunstone.core.SunstoneStore.get;

/**
 * Extension handles {@link WithAzureArmTemplate} annotation, serves and orchestrate/delegate all the work,
 * initialize resources, ...
 * <p>
 * Uses {@link AutoCloseable} LIFO queue (a.k.a. Stack) for registering resources that needs to be cleaned/closed -
 * closing clients, cleaning Cloud resources ... Class that creates
 * a resource that needs to be cleaned/closed is also responsible for registering it.
 */
public class SunstoneExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor {

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        if (ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class).length > 0) {
            handleAzureArmTemplateAnnotations(ctx);
        }

        if (ctx.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class).length > 0) {
            handleAwsCloudFormationAnnotations(ctx);
        }
        injectStaticResources(ctx, ctx.getRequiredTestClass());
    }

    static void handleAwsCloudFormationAnnotations(ExtensionContext ctx) {
        Optional<SunstoneCloudDeployer> deployer = getDeployer(WithAwsCfTemplate.class);
        deployer.orElseThrow(() -> new RuntimeException("Unable to load a service via SPI that handles " + WithAwsCfTemplate.class.getName() + " annotation."));
        for (WithAwsCfTemplate withAwsCfTemplate : ctx.getRequiredTestClass().getAnnotationsByType(WithAwsCfTemplate.class)) {
            deployer.get().deploy(withAwsCfTemplate, ctx);
        }
    }

    protected static void handleAzureArmTemplateAnnotations(ExtensionContext ctx) {
        Optional<SunstoneCloudDeployer> deployer = getDeployer(WithAzureArmTemplate.class);
        deployer.orElseThrow(() -> new RuntimeException("Unable to load a service via SPI that handles " + WithAzureArmTemplate.class.getName() + " annotation."));
        for (WithAzureArmTemplate withAzureArmTemplate : ctx.getRequiredTestClass().getAnnotationsByType(WithAzureArmTemplate.class)) {
            deployer.get().deploy(withAzureArmTemplate, ctx);
        }
    }

    static <A extends Annotation> Optional<SunstoneCloudDeployer> getDeployer(Class<A> annotation) {
        ServiceLoader<SunstoneCloudDeployerProvider> loader = ServiceLoader.load(SunstoneCloudDeployerProvider.class);
        for (SunstoneCloudDeployerProvider sunstoneCloudDeployerProvider : loader) {
            Optional<SunstoneCloudDeployer> deployer = sunstoneCloudDeployerProvider.create(annotation);
            if (deployer.isPresent()) {
                return deployer;
            }
        }
        return Optional.empty();
    }

    static Optional<SunstoneResourceInjector> getSunstoneResourceInjector(Field field) {
        ServiceLoader<SunstoneResourceInjectorProvider> loader = ServiceLoader.load(SunstoneResourceInjectorProvider.class);
        for (SunstoneResourceInjectorProvider provider : loader) {
            Optional<SunstoneResourceInjector> injector = provider.create(field);
            if (injector.isPresent()) {
                return injector;
            }
        }
        return Optional.empty();
    }

    static Optional<Annotation> getAndCheckInjectionAnnotation(Field field) throws IllegalArgumentSunstoneException {
        List<Annotation> injectionAnnotations = Arrays.stream(field.getAnnotations())
                .filter(ann -> AnnotationUtils.isAnnotatedBy(ann.annotationType(), SunstoneInjectionAnnotation.class))
                .collect(Collectors.toList());
        if (injectionAnnotations.size() > 1) {
            throw new IllegalArgumentSunstoneException(format("More than one annotation (in)direrectly annotated by %s found on %s %s in %s class",
                    SunstoneInjectionAnnotation.class, field.getType().getName(), field.getName(), field.getDeclaringClass()));
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
        try {
            Optional<Annotation> injectionAnnotation = getAndCheckInjectionAnnotation(field);
            if (injectionAnnotation.isPresent()) {
                Optional<SunstoneResourceInjector> injector = getSunstoneResourceInjector(field);
                injector.orElseThrow(() -> new RuntimeException(format("Unable to load a service via SPI that can inject into %s %s in %s class", field.getType().getName(), field.getName(), field.getDeclaringClass().getName())));
                Object injectObject = injector.get().getAndRegisterResource(injectionAnnotation.get(), field.getType(), ctx);
                field.setAccessible(true);
                field.set(instance, injectObject);
            }
        } catch (SunstoneException e) {
            throw new RuntimeException(format("Unable to injec %s %s in %s", field.getType().getName(), field.getName(), field.getDeclaringClass()), e);
        } catch (IllegalAccessException e) {
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
        var ref = new Object() {
            Exception e = null;
        };
        get(ctx).getClosablesOrCreate().forEach(closeable -> {
            try {
                closeable.close();
            } catch (Exception e) {
                if (ref.e == null) {
                    ref.e = e;
                }
            }
        });
        if (ref.e != null) {
            throw ref.e;
        }
    }
}
