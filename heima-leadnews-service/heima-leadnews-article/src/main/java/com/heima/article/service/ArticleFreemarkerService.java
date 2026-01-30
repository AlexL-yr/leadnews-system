package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ArticleFreemarkerService {

    /**
     * generate static file and upload to minio
     * @param apArticle
     */
    public void buildArticleToMinIO(ApArticle apArticle,String content);
}
