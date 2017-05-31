package com.github.mich8bsp.tlhm.operationMessages;

import java.util.Map;

/**
 * Created by Michael Bespalov on 30-May-17.
 */
public class KeyValuePair<K, V> implements Map.Entry<K, V>{
    private K key;
    private V value;

    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        this.value = value;
        return value;
    }

}
