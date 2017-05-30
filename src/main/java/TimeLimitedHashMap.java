import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.pattern.AskTimeoutException;
import akka.pattern.Patterns;
import akka.util.Timeout;
import operationMessages.GetMessage;
import operationMessages.KeyValuePair;
import operationMessages.PutMessage;
import operationMessages.SizeMessage;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by U43155 on 29/05/2017.
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
        throw new UnsupportedOperationException();
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
        return null;
    }

    public void putAll(Map<? extends K, ? extends V> m) {

    }

    public void clear() {

    }

    public Set<K> keySet() {
        return null;
    }

    public Collection<V> values() {
        return null;
    }

    public Set<Entry<K, V>> entrySet() {
        return null;
    }
}
