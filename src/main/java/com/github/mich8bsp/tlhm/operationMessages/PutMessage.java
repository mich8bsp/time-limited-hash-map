package com.github.mich8bsp.tlhm.operationMessages;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public class PutMessage<K, V> extends KeyValuePair<K, V>{
    public PutMessage(K key, V value) {
        super(key, value);
    }
}
