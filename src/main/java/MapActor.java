import akka.actor.AbstractActor;
import akka.actor.Props;
import operationMessages.GetMessage;
import operationMessages.KeyValuePair;
import operationMessages.PutMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by U43155 on 29/05/2017.
 */
public class MapActor<K, V> extends AbstractActor {

    private final long timeDelayMillis;
    private Map<K, TimestampedValue> underlyingMap = new HashMap<>();
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    public MapActor(long timeDelayMillis) {
        this.timeDelayMillis = timeDelayMillis;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PutMessage.class, message -> {
                    long removalTime = System.currentTimeMillis() + timeDelayMillis;
                    underlyingMap.put((K)message.getKey(), new TimestampedValue((V) message.getValue(), removalTime));
                    if(timeDelayMillis >0){
                        Runnable removalSender = ()->self().tell(new RemovalMessage((K) message.getKey(), removalTime), self());
                        executorService.schedule(removalSender, removalTime, TimeUnit.MILLISECONDS);
                    }
                })
                .match(GetMessage.class, message ->{
                    V value = Optional.ofNullable(underlyingMap.get(message.key))
                            .map(TimestampedValue::getValue)
                            .orElse(null);
                    KeyValuePair<K, V> storedEntry = new KeyValuePair<>((K) message.key, value);
                    getSender().tell(storedEntry, self());
                })
                .match(RemovalMessage.class, message -> {
                    TimestampedValue storedValue = underlyingMap.get(message.key);
                    if(storedValue.timestamp<=message.removalTime){
                        underlyingMap.remove(message.key);
                    }
                })
                .build();
    }

    private class RemovalMessage{
        private K key;
        private long removalTime;

        RemovalMessage(K key, long removalTime) {
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

        public V getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

}
