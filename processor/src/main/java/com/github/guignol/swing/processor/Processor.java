package com.github.guignol.swing.processor;

import com.github.guignol.swing.binding.ComponentHolder;
import com.github.guignol.swing.binding.IView;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * http://www.ne.jp/asahi/hishidama/home/tech/java/annotation.html
 * https://stackoverflow.com/a/29923505
 * https://github.com/rejasupotaro/kvs-schema/tree/master/compiler/src/main/java/com/rejasupotaro/android/kvs/internal
 */
@AutoService(javax.annotation.processing.Processor.class)
@SupportedAnnotationTypes("com.github.guignol.swing.processor.*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Types typeUtils = processingEnv.getTypeUtils();
        final Messager messager = processingEnv.getMessager();
        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(View.class);
        for (Element element : elements) {
            final TypeElement clazz = (TypeElement) element;
            final TypeMirror iView = findInterface(clazz, IView.class);
            if (iView == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        clazz.getSimpleName() + " should implement " + View.class.getCanonicalName() + ".");
                return true;
            }

            List<? extends TypeMirror> typeParameters = getTypeParameters(iView);
            if (typeParameters == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        clazz.getSimpleName() + " should have type parameter.");
                return true;
            }
            final TypeMirror viewModel = typeParameters.get(0);
            final List<? extends TypeMirror> viewModelParent = typeUtils.directSupertypes(viewModel);
            typeParameters = getTypeParameters(viewModelParent.get(0));
            if (typeParameters == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "IViewModel's type parameter is not found.");
                return true;
            }

            final boolean viewIsComponent = findInterface(clazz, ComponentHolder.class) == null;
            final ViewFactoryWriter writer = new ViewFactoryWriter(
                    clazz.getAnnotation(View.class).factoryName(),
                    element.asType(),
                    viewModel,
                    typeParameters.get(0),
                    viewIsComponent ? "" : ".getComponent()");
            try {
                writer.write(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private static TypeMirror findInterface(TypeElement clazz, Class<?> target) {
        for (TypeMirror i : clazz.getInterfaces()) {
            final String className = i.toString().split("<")[0];
            if (target.getCanonicalName().equals(className)) {
                return i;
            }
        }
        return null;
    }

    private static List<? extends TypeMirror> getTypeParameters(TypeMirror type) {
        final DeclaredType declaredType = (DeclaredType) type;
        return declaredType.getTypeArguments();
    }
}
