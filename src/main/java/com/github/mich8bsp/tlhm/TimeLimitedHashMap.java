package com.github.mich8bsp.tlhm;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.github.mich8bsp.tlhm.operationMessages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.github.mich8bsp.tlhm.MapActorSystem.MAP_ACTOR_NAME;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public class TimeLimitedHashMap<K, V> implements ITimeLimitedHashMap<K, V> {

    private ActorRef mapActor;
    private static final long TIMEOUT = 5; //sec

    private TimeLimitedHashMap(long timeLimitMillis) {
        this.mapActor = MapActorSystem.INSTANCE.getSystem()
                .actorOf(Props.create(MapActor.class, timeLimitMillis), MAP_ACTOR_NAME);
    }

    public static <K, V> ITimeLimitedHashMap<K, V> create(long timeLimitMillis) {
        return new TimeLimitedHashMap<>(timeLimitMillis);
    }


    public void close() {
        if (mapActor != null && !mapActor.isTerminated()) {
            mapActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    @Override
    public void addRemovalCallbacks(List<Consumer<Entry<K, V>>> callbacks) {
        checkMapNotClosed();
        mapActor.tell(new CallbackBundle<>(callbacks), ActorRef.noSender());
    }

    private void checkMapNotClosed() throws MapClosedException {
        if (mapActor == null || mapActor.isTerminated()) {
            throw new MapClosedException();
        }
    }

    private Object askActor(Object message) {
        Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
        Future<Object> future = Patterns.ask(mapActor, message, timeout);
        try {
            return Await.result(future, timeout.duration());
        } catch (Exception e) {
            return null;
        }
    }

    public int size() {
        checkMapNotClosed();
        Integer size = (Integer) askActor(new SizeMessage());
        if (size == null) {
            throw new RuntimeException("Waiting for size operation timed out");
        }
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public boolean containsValue(Object value) {
        checkMapNotClosed();
        Boolean isContainsValue = (Boolean) askActor(new ContainsValueMessage<>(value));
        if (isContainsValue == null) {
            throw new RuntimeException("Waiting for containsValue operation timed out");
        }
        return isContainsValue;
    }

    public V get(Object key) {
        checkMapNotClosed();
        KeyValuePair<K, V> storedEntry = (KeyValuePair<K, V>) askActor(new GetMessage<>(key));
        return Optional.ofNullable(storedEntry)
                .map(KeyValuePair::getValue)
                .orElse(null);
    }

    public V put(K key, V value) {
        checkMapNotClosed();
        mapActor.tell(new PutMessage<>(key, value), ActorRef.noSender());
        return value;
    }

    public V remove(Object key) {
        checkMapNotClosed();
        KeyValuePair<K, V> removedEntry = (KeyValuePair<K, V>) askActor(new RemoveMessage<>(key));
        return Optional.ofNullable(removedEntry)
                .map(KeyValuePair::getValue)
                .orElse(null);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        checkMapNotClosed();
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        checkMapNotClosed();
        mapActor.tell(new ClearMessage(), ActorRef.noSender());
    }

    public Set<K> keySet() {
        checkMapNotClosed();
        return entrySet()
                .stream()
                .map(Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Collection<V> values() {
        checkMapNotClosed();
        return entrySet()
                .stream()
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    public Set<Entry<K, V>> entrySet() {
        checkMapNotClosed();
        return (Set<Entry<K, V>>) askActor(new EntrySetMessage());
    }
}
