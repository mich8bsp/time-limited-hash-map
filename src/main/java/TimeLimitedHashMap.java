import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.pattern.AskTimeoutException;
import akka.pattern.Patterns;
import akka.util.Timeout;
import operationMessages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public class TimeLimitedHashMap<K, V> implements IClosableMap<K, V> {

    private ActorRef mapActor;
    private static final long TIMEOUT = 5; //sec

    private TimeLimitedHashMap(long timeLimitMillis) {
        this.mapActor = ActorSystem.create("MapActorSystem").actorOf(Props.create(MapActor.class, timeLimitMillis));
    }

    public static <K, V> IClosableMap<K, V> create(long timeLimitMillis){
        return new TimeLimitedHashMap<>(timeLimitMillis);
    }


    public void close() {
        mapActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    private Object askActor(Object message){
        Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
        Future<Object> future = Patterns.ask(mapActor, message, timeout);
        try {
            return Await.result(future, timeout.duration());
        } catch (Exception e) {
            return null;
        }
    }

    public int size() {
        Integer size = (Integer) askActor(new SizeMessage());
        if(size==null){
            throw new RuntimeException("Waiting for size operation timed out");
        }
        return size;
    }

    public boolean isEmpty() {
        return size()==0;
    }

    public boolean containsKey(Object key) {
        return get(key)!=null;
    }

    public boolean containsValue(Object value) {
        Boolean isContainsValue = (Boolean) askActor(new ContainsValueMessage<>(value));
        if(isContainsValue==null){
            throw new RuntimeException("Waiting for containsValue operation timed out");
        }
        return isContainsValue;
    }

    public V get(Object key) {
        KeyValuePair<K, V> storedEntry = (KeyValuePair<K, V>) askActor(new GetMessage<>(key));
        return Optional.ofNullable(storedEntry)
                .map(KeyValuePair::getValue)
                .orElse(null);
    }

    public V put(K key, V value) {
        mapActor.tell(new PutMessage<>(key, value), ActorRef.noSender());
        return value;
    }

    public V remove(Object key) {
        KeyValuePair<K, V> removedEntry = (KeyValuePair<K, V>)askActor(new RemoveMessage<>(key));
        return Optional.ofNullable(removedEntry)
                .map(KeyValuePair::getValue)
                .orElse(null);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for(Entry<? extends K, ? extends V> entry : m.entrySet()){
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        mapActor.tell(new ClearMessage(), ActorRef.noSender());
    }

    public Set<K> keySet() {
        return entrySet()
                .stream()
                .map(Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Collection<V> values() {
        return entrySet()
                .stream()
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    public Set<Entry<K, V>> entrySet() {
        return (Set<Entry<K, V>>) askActor(new EntrySetMessage());
    }
}
