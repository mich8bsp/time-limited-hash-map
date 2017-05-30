import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Created by U43155 on 29/05/2017.
 */
public class TimeLimitedHashMapTest {

    private Map<Integer, String> testMap;

    @Before
    public void init(){
        testMap = TimeLimitedHashMap.create(5000);
    }

    @After
    public void cleanup(){
        ((IClosableMap)testMap).close();
    }

    @Test
    public void testGetPut(){
        testMap.put(5, "test");
        testMap.put(4, "test2");
        Assert.assertEquals("test", testMap.get(5));
        Assert.assertEquals("test2", testMap.get(4));
        Assert.assertEquals(null, testMap.get(3));
    }
}
