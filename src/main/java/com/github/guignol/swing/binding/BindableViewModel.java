package com.github.guignol.swing.binding;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.Consumer;

public class BindableViewModel<T> extends Bindable<T> {

    public BindableViewModel(Observable<T> source) {
        super(source);
    }

    public void toView(Consumer<T> onNext) {
        source.subscribe(onNext);
    }

    public void toView(Consumer<T> onNext, Consumer<? super Throwable> onError) {
        source.subscribe(onNext, onError);
    }

    public void toView(Observer<T> observer) {
        source.subscribe(observer);
    }
}
