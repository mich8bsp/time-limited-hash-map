package com.github.mich8bsp.tlhm;

import java.util.Map;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public interface IClosableMap<K, V> extends Map<K, V> {
    void close();
}
