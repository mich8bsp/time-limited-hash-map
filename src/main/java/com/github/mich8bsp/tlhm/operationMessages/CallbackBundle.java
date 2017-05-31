package com.github.mich8bsp.tlhm.operationMessages;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Michael Bespalov on 31-May-17.
 */
public class CallbackBundle<K, V> {
    public List<Consumer<Map.Entry<K, V>>> getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(List<Consumer<Map.Entry<K, V>>> callbacks) {
        this.callbacks = callbacks;
    }

    private List<Consumer<Map.Entry<K, V>>> callbacks;

    public CallbackBundle(List<Consumer<Map.Entry<K, V>>> callbacks){
        this.callbacks = callbacks;
    }
}
