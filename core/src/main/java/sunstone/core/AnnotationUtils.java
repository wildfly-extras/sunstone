package sunstone.core;


import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class AnnotationUtils {

    public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        return findAnnotation(clazz, annotationType, new HashSet<Annotation>());
    }

    public static <A extends Annotation> boolean isAnnotatedBy(Class<?> clazz, Class<A> annotationType) {
        return findAnnotation(clazz, annotationType, new HashSet<Annotation>()) != null;
    }

    public static <A extends Annotation> List<Annotation> findAnnotationsAnnotatedBy(Annotation[] anns, Class<A> annotationType) {
        List<Annotation> result = new ArrayList<>();
        for (Annotation ann : anns) {
            if (isAnnotatedBy(ann.annotationType(), annotationType)) {
                result.add(ann);
            }
        }
        return result;
    }

    private static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType, Set<Annotation> visited) {
        Annotation[] anns = clazz.getDeclaredAnnotations();
        for (Annotation ann : anns) {
            if (ann.annotationType() == annotationType) {
                return (A) ann;
            }
        }
        for (Annotation ann : anns) {
            if (!isInJavaLangAnnotationPackage(ann.getClass()) && visited.add(ann)) {
                A annotation = findAnnotation(ann.annotationType(), annotationType, visited);
                if (annotation != null) {
                    return annotation;
                }
            }
        }

        for (Class<?> ifc : clazz.getInterfaces()) {
            A annotation = findAnnotation(ifc, annotationType, visited);
            if (annotation != null) {
                return annotation;
            }
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || Object.class == superclass) {
            return null;
        }
        return findAnnotation(superclass, annotationType, visited);
    }
    private static boolean isInJavaLangAnnotationPackage(Class<? extends Annotation> annotationType) {
        return annotationType != null && annotationType.getName().startsWith("java.lang.annotation");
    }
}
