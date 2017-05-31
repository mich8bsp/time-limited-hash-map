package com.github.mich8bsp.tlhm;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public interface ITimeLimitedHashMap<K, V> extends Map<K, V> {
    void close();

    void addRemovalCallbacks(List<Consumer<Entry<K, V>>> callbacks);
}
