package com.heima.xxljob.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class HelloJob {

    @Value("${server.port}")
    private String port;

/*    @XxlJob("demoJobHandler")
    public void helloJob(){
        System.out.println("简单任务执行了。。。。"+port);
        log.info("简单任务执行了。。。。");
    }*/

    @XxlJob("shardingJobHandler")
    public void shardingJobHandler(){
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        List<Integer> list = getList();
        for(Integer integer: list){
            if(integer % shardTotal == shardIndex){
                System.out.println("当前第"+shardIndex+"分片处理数据："+integer);
            }
        }
    }

    public List<Integer> getList(){
        List<Integer> list = new ArrayList<>();
        for(int i=0;i<100;i++){
            list.add(i);
        }
        return list;
    }

}