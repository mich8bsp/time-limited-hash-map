package com.github.mich8bsp.tlhm.operationMessages;

/**
 * Created by Michael Bespalov on 31-May-17.
 */
public class ContainsValueMessage<V> {
    private V value;

    public ContainsValueMessage(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
