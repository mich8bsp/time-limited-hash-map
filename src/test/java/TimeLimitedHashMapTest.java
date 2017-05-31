import com.github.mich8bsp.tlhm.ITimeLimitedHashMap;
import com.github.mich8bsp.tlhm.MapClosedException;
import com.github.mich8bsp.tlhm.TimeLimitedHashMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Michael Bespalov on 29/05/2017.
 */
public class TimeLimitedHashMapTest {

    private Map<Integer, String> testMap;

    @Before
    public void init(){
        testMap = TimeLimitedHashMap.create(1000);
    }

    @After
    public void cleanup() throws InterruptedException {
        ((ITimeLimitedHashMap)testMap).close();
        Thread.sleep(200);
    }

    @Test
    public void testGetPut(){
        Assert.assertEquals(0, testMap.size());
        testMap.put(5, "test");
        Assert.assertEquals(1, testMap.size());
        testMap.put(4, "test2");
        Assert.assertEquals(2, testMap.size());
        Assert.assertEquals("test", testMap.get(5));
        Assert.assertEquals("test2", testMap.get(4));
        Assert.assertEquals(null, testMap.get(3));
    }

    @Test
    public void testRemove(){
        testMap.put(32, "before");
        String storedValue = testMap.remove(32);
        Assert.assertEquals("before", storedValue);
        Assert.assertEquals(0, testMap.size());
        Assert.assertTrue(testMap.isEmpty());
    }

    @Test
    public void testTimeout() throws InterruptedException {
        Assert.assertTrue(testMap.isEmpty());
        testMap.put(1, "aaa");
        testMap.put(2, "bbb");
        Thread.sleep(500);
        testMap.put(1, "aaab");
        Thread.sleep(600);
        Assert.assertFalse(testMap.isEmpty());
        Assert.assertEquals(1, testMap.size());
        Assert.assertEquals("aaab", testMap.get(1));
        Assert.assertNull(testMap.get(2));
        Thread.sleep(500);
        Assert.assertTrue(testMap.isEmpty());
        Assert.assertEquals(0, testMap.size());
        Assert.assertNull(testMap.get(1));
    }

    @Test
    public void testMapOperations(){
        testMap.put(5, "sdfs");
        testMap.put(55, "aaaa");
        testMap.put(314, "ascbffs");

        Assert.assertEquals(3, testMap.keySet().size());
        Assert.assertEquals(3, testMap.values().size());
        Assert.assertEquals(3, testMap.entrySet().size());
        Assert.assertTrue(testMap.containsValue("aaaa"));
        Assert.assertFalse(testMap.containsValue("aa"));
        Assert.assertFalse(testMap.containsKey(53));
        Assert.assertTrue(testMap.containsKey(55));

        testMap.remove(55);

        Assert.assertEquals(2, testMap.keySet().size());
        Assert.assertEquals(2, testMap.values().size());
        Assert.assertEquals(2, testMap.entrySet().size());
        Assert.assertFalse(testMap.containsValue("aaaa"));
        Assert.assertFalse(testMap.containsKey(55));

        Map<Integer, String> mapToPut = new HashMap<>();
        mapToPut.put(1, "sdfs");
        mapToPut.put(5, "aaaa");
        mapToPut.put(53, "bbbb");
        mapToPut.put(342, "asasa");

        testMap.putAll(mapToPut);

        Assert.assertEquals(5, testMap.keySet().size());
        Assert.assertEquals(5, testMap.values().size());
        Assert.assertEquals(5, testMap.entrySet().size());

        testMap.clear();

        Assert.assertEquals(0, testMap.keySet().size());
        Assert.assertEquals(0, testMap.values().size());
        Assert.assertEquals(0, testMap.entrySet().size());
    }

    @Test(expected = MapClosedException.class)
    public void testOperationAfterClose() throws InterruptedException {
        testMap.put(1, "asb");
        ((ITimeLimitedHashMap)testMap).close();
        testMap.put(1, "bsb");
        Thread.sleep(100);
        testMap.get(1);
    }

    @Test
    public void testConcurrent() throws InterruptedException {
        ((ITimeLimitedHashMap)testMap).close();
        Thread.sleep(1000);
        testMap = TimeLimitedHashMap.create(100);

        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(10);
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(() -> testMap.put(5, "aaa"), 0, 50, TimeUnit.MILLISECONDS);

        for(int i=0; i<1000; i++) {
            testMap.put(5, "bbb");
            if (i % 10 == 0) {
                testMap.remove(5);
            }
            Thread.sleep(10);
        }
        Assert.assertTrue(testMap.containsKey(5));
        future.cancel(true);
        Thread.sleep(110);
        Assert.assertFalse(testMap.containsKey(5));
    }

    @Test
    public void testCallbacks() throws InterruptedException {
        final int[] counter = {0};

        Consumer<Map.Entry<Integer, String>> consumer = entry -> counter[0]++;
        List<Consumer<Map.Entry<Integer, String>>> consumers = IntStream.range(0, 10)
                .boxed()
                .map(i -> consumer)
                .collect(Collectors.toList());
        ((ITimeLimitedHashMap<Integer, String>)testMap).addRemovalCallbacks(consumers);

        testMap.put(1, "sdf");
        Thread.sleep(500);
        testMap.put(5, "sdfs");

        Thread.sleep(600);

        Assert.assertEquals(consumers.size(), counter[0]);

        Thread.sleep(500);

        Assert.assertEquals(2 * consumers.size(), counter[0]);

    }
}
