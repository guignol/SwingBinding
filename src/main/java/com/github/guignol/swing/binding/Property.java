package com.github.guignol.swing.binding;

import com.github.guignol.swing.rx.SwingScheduler;
import io.reactivex.Observable;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Property {

    public enum Event {
        MOVED,
        RESIZED,
        SHOWN,
        HIDDEN
    }

    public static Observable<Event> onEvent(Component component, Event... filter) {
        final List<Event> events = Arrays.asList(filter == null ? Event.values() : filter);
        final PublishSubject<Event> eventHappened = PublishSubject.create();
        final ComponentListener eventListener = new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                eventHappened.onNext(Event.RESIZED);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                eventHappened.onNext(Event.MOVED);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                eventHappened.onNext(Event.SHOWN);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                eventHappened.onNext(Event.HIDDEN);
            }
        };
        return eventHappened
                .doOnSubscribe(disposable -> component.addComponentListener(eventListener))
                .doOnDispose(() -> component.removeComponentListener(eventListener))
                .filter(events::contains);
    }

    public static Observable<ActionEvent> onEvent(AbstractButton button) {
        final PublishSubject<ActionEvent> onAction = PublishSubject.create();
        final ActionListener actionListener = onAction::onNext;
        return onAction
                .hide()
                .doOnSubscribe(disposable -> button.addActionListener(actionListener))
                .doOnDispose(() -> button.removeActionListener(actionListener))
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<String> onInput(JTextComponent textComponent) {
        final Observable<String> onDocumentUpdated = onDocumentUpdated(textComponent.getDocument())
                .map(documentEvent -> textComponent.getText());
        final PublishSubject<String> initialValue = PublishSubject.create();
        return Observable.merge(onDocumentUpdated.hide(), initialValue)
                .doOnSubscribe(disposable -> initialValue.onNext(textComponent.getText()))
                .subscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<DocumentEvent> onDocumentUpdated(Document document) {
        final BehaviorSubject<DocumentEvent> updated = BehaviorSubject.create();
        final DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updated.onNext(event);
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updated.onNext(event);
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updated.onNext(event);
            }
        };
        return updated
                .hide()
                .doOnSubscribe(disposable -> document.addDocumentListener(listener))
                .doOnDispose(() -> document.removeDocumentListener(listener))
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    public static <T> Observable<T> onChanged(AbstractButton button,
                                              Function<AbstractButton, T> getter) {
        return onChanged(button, getter, Objects::equals);
    }

    public static <T> Observable<T> onChanged(AbstractButton button,
                                              Function<AbstractButton, T> getter,
                                              BiPredicate<? super T, ? super T> comparer) {
        final BehaviorSubject<T> propertyChanged = BehaviorSubject.create();
        final ChangeListener changeListener = e -> {
            final T property;
            try {
                property = getter.apply(button);
            } catch (Exception exception) {
                exception.printStackTrace();
                return;
            }
            try {
                // distinctUntilChangedだと繋ぎ直したときに弾いてしまうので自分で比較する
                if (!comparer.test(propertyChanged.getValue(), property)) {
                    propertyChanged.onNext(property);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                // いちおう流す
                propertyChanged.onNext(property);
            }
        };
        return propertyChanged
                .hide()
                .doOnSubscribe(disposable -> {
                    button.addChangeListener(changeListener);
                    final T property = getter.apply(button);
                    propertyChanged.onNext(property);
                })
                .doOnDispose(() -> button.removeChangeListener(changeListener))
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }
}
