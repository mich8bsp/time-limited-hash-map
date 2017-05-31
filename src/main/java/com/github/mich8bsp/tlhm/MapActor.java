package com.github.mich8bsp.tlhm;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import com.github.mich8bsp.tlhm.operationMessages.*;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public class MapActor<K, V> extends AbstractActor {

    private final long timeDelayMillis;
    private Map<K, TimestampedValue> underlyingMap = new HashMap<>();
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    private List<Consumer<Map.Entry<K, V>>> callbacks = new LinkedList<>();

    public MapActor(long timeDelayMillis) {
        this.timeDelayMillis = timeDelayMillis;
    }

    private void sendTimeoutRemoval(K key, long removalTime){
        ActorRef mapActor = MapActorSystem.INSTANCE.getSystem().actorFor("/user/"+MapActorSystem.MAP_ACTOR_NAME);
        if(mapActor!=null && !mapActor.isTerminated()){
            mapActor.tell(new TimeRemovalMessage(key, removalTime), self());
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PutMessage.class, message -> {
                    long removalTime = System.currentTimeMillis() + timeDelayMillis;
                    underlyingMap.put((K)message.getKey(), new TimestampedValue((V) message.getValue(), removalTime));
                    if(timeDelayMillis >0){
                        Runnable removalSender = ()-> sendTimeoutRemoval((K)message.getKey(), removalTime);
                        executorService.schedule(removalSender, timeDelayMillis, TimeUnit.MILLISECONDS);
                    }
                })
                .match(GetMessage.class, message ->{
                    KeyValuePair<K, V> storedEntry = extractEntry((K) message.key, underlyingMap.get(message.key));
                    getSender().tell(storedEntry, self());
                })
                .match(TimeRemovalMessage.class, message -> {
                    K key = (K)message.key;
                    TimestampedValue storedValue = underlyingMap.get(key);
                    if(storedValue!=null && storedValue.timestamp<=message.removalTime){
                        underlyingMap.remove(key);
                        callbacks.forEach(callback -> callback.accept(new KeyValuePair<K, V>(key, storedValue.value)));
                    }
                })
                .match(SizeMessage.class, message -> sender().tell(underlyingMap.size(), self()))
                .match(RemoveMessage.class, message -> {
                    KeyValuePair<K, V> storedEntry = extractEntry((K)message.getKey(), underlyingMap.remove(message.getKey()));
                    sender().tell(storedEntry, self());
                })
                .match(ClearMessage.class, message -> underlyingMap.clear())
                .match(EntrySetMessage.class, message -> {
                    Set<Map.Entry<K, V>> entrySet = underlyingMap.entrySet()
                            .stream()
                            .map(entry -> extractEntry(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toSet());
                    sender().tell(entrySet, self());
                })
                .match(ContainsValueMessage.class, message -> {
                    Boolean containsValue = underlyingMap.values()
                            .stream()
                            .map(TimestampedValue::getValue)
                            .anyMatch(x->x.equals(message.getValue()));
                    sender().tell(containsValue, self());
                })
                .match(CallbackBundle.class, message -> callbacks.addAll(message.getCallbacks()))
                .build();
    }


    private KeyValuePair<K, V> extractEntry(K key, TimestampedValue mapValue){
        V value = Optional.ofNullable(mapValue)
                .map(TimestampedValue::getValue)
                .orElse(null);
        return new KeyValuePair<>(key, value);
    }

    private class TimeRemovalMessage {
        private K key;
        private long removalTime;

        TimeRemovalMessage(K key, long removalTime) {
            this.key = key;
            this.removalTime = removalTime;
        }
    }

    private class TimestampedValue {
        private V value;
        private long timestamp;

        TimestampedValue(V value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        V getValue() {
            return value;
        }

    }

}
