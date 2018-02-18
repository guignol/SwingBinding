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
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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

            final boolean notifyWhenResized = events.contains(Event.RESIZED);
            final boolean notifyWhenMoved = events.contains(Event.MOVED);
            final boolean notifyWhenShown = events.contains(Event.SHOWN);
            final boolean notifyWhenHidden = events.contains(Event.HIDDEN);

            @Override
            public void componentResized(ComponentEvent e) {
                if (notifyWhenResized) {
                    eventHappened.onNext(Event.RESIZED);
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (notifyWhenMoved) {
                    eventHappened.onNext(Event.MOVED);
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                if (notifyWhenShown) {
                    eventHappened.onNext(Event.SHOWN);
                }
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                if (notifyWhenHidden) {
                    eventHappened.onNext(Event.HIDDEN);
                }
            }
        };
        return eventHappened
                .doOnSubscribe(disposable -> component.addComponentListener(eventListener))
                .doOnDispose(() -> component.removeComponentListener(eventListener))
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<MouseEvent> onClick(Component component) {
        final PublishSubject<MouseEvent> onClick = PublishSubject.create();
        final MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.onNext(e);
            }
        };
        return onClick
                .hide()
                .doOnSubscribe(disposable -> component.addMouseListener(listener))
                .doOnDispose(() -> component.removeMouseListener(listener))
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<ActionEvent> onAction(AbstractButton button) {
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

    public static Observable<Integer> onHovered(JList list) {
        final PublishSubject<Integer> hovered = PublishSubject.create();
        final MouseAdapter adapter = new MouseAdapter() {
            private boolean entered = false;

            @Override
            public void mouseEntered(MouseEvent e) {
                entered = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                entered = false;
                hovered.onNext(-1);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (entered) {
                    hovered.onNext(list.locationToIndex(e.getPoint()));
                } else {
                    hovered.onNext(-1);
                }
            }
        };
        return hovered
                .hide()
                .doOnSubscribe(disposable -> {
                    list.addMouseListener(adapter);
                    list.addMouseMotionListener(adapter);
                })
                .doOnDispose(() -> {
                    list.removeMouseListener(adapter);
                    list.removeMouseMotionListener(adapter);
                })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance())
                .distinctUntilChanged();
    }

    public static Observable<int[]> onSelection(JList list) {
        final BehaviorSubject<int[]> indices = BehaviorSubject.create();
        final ListSelectionListener selectionListener = e -> {
            final int[] selectedIndices = list.getSelectedIndices();
            // distinctUntilChangedだと繋ぎ直したときに流せないので自分で比較する
            if (!Arrays.equals(indices.getValue(), selectedIndices)) {
                indices.onNext(selectedIndices);
            }
        };
        return indices
                .hide()
                .doOnSubscribe(disposable -> {
                    list.addListSelectionListener(selectionListener);
                    indices.onNext(list.getSelectedIndices());
                })
                .doOnDispose(() -> list.removeListSelectionListener(selectionListener))
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    // TODO column
    public static Observable<int[]> onSelection(JTable table) {
        final BehaviorSubject<int[]> indices = BehaviorSubject.create();
        final ListSelectionListener selectionListener = e -> {
            final int[] selectedIndices = table.getSelectedRows();
            // distinctUntilChangedだと繋ぎ直したときに流せないので自分で比較する
            if (!Arrays.equals(indices.getValue(), selectedIndices)) {
                indices.onNext(selectedIndices);
            }
        };
        return indices
                .hide()
                .doOnSubscribe(disposable -> {
                    table.getSelectionModel().addListSelectionListener(selectionListener);
                    indices.onNext(table.getSelectedRows());
                })
                .doOnDispose(() -> table.getSelectionModel().removeListSelectionListener(selectionListener))
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
                // distinctUntilChangedだと繋ぎ直したときに流せないので自分で比較する
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
