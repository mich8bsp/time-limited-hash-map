package operationMessages;

/**
 * Created by U43155 on 29/05/2017.
 */
public class PutMessage<K, V> extends KeyValuePair<K, V>{
    public PutMessage(K key, V value) {
        super(key, value);
    }
}
