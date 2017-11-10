package com.github.guignol.swing.binding;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Predicate;

import java.util.Arrays;

public class Notification<K, V> {
    public final K key;
    public final V value;

    private Notification(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public interface FromValue<E extends Enum<E>, V> {

        default Notification<E, V> notify(V value) {
            return new Notification<>(toEnum(), value);
        }

        @SuppressWarnings("unchecked")
        default E toEnum() {
            return (E) this;
        }
    }

    public static <V> Notification<String, V> from(String key, V value) {
        return new Notification<>(key, value);
    }

    @SafeVarargs
    private static <K> Predicate<Notification> getFilter(@Nullable K... keys) {
        if (keys == null || keys.length == 0) {
            return it -> true;
        }
        return notification -> Arrays.stream(keys).anyMatch(k -> k == notification.key);
    }

    @SafeVarargs
    public static <K extends Enum<K> & Notification.FromValue<K, V>, V> BindableViewModel<V> getBinder(
            @NonNull Observable<Notification<K, V>> source,
            @Nullable K... filter) {
        return new BindableViewModel<>(source.filter(getFilter(filter)).map(notification -> notification.value));
    }
}
