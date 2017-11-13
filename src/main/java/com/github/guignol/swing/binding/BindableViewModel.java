package com.github.guignol.swing.binding;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class BindableViewModel<T> extends Bindable<T> implements Disposable {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public BindableViewModel(Observable<T> source) {
        super(source);
    }

    public Disposable toView(Consumer<T> onNext) {
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
