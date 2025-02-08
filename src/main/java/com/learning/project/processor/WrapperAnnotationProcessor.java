package com.learning.project.processor;

import com.google.auto.service.AutoService;
import com.learning.project.annotation.GenerateWrapper;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.learning.project.annotation.GenerateWrapper")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
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
        String className = typeElement.getSimpleName().toString();
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
}