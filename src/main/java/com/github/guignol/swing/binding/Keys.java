package com.github.guignol.swing.binding;

import com.github.guignol.swing.rx.EventStatus;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Keys {

    public static class Registry {
        private final JComponent component;

        public Registry(JComponent component) {
            this.component = component;
        }

        public void registerAction(String name,
                                   KeyStroke keyStroke,
                                   Action action) {
            component.getInputMap().put(keyStroke, name);
            component.getActionMap().put(name, action);
        }

        public Observable<EventStatus> onFired(KeyHolder keyHolder) {
            return onFired(keyHolder.name, keyHolder.keyStroke);
        }

        public Observable<EventStatus> onFired(String name, KeyStroke keyStroke) {
            final EventStatus.Publisher subject = EventStatus.create();
            registerAction(name, keyStroke, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    subject.onNext();
                }
            });
            return subject.asObservable();
        }

        public Observable<MouseEvent> onRightClick() {
            return RightClick.onPopupTriggered(component);
        }
    }

    public static class KeyHolder {
        @NonNull
        public final String name;
        @NonNull
        public final KeyStroke keyStroke;

        public KeyHolder(@NonNull String name, @NonNull KeyStroke keyStroke) {
            this.name = name;
            this.keyStroke = keyStroke;
        }
    }

    // ctrl + c または command + c でcopy
    public static KeyHolder COPY = new KeyHolder("copy",
            KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
    );

    public static KeyHolder SELECT_ALL = new KeyHolder("selectAll",
            KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
    );
}
