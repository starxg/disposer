package com.starxg.disposer;

public interface Disposable {

    default void dispose() {

    }

    interface Parent extends Disposable {
        void beforeTreeDispose();
    }

}
