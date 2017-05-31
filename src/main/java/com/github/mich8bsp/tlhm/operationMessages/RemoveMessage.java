package com.github.mich8bsp.tlhm.operationMessages;

/**
 * Created by Michael Bespalov on 31-May-17.
 */
public class RemoveMessage<K> {
    private K key;

    public RemoveMessage(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }
}
