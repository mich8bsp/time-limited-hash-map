import java.util.Map;

/**
 * Created by U43155 on 29/05/2017.
 */
public interface IClosableMap<K, V> extends Map<K, V> {
    public void close();
}
