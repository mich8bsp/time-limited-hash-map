package com.github.mich8bsp.tlhm;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.github.mich8bsp.tlhm.operationMessages.*;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.github.mich8bsp.tlhm.MapActorSystem.MAP_ACTOR_NAME;
import static com.github.mich8bsp.tlhm.Utils.awaitFuture;
import static com.github.mich8bsp.tlhm.Utils.getMapper;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public class TimeLimitedHashMap<K, V> implements ITimeLimitedHashMap<K, V> {

    private ActorRef mapActor;
    private static final long TIMEOUT = 5; //sec
    private Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
    private ExecutionContextExecutor dispatcher = MapActorSystem.INSTANCE.getSystem().dispatcher();

    private TimeLimitedHashMap(long timeLimitMillis) {
        this.mapActor = MapActorSystem.INSTANCE.getSystem()
                .actorOf(Props.create(MapActor.class, timeLimitMillis), MAP_ACTOR_NAME);
    }

    public static <K, V> ITimeLimitedHashMap<K, V> create(long timeLimitMillis) {
        return new TimeLimitedHashMap<>(timeLimitMillis);
    }

    public ITimeLimitedHashMap<K, V> setOperationTimeout(long timeoutSec){
        this.timeout = new Timeout(Duration.create(timeoutSec, "seconds"));
        return this;
    }

    public void close() {
        if (mapActor != null && !mapActor.isTerminated()) {
            mapActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    @Override
    public ITimeLimitedHashMap<K, V> addRemovalCallbacks(List<Consumer<Entry<K, V>>> callbacks) {
        checkMapNotClosed();
        mapActor.tell(new CallbackBundle<>(callbacks), ActorRef.noSender());
        return this;
    }

    private void checkMapNotClosed() throws MapClosedException {
        if (mapActor == null || mapActor.isTerminated()) {
            throw new MapClosedException();
        }
    }

    private Future<Object> askActorAsync(Object message){
        return Patterns.ask(mapActor, message, timeout);
    }

    public int size() {
        Integer size = awaitFuture(sizeAsync(), timeout);
        if (size == null) {
            throw new RuntimeException("Waiting for size operation timed out");
        }
        return size;
    }

    public Future<Integer> sizeAsync(){
        checkMapNotClosed();
        return askActorAsync(new SizeMessage())
                .map(getMapper(size -> (Integer)size), dispatcher);
    }

    public boolean isEmpty() {
        Boolean isEmptyRes = awaitFuture(isEmptyAsync(), timeout);
        if(isEmptyRes == null){
            throw new RuntimeException("Waiting for isEmpty operation timed out");
        }
        return isEmptyRes;
    }

    public Future<Boolean> isEmptyAsync(){
        return sizeAsync()
                .map(getMapper(size -> size == 0), dispatcher);
    }

    public V get(Object key) {
        return awaitFuture(getAsync(key), timeout);
    }

    public Future<V> getAsync(Object key){
        checkMapNotClosed();
        return askActorAsync(new GetMessage<>(key))
                .map(getMapper(pair -> Optional.ofNullable((KeyValuePair<K, V>)pair)), dispatcher)
                .map(getMapper(pair -> pair.map(KeyValuePair::getValue).orElse(null)), dispatcher);
    }

    public boolean containsKey(Object key) {
        Boolean doesContainKey = awaitFuture(containsKeyAsync(key), timeout);
        if(doesContainKey==null){
            throw new RuntimeException("Waiting for containsKey operation timed out");
        }
        return doesContainKey;
    }

    public Future<Boolean> containsKeyAsync(Object key){
        return getAsync(key)
                .map(getMapper(Objects::nonNull), dispatcher);
    }


    public boolean containsValue(Object value) {
        Boolean doesContainValue = awaitFuture(containsValueAsync(value), timeout);
        if (doesContainValue == null) {
            throw new RuntimeException("Waiting for containsValue operation timed out");
        }
        return doesContainValue;
    }

    public Future<Boolean> containsValueAsync(Object value){
        checkMapNotClosed();
        return askActorAsync(new ContainsValueMessage<>(value))
                .map(getMapper(res -> (Boolean)res), dispatcher);
    }

    public V put(K key, V value) {
        checkMapNotClosed();
        mapActor.tell(new PutMessage<>(key, value), ActorRef.noSender());
        return value;
    }

    public V remove(Object key) {
        return awaitFuture(removeAsync(key), timeout);
    }

    public Future<V> removeAsync(Object key){
        checkMapNotClosed();
        return askActorAsync(new RemoveMessage<>(key))
                .map(getMapper(pair -> Optional.ofNullable((KeyValuePair<K, V>)pair)), dispatcher)
                .map(getMapper(pair -> pair.map(KeyValuePair::getValue).orElse(null)), dispatcher);
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
        Set<K> keySet = awaitFuture(keySetAsync(), timeout);
        if(keySet==null){
            throw new RuntimeException("Waiting for keySet operation timed out");
        }
        return keySet;
    }

    public Future<Set<K>> keySetAsync(){
        return entrySetAsync()
                .map(getMapper(entrySet -> entrySet
                        .stream()
                        .map(Entry::getKey)
                        .collect(Collectors.toSet())), dispatcher);
    }

    public Collection<V> values() {
        Collection<V> values = awaitFuture(valuesAsync(), timeout);
        if (values == null){
            throw new RuntimeException("Waiting for values operation timed out");
        }
        return values;
    }

    public Future<Collection<V>> valuesAsync(){
        return entrySetAsync()
                .map(getMapper(entrySet -> entrySet
                            .stream()
                            .map(Entry::getValue)
                            .collect(Collectors.toList())), dispatcher);
    }

    public Set<Entry<K, V>> entrySet() {
       Set<Entry<K, V>> entrySet = awaitFuture(entrySetAsync(), timeout);
        if (entrySet == null) {
            throw new RuntimeException("Waiting for entrySet operation timed out");
        }
        return entrySet;
    }

    public Future<Set<Entry<K, V>>> entrySetAsync(){
        checkMapNotClosed();
        return askActorAsync(new EntrySetMessage())
                .map(getMapper(entrySet -> (Set<Entry<K, V>>) entrySet), dispatcher);
    }
}
