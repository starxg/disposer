// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.starxg.disposer;

import org.jetbrains.annotations.*;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * <p>Manages a parent-child relation of chained objects requiring cleanup.</p>
 *
 * <p>A root node can be created via {@link #newDisposable()}, to which children are attached via subsequent calls to {@link #register(Disposable, Disposable)}.
 * Invoking {@link #dispose(Disposable)} will process all its registered children's {@link Disposable#dispose()} method.</p>
 * <p>
 * See <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/disposers.html">Disposer and Disposable</a> in SDK Docs.
 *
 * @see Disposable
 */
public final class Disposer {
    private static final ObjectTree ourTree = new ObjectTree();

    public static boolean isDebugDisposerOn() {
        return "on".equals(System.getProperty("idea.disposer.debug"));
    }

    private static boolean ourDebugMode;

    private Disposer() {
    }

    @NotNull
    @Contract(pure = true, value = "->new")
    public static Disposable newDisposable() {
        // must not be lambda because we care about identity in ObjectTree.myObject2NodeMap
        return newDisposable("");
    }

    @NotNull
    @Contract(pure = true, value = "_->new")
    public static Disposable newDisposable(@NotNull @NonNls String debugName) {
        // must not be lambda because we care about identity in ObjectTree.myObject2NodeMap
        return new Disposable() {
            @Override
            public void dispose() {
            }

            @Override
            public String toString() {
                return debugName;
            }
        };
    }

    @Contract(pure = true, value = "_,_->new")
    public static @NotNull Disposable newDisposable(@NotNull Disposable parentDisposable, @NotNull String debugName) {
        Disposable result = newDisposable(debugName);
        register(parentDisposable, result);
        return result;
    }

    private static final Map<String, Disposable> ourKeyDisposables = Collections.synchronizedMap(new WeakHashMap<>());


    public static void register(@NotNull Disposable parent, @NotNull Disposable child)  {
        RuntimeException e = ourTree.register(parent, child);
        if (e != null) throw e;
    }

    /**
     * Registers child disposable under parent unless the parent has already been disposed
     *
     * @return whether the registration succeeded
     */
    public static boolean tryRegister(@NotNull Disposable parent, @NotNull Disposable child) {
        return ourTree.register(parent, child) == null;
    }

    public static void register(@NotNull Disposable parent, @NotNull Disposable child, @NonNls @NotNull final String key) {
        register(parent, child);
        Disposable v = get(key);
        if (v != null) throw new IllegalArgumentException("Key " + key + " already registered: " + v);
        ourKeyDisposables.put(key, child);
        register(child, new KeyDisposable(key));
    }

    private static final class KeyDisposable implements Disposable {
        @NotNull
        private final String myKey;

        KeyDisposable(@NotNull String key) {
            myKey = key;
        }

        @Override
        public void dispose() {
            ourKeyDisposables.remove(myKey);
        }

        @Override
        public String toString() {
            return "KeyDisposable (" + myKey + ")";
        }
    }

    /**
     * @return true if {@code disposable} is disposed or being disposed (i.e. its {@link Disposable#dispose()} method is executing).
     */
    public static boolean isDisposed(@NotNull Disposable disposable) {
        return ourTree.getDisposalInfo(disposable) != null;
    }

    /**
     * @deprecated use {@link #isDisposed(Disposable)} instead
     */
    @Deprecated
    public static boolean isDisposing(@NotNull Disposable disposable) {
        return isDisposed(disposable);
    }

    public static Disposable get(@NotNull String key) {
        return ourKeyDisposables.get(key);
    }

    public static void dispose(@NotNull Disposable disposable) {
        dispose(disposable, true);
    }

    /**
     * {@code predicate} is used only for direct children.
     */
    @ApiStatus.Internal
    public static void disposeChildren(@NotNull Disposable disposable, @Nullable Predicate<? super Disposable> predicate) {
        ourTree.executeAllChildren(disposable, predicate);
    }

    public static void dispose(@NotNull Disposable disposable, boolean processUnregistered) {
        ourTree.executeAll(disposable, processUnregistered);
    }

    @NotNull
    public static ObjectTree getTree() {
        return ourTree;
    }

    public static void assertIsEmpty() {
        assertIsEmpty(false);
    }

    public static void assertIsEmpty(boolean throwError) {
        if (ourDebugMode) {
            ourTree.assertIsEmpty(throwError);
        }
    }

    /**
     * @return old value
     */
    public static boolean setDebugMode(boolean debugMode) {
        if (debugMode) {
            debugMode = !"off".equals(System.getProperty("idea.disposer.debug"));
        }
        boolean oldValue = ourDebugMode;
        ourDebugMode = debugMode;
        return oldValue;
    }

    public static boolean isDebugMode() {
        return ourDebugMode;
    }

    /**
     * @return object registered on {@code parentDisposable} which is equal to object, or {@code null} if not found
     */
    @Nullable
    public static <T extends Disposable> T findRegisteredObject(@NotNull Disposable parentDisposable, @NotNull T object) {
        return ourTree.findRegisteredObject(parentDisposable, object);
    }

    public static Throwable getDisposalTrace(@NotNull Disposable disposable) {
        if (getTree().getDisposalInfo(disposable) instanceof Throwable) {
            return (Throwable) disposable;
        }
        return null;
    }

    @ApiStatus.Internal
    public static void clearDisposalTraces() {
        ourTree.clearDisposedObjectTraces();
    }
}