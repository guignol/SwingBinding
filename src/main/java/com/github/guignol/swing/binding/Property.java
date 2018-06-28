package com.github.guignol.swing.binding;

import com.github.guignol.swing.rx.SwingScheduler;
import io.reactivex.Observable;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
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

    public static Observable<Event> onEvent(Component component, @Nullable Event... filter) {
        final List<Event> events = Arrays.asList(filter == null ? Event.values() : filter);
        final boolean notifyWhenResized = events.contains(Event.RESIZED);
        final boolean notifyWhenMoved = events.contains(Event.MOVED);
        final boolean notifyWhenShown = events.contains(Event.SHOWN);
        final boolean notifyWhenHidden = events.contains(Event.HIDDEN);
        return Observable.<Event>create(emitter -> {
            final ComponentListener eventListener = new ComponentListener() {

                @Override
                public void componentResized(ComponentEvent e) {
                    if (notifyWhenResized) {
                        emitter.onNext(Event.RESIZED);
                    }
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    if (notifyWhenMoved) {
                        emitter.onNext(Event.MOVED);
                    }
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    if (notifyWhenShown) {
                        emitter.onNext(Event.SHOWN);
                    }
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                    if (notifyWhenHidden) {
                        emitter.onNext(Event.HIDDEN);
                    }
                }
            };
            component.addComponentListener(eventListener);
            emitter.setDisposable(Disposables.fromAction(() -> component.removeComponentListener(eventListener)));
        })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<MouseEvent> onClick(Component component) {
        return Observable.<MouseEvent>create(emitter -> {
            final MouseAdapter listener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    emitter.onNext(event);
                }
            };
            component.addMouseListener(listener);
            emitter.setDisposable(Disposables.fromAction(() -> component.removeMouseListener(listener)));
        })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<ActionEvent> onAction(AbstractButton button) {
        return Observable.<ActionEvent>create(emitter -> {
            final ActionListener actionListener = emitter::onNext;
            button.addActionListener(actionListener);
            emitter.setDisposable(Disposables.fromAction(() -> button.removeActionListener(actionListener)));
        })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<String> onInput(JTextComponent textComponent) {
        final Observable<String> onDocumentUpdated = onDocumentUpdated(textComponent.getDocument())
                .map(documentEvent -> textComponent.getText());
        final PublishSubject<String> initialValue = PublishSubject.create();
        return Observable.merge(onDocumentUpdated.hide(), initialValue)
                // TODO 繋ぐ度に最新の値を取得したいけど、これで正しい？
                .doOnSubscribe(disposable -> initialValue.onNext(textComponent.getText()))
                .subscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<DocumentEvent> onDocumentUpdated(Document document) {
        return Observable.<DocumentEvent>create(emitter -> {
            final DocumentListener listener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    emitter.onNext(event);
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    emitter.onNext(event);
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    emitter.onNext(event);
                }
            };
            document.addDocumentListener(listener);
            emitter.setDisposable(Disposables.fromAction(() -> document.removeDocumentListener(listener)));
        })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance());
    }

    public static Observable<Integer> onHovered(JList list) {
        return Observable.<Integer>create(emitter -> {
            final MouseAdapter adapter = new MouseAdapter() {
                private boolean entered = false;

                @Override
                public void mouseEntered(MouseEvent e) {
                    entered = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    entered = false;
                    emitter.onNext(-1);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (entered) {
                        emitter.onNext(list.locationToIndex(e.getPoint()));
                    } else {
                        emitter.onNext(-1);
                    }
                }
            };
            list.addMouseListener(adapter);
            list.addMouseMotionListener(adapter);
            emitter.setDisposable(Disposables.fromAction(() -> {
                list.removeMouseListener(adapter);
                list.removeMouseMotionListener(adapter);
            }));
        })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance())
                .distinctUntilChanged();
    }

    public static Observable<int[]> onSelection(JList list) {
        return Observable.<int[]>create(emitter -> {
            final ListSelectionListener selectionListener = e -> emitter.onNext(list.getSelectedIndices());
            list.addListSelectionListener(selectionListener);
            // 初期値
            emitter.onNext(list.getSelectedIndices());
            emitter.setDisposable(Disposables.fromAction(() -> list.removeListSelectionListener(selectionListener)));
        })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance())
                .distinctUntilChanged();
    }

    // TODO column
    public static Observable<int[]> onSelection(JTable table) {
        return Observable.<int[]>create(emitter -> {
            final ListSelectionListener selectionListener = e -> emitter.onNext(table.getSelectedRows());
            final ListSelectionModel selectionModel = table.getSelectionModel();
            selectionModel.addListSelectionListener(selectionListener);
            // 初期値
            emitter.onNext(table.getSelectedRows());
            emitter.setDisposable(Disposables.fromAction(() -> selectionModel.removeListSelectionListener(selectionListener)));
        })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance())
                .distinctUntilChanged();
    }

    public static Observable<ChangeEvent> onChanged(JViewport viewport) {
        return Observable.<ChangeEvent>create(emitter -> {
            final ChangeListener changeListener = emitter::onNext;
            viewport.addChangeListener(changeListener);
            emitter.setDisposable(Disposables.fromAction(() -> viewport.removeChangeListener(changeListener)));
        })
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
        return Observable.<T>create(emitter -> {
            final ChangeListener changeListener = e -> {
                final T property;
                try {
                    property = getter.apply(button);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    emitter.onError(exception);
                    return;
                }
                emitter.onNext(property);
            };
            button.addChangeListener(changeListener);
            // 初期値
            final T property = getter.apply(button);
            emitter.onNext(property);
            emitter.setDisposable(Disposables.fromAction(() -> button.removeChangeListener(changeListener)));
        })
                .subscribeOn(SwingScheduler.getInstance())
                .unsubscribeOn(SwingScheduler.getInstance())
                .distinctUntilChanged(comparer);
    }
}
