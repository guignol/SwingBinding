package com.github.guignol.swing.binding;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.Consumer;

public class BindableView<T> extends Bindable<T> {

    BindableView(Observable<T> source) {
        super(source);
    }

    public void toViewModel(Consumer<T> onNext) {
        source.subscribe(onNext);
    }

    public void toViewModel(Consumer<T> onNext, Consumer<? super Throwable> onError) {
        source.subscribe(onNext, onError);
    }

    public void toViewModel(Observer<T> observer) {
        source.subscribe(observer);
    }
}
