package com.github.guignol.swing.binding;

public class IViewModel<M> {

    protected final M model;

    public IViewModel(M model) {
        this.model = model;
    }
}
