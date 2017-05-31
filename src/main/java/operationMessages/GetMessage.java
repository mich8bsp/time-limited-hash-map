package operationMessages;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public class GetMessage<K, V> {
    public K key;

    public GetMessage(K key) {
        this.key=key;
    }
}
