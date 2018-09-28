package com.github.guignol.swing.binding;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class BindableView<T> extends Bindable<T> implements Disposable {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    BindableView(Observable<T> source) {
        super(source);
    }

    public Disposable toViewModel(Runnable runnable) {
        final Disposable disposable = source.subscribe(t -> runnable.run());
        compositeDisposable.add(disposable);
        return disposable;
    }

    public Disposable toViewModel(Consumer<T> onNext) {
        final Disposable disposable = source.subscribe(onNext);
        compositeDisposable.add(disposable);
        return disposable;
    }

    @Override
    public void dispose() {
        compositeDisposable.dispose();
    }

    @Override
    public boolean isDisposed() {
        return compositeDisposable.isDisposed();
    }
}
