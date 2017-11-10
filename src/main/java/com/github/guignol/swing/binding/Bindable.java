package com.github.guignol.swing.binding;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;

public class Bindable<T> {

    final Observable<T> source;

    Bindable(Observable<T> source) {
        this.source = source;
    }

    public static <T> BindableView<T> view(Observable<T> source) {
        return new BindableView<>(source);
    }

    public static <T> BindableView<T> view(AbstractButton button,
                                           Function<AbstractButton, T> getter) {
        return new BindableView<>(Property.onChanged(button, getter));
    }

    public static BindableView<ActionEvent> view(AbstractButton button) {
        return new BindableView<>(Property.onAction(button));
    }

    public static BindableView<String> view(JTextComponent textComponent) {
        return new BindableView<>(Property.onInput(textComponent));
    }

    public static BindableView<int[]> view(JList list) {
        return new BindableView<>(Property.onSelection(list));
    }
}
