package com.learning.project.processor;

import com.google.auto.service.AutoService;
import com.learning.project.annotation.GenerateWrapper;

import javax.annotation.WillClose;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.learning.project.annotation.GenerateWrapper")
public class WrapperAnnotationProcessor extends AbstractProcessor {

    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateWrapper.class)) {
            if (element instanceof TypeElement typeElement) {
                generateWrapperClass(typeElement);
            }
        }
        return true;
    }

    private void generateWrapperClass(TypeElement typeElement) {
        TypeElement wrapperClass = getWrapperClass(typeElement);
        String className = typeElement.getSimpleName().toString();

        Class<?> rootClassWrapper = null;

        String wrapperClassName = className + "Wrapper";
        String packageName = elementUtils.getPackageOf(typeElement).toString();

        String source = """
                package %s;
                
                public class %s {
                    private final %s instance;

                    public %s(%s instance) {
                        this.instance = instance;
                    }

                    public %s getInstance() {
                        return instance;
                    }
                }
                """.formatted(packageName, wrapperClassName, className, wrapperClassName, className, className);

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + wrapperClassName);
            try (PrintWriter writer = new PrintWriter(file.openWriter())) {
                writer.write(source);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TypeElement getWrapperClass(TypeElement typeElement) {
        for (AnnotationMirror annotationMirror : typeElement.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(GenerateWrapper.class.getName())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                        annotationMirror.getElementValues().entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    AnnotationValue value = entry.getValue();
                    if (key.equals("value")) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Class in annotation: " + value);
                        TypeMirror typeMirror = (TypeMirror) value.getValue();
                        return (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
                    }
                }
            }
        }
        return null; // Return null if no match is found
    }
}