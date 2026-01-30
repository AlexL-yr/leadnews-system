package com.heima.schedule.test;

import com.heima.common.redis.CacheService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rocksdb.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RedisTest {

    @Autowired
    private CacheService cacheService;

    @Test
    public void testList(){
        // add element at the left of list
        cacheService.lLeftPush("list_001","hello,redis");
        // remove element from the right of list
        /*String list001 = cacheService.lRightPop("list_001");

        System.out.println(list001);*/
    }

    @Test
    public void testZset(){
        /*cacheService.zAdd("zset_key_001","hello zset 001",1000);
        cacheService.zAdd("zset_key_001","hello zset 002",2000);
        cacheService.zAdd("zset_key_001","hello zset 003",3000);
        cacheService.zAdd("zset_key_001","hello zset 004",4000);*/

        Set<String> zsetKey001 = cacheService.zRangeByScore("zset_key_001", 0, 3000);
        System.out.println(zsetKey001);
    }

    @Test
    public void testKeys(){
        Set<String> keys = cacheService.keys("future_*");
        System.out.println(keys);

        Set<String> scan = cacheService.scan("future_*");
        System.out.println(scan);
    }
}
