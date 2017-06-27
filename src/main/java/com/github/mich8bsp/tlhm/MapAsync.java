package com.github.mich8bsp.tlhm;

import scala.concurrent.Future;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by Michael Bespalov on 28-Jun-17.
 */
public interface MapAsync<K, V> {

    Future<Integer> sizeAsync();

    Future<Boolean> isEmptyAsync();

    Future<V> getAsync(Object key);

    Future<Boolean> containsKeyAsync(Object key);

    Future<Boolean> containsValueAsync(Object value);

    Future<V> removeAsync(Object key);

    Future<Set<K>> keySetAsync();

    Future<Collection<V>> valuesAsync();

    Future<Set<Map.Entry<K, V>>> entrySetAsync();
}
