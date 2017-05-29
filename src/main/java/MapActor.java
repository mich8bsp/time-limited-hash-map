import akka.actor.AbstractActor;
import akka.actor.Props;
import operationMessages.GetMessage;
import operationMessages.PutMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by U43155 on 29/05/2017.
 */
public class MapActor<K, V> extends AbstractActor {

    private final long timeDelayMillis;
    private Map<K, TimestampedValue> underlyingMap = new HashMap<>();
    private static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    public MapActor(long timeDelayMillis) {
        this.timeDelayMillis = timeDelayMillis;
    }

    static public Props props(long timeDelayMillis) {
        return Props.create(MapActor.class, () -> new MapActor(timeDelayMillis));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PutMessage.class, message -> {
                    long removalTime = System.currentTimeMillis() + timeDelayMillis;
                    underlyingMap.put((K)message.key, new TimestampedValue((V) message.value, removalTime));
                    if(timeDelayMillis >0){
                        Runnable removalSender = ()->self().tell(new RemovalMessage((K) message.key, removalTime), self());
                        executorService.schedule(removalSender, removalTime, TimeUnit.MILLISECONDS);
                    }
                })
                .match(GetMessage.class, message ->{
                    getSender().tell(underlyingMap.get(message.key).value, self());
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
    }

}
