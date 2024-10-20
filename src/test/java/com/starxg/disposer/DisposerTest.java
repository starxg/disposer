package com.starxg.disposer;


import org.junit.jupiter.api.Test;

class DisposerTest {
    @Test
    public void test() {
        final Disposable root = Disposer.newDisposable();
        Disposer.register(root, new Disposable() {
            @Override
            public void dispose() {
                System.out.println("Dispose 1");
            }
        });
        Disposer.register(root, new Disposable() {
            @Override
            public void dispose() {
                System.out.println("Dispose 2");
            }
        });
        Disposer.register(root, new Disposable() {
            @Override
            public void dispose() {
                System.out.println("Dispose 3");
            }
        });

        Disposer.dispose(root);


    }
}