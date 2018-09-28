package com.github.guignol.swing.binding;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public class RightClick {

    public static Observable<MouseEvent> onPopupTriggered(JComponent component) {
        final PublishSubject<MouseEvent> subject = PublishSubject.create();
        final MouseListener listener = popupListener(e -> {
            subject.onNext(e);
            e.consume();
        });
        return subject.hide()
                .doOnSubscribe(disposable -> component.addMouseListener(listener))
                .doOnDispose(() -> component.removeMouseListener(listener));
    }

    private static MouseListener popupListener(Consumer<MouseEvent> consumer) {
        // 「ポップアップメニューを出すきっかけは、
        // 　Windowsではマウスの右ボタンを（押した後に）離した時だが、
        // 　実行される環境（OS）によっては、押した時に処理されることもあるらしい。」
        // http://www.ne.jp/asahi/hishidama/home/tech/java/swing/JPopupMenu.html
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mousePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mousePopup(e);
            }

            private void mousePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    consumer.accept(e);
                }
            }
        };
    }
}
