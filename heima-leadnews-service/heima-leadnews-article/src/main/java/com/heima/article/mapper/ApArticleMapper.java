package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.dtos.ArticleCommentDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.ArticleCommnetVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface ApArticleMapper extends BaseMapper<ApArticle> {
    /**
     * load the article list
     * @param dto
     * @param type
     * @return
     */
    public List<ApArticle> loadArticleList(ArticleHomeDto dto, Short type);

    public List<ApArticle> findArticleListByLast5Days(@Param("dateParam")Date dateParam);

    public Map queryLikesAndConllections(@Param("wmUserId") Integer wmUserId, @Param("beginDate") Date beginDate, @Param("endDate")  Date endDate);

    List<ArticleCommnetVo> findNewsComments(@Param("dto") ArticleCommentDto dto);

    int findNewsCommentsCount(@Param("dto")  ArticleCommentDto dto);
}
