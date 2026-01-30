package com.heima.article.job;

import com.heima.article.service.HotArticleService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ComputeHotArticleJob {
    @Autowired
    private HotArticleService hotArticleService;
    @XxlJob("computeHotArticleJob")
    public void handle(){
        log.info("Hot article score calculation and scheduling task start");
        hotArticleService.computeHotArticle();
        log.info("Hot article score calculation and scheduling task end");
    }
}
