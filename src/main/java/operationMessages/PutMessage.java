package operationMessages;

/**
 * Created by U43155 on 29/05/2017.
 */
public class PutMessage<K, V> {
    public K key;
    public V value;

    public PutMessage(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
