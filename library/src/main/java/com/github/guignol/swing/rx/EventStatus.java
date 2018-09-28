package com.github.guignol.swing.rx;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

// Observableにnullを流せないので、代わりのイベント通知
public enum EventStatus {
    NEXT;

    public static Publisher create() {
        return new Publisher();
    }

    public static class Publisher {
        private final PublishSubject<EventStatus> subject = PublishSubject.create();

        private Publisher() {
        }

        public void onNext() {
            subject.onNext(EventStatus.NEXT);
        }

        public Observable<EventStatus> asObservable() {
            return subject.hide();
        }
    }
}
