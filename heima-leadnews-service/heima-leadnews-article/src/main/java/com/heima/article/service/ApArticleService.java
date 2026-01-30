package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.wemedia.dtos.StatisticsDto;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;

public interface ApArticleService extends IService<ApArticle> {
    /**
     *
     * @param dto
     * @param type 1: load more 2: load newest
     * @return
     */
    public ResponseResult load(ArticleHomeDto dto,Short type);

    /**
     * load more or load newest
     * @param dto
     * @param type
     * @param firstPage true: first page false: not first page
     * @return
     */
    public ResponseResult load2(ArticleHomeDto dto,Short type,boolean firstPage);


    /**
     * save the relevant article in app terminal
     * @param dto
     * @return
     */
    public ResponseResult saveArticle(ArticleDto dto);

    /**
     *
     * @param dto
     * @return
     */
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto);


    public void updateScore(ArticleVisitStreamMess mess);

    ResponseResult queryLikesAndConllections(Integer wmUserId, Date beginDate, Date endDate);


    PageResponseResult newPage(StatisticsDto dto);

    public PageResponseResult findNewsComments(ArticleCommentDto dto);
}
