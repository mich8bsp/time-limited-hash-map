# time-limited-hash-map [![Build Status](https://travis-ci.org/mich8bsp/time-limited-hash-map.svg?branch=master)](https://travis-ci.org/mich8bsp/time-limited-hash-map)
A concurrent hash map that supports removal of entries after they've not been updated for a specified amount of time

### How to use

Add the following dependency to the pom of your project:

```
<dependency>
    <groupId>com.github.mich8bsp</groupId>
    <artifactId>TimeLimitedHashMap</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Create the hashmap with:
```
Map<K, V> yourMap = TimeLimitedHashMap.create(ttl);
```
where ttl is the maximum time for map entries to remain in the map without update (in millis)

When entries have reached the maximum time, they will be removed and consumed by the supplied callbacks.

To supply callbacks or close the map, use: ``` ITimeLimitedHashMap ``` interface 
