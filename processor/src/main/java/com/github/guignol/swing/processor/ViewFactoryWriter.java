package com.github.guignol.swing.processor;

import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;

class ViewFactoryWriter {
    private final String factoryName;
    private final TypeMirror view;
    private final TypeMirror viewModel;
    private final TypeMirror model;
    private final String getComponent;

    ViewFactoryWriter(String factoryName,
                      TypeMirror view,
                      TypeMirror viewModel,
                      TypeMirror model,
                      String getComponent) {
        if (factoryName.isEmpty()) {
            this.factoryName = getSimpleName(view) + "Factory";
        } else {
            this.factoryName = factoryName;
        }
        this.view = view;
        this.viewModel = viewModel;
        this.model = model;
        this.getComponent = getComponent;
    }

    void write(Filer filer) throws IOException {
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(factoryName)
                .addJavadoc("Automatically generated file. DO NOT MODIFY\n")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        final String parameterName = "model";
        final MethodSpec method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(java.awt.Component.class)
                .addParameter(ParameterSpec.builder(TypeName.get(this.model), parameterName).build())
                .addComment("ViewModel to Model")
                .addStatement("final $T viewModel = new $T($L)", viewModel, viewModel, parameterName)
                .addComment("View to ViewModel")
                .addStatement("final $T view = new $T()", view, view)
                .addStatement("view.bind(viewModel)")
                .addStatement("return view" + getComponent)
                .build();
        typeSpecBuilder.addMethod(method);

        JavaFile.builder(getPackageName(this.model), typeSpecBuilder.build())
                .build()
                .writeTo(filer);
    }

    private static String getPackageName(TypeMirror type) {
        // TODO
        final String fullyQualified = type.toString();
        final int lastDot = fullyQualified.lastIndexOf(".");
        return fullyQualified.substring(0, lastDot);
    }

    private static String getSimpleName(TypeMirror type) {
        DeclaredType declaredType = (DeclaredType) type;
        return declaredType.asElement().getSimpleName().toString();
    }
}
