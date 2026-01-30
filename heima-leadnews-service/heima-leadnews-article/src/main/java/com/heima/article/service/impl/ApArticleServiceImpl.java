    package com.heima.article.service.impl;

    import com.alibaba.fastjson.JSON;
    import com.baomidou.mybatisplus.core.toolkit.Wrappers;
    import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
    import com.heima.article.mapper.ApArticleConfigMapper;
    import com.heima.article.mapper.ApArticleContentMapper;
    import com.heima.article.mapper.ApArticleMapper;
    import com.heima.article.service.ApArticleService;
    import com.heima.article.service.ArticleFreemarkerService;
    import com.heima.common.constants.ArticleConstants;
    import com.heima.common.constants.BehaviorConstants;
    import com.heima.common.redis.CacheService;
    import com.heima.model.article.dtos.ArticleCommentDto;
    import com.heima.model.article.dtos.ArticleDto;
    import com.heima.model.article.dtos.ArticleHomeDto;
    import com.heima.model.article.dtos.ArticleInfoDto;
    import com.heima.model.article.pojos.ApArticle;
    import com.heima.model.article.pojos.ApArticleConfig;
    import com.heima.model.article.pojos.ApArticleContent;
    import com.heima.model.article.vos.HotArticleVo;
    import com.heima.model.common.dtos.PageResponseResult;
    import com.heima.model.common.dtos.ResponseResult;
    import com.heima.model.common.enums.AppHttpCodeEnum;
    import com.heima.model.mess.ArticleVisitStreamMess;
    import com.heima.model.user.pojos.ApUser;
    import com.heima.model.wemedia.dtos.StatisticsDto;
    import com.heima.utils.thread.AppThreadLocalUtil;
    import lombok.extern.slf4j.Slf4j;
    import org.apache.commons.lang.StringUtils;
    import org.springframework.beans.BeanUtils;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.*;
    import java.util.stream.Collectors;

    @Service
    @Transactional
    @Slf4j
    public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {
        @Autowired
        private ApArticleMapper apArticleMapper;
        private final static  short MAX_PAGE_SIZE = 50;

        @Autowired
        private CacheService cacheService;
        @Override
        public ResponseResult load(ArticleHomeDto dto, Short type) {
    //      validate parameters
            Integer size = dto.getSize();
            if(size ==null || size ==0){
                size =10;
            }
            size= Math.min(size, MAX_PAGE_SIZE);

            if(!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
                type = ArticleConstants.LOADTYPE_LOAD_MORE;
            }

            if(StringUtils.isBlank(dto.getTag())){
                dto.setTag(ArticleConstants.DEFAULT_TAG);
            }

            if(dto.getMaxBehotTime()==null){
                dto.setMaxBehotTime(new Date());
            }
            if(dto.getMinBehotTime()==null){
                dto.setMinBehotTime(new Date());
            }

            // query article list
            List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, type);
            // return the result
            return ResponseResult.okResult(apArticles);
        }

        @Autowired
        private ApArticleConfigMapper apArticleConfigMapper;

        @Autowired
        private ApArticleContentMapper apArticleContentMapper;

        @Autowired
        private ArticleFreemarkerService articleFreemarkerService;
        @Override
        public ResponseResult saveArticle(ArticleDto dto) {
            // examine the parameter
            if(dto ==null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
            }
            ApArticle apArticle = new ApArticle();
            BeanUtils.copyProperties(dto,apArticle);
            Date now = new Date();
            if (apArticle.getCreatedTime() == null) {
                apArticle.setCreatedTime(now);
            }
            if (apArticle.getPublishTime() == null) {
                apArticle.setPublishTime(now);
            }
            // determine whether id  exists
            if(dto.getId()==null){
                // if not exist insert
                apArticle.setCreatedTime(now);
                apArticle.setPublishTime(now);
                save(apArticle);

                ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
                apArticleConfigMapper.insert(apArticleConfig);

                ApArticleContent apArticleContent = new ApArticleContent();
                apArticleContent.setArticleId(apArticle.getId());
                apArticleContent.setContent(dto.getContent());
                apArticleContentMapper.insert(apArticleContent);
            }
            else{
                apArticle.setPublishTime(new Date());
                updateById(apArticle);
                ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getId()));
                apArticleContent.setContent(dto.getContent());
                apArticleContentMapper.updateById(apArticleContent);

            }
            articleFreemarkerService.buildArticleToMinIO(apArticle,dto.getContent());

            return ResponseResult.okResult(apArticle.getId());

        }

        @Override
        public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {

            //0.检查参数
            if (dto == null || dto.getArticleId() == null || dto.getAuthorId() == null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
            }

            //{ "isfollow": true, "islike": true,"isunlike": false,"iscollection": true }
            boolean isfollow = false, islike = false, isunlike = false, iscollection = false;

            ApUser user = AppThreadLocalUtil.getUser();
            if(user != null){
                //喜欢行为
                String likeBehaviorJson = (String) cacheService.hGet(BehaviorConstants.LIKE_BEHAVIOR + dto.getArticleId().toString(), user.getId().toString());
                if(org.apache.commons.lang3.StringUtils.isNotBlank(likeBehaviorJson)){
                    islike = true;
                }
                //不喜欢的行为
                String unLikeBehaviorJson = (String) cacheService.hGet(BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId().toString(), user.getId().toString());
                if(org.apache.commons.lang3.StringUtils.isNotBlank(unLikeBehaviorJson)){
                    isunlike = true;
                }
                //是否收藏
                String collctionJson = (String) cacheService.hGet(BehaviorConstants.COLLECTION_BEHAVIOR+user.getId(),dto.getArticleId().toString());
                if(org.apache.commons.lang3.StringUtils.isNotBlank(collctionJson)){
                    iscollection = true;
                }

                //是否关注
                Double score = cacheService.zScore(BehaviorConstants.APUSER_FOLLOW_RELATION + user.getId(), dto.getAuthorId().toString());
                System.out.println(score);
                if(score != null){
                    isfollow = true;
                }

            }

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("isfollow", isfollow);
            resultMap.put("islike", islike);
            resultMap.put("isunlike", isunlike);
            resultMap.put("iscollection", iscollection);

            return ResponseResult.okResult(resultMap);
        }

        @Override
        public void updateScore(ArticleVisitStreamMess mess) {
            //1. update collections likes unlikes comments
            ApArticle apArticle = updateArticle(mess);
            //2. compute the  score
            Integer score = computeScore(apArticle);
            score = score * 3;

            //3. replace data to redis
            replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());

            //4. update recommending date
            replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);

        }

        private void replaceDataToRedis(ApArticle apArticle, Integer score, String s) {
            String articleListStr = cacheService.get(s);
            if (StringUtils.isNotBlank(articleListStr)) {
                List<HotArticleVo> hotArticleVoList = JSON.parseArray(articleListStr, HotArticleVo.class);

                boolean flag = true;

                //If the article exists in the cache, only update its score
                for (HotArticleVo hotArticleVo : hotArticleVoList) {
                    if (hotArticleVo.getId().equals(apArticle.getId())) {
                        hotArticleVo.setScore(score);
                        flag = false;
                        break;
                    }
                }

                //If the data does not exist in the cache, query the data with the smallest score in the cache for score comparison. If the score of the current article is greater than the data in the cache, replace it
                if (flag) {
                    if (hotArticleVoList.size() >= 30) {
                        hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
                        HotArticleVo lastHot = hotArticleVoList.get(hotArticleVoList.size() - 1);
                        if (lastHot.getScore() < score) {
                            hotArticleVoList.remove(lastHot);
                            HotArticleVo hot = new HotArticleVo();
                            BeanUtils.copyProperties(apArticle, hot);
                            hot.setScore(score);
                            hotArticleVoList.add(hot);
                        }


                    } else {
                        HotArticleVo hot = new HotArticleVo();
                        BeanUtils.copyProperties(apArticle, hot);
                        hot.setScore(score);
                        hotArticleVoList.add(hot);
                    }
                }
                //cache to redis
                hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
                cacheService.set(s, JSON.toJSONString(hotArticleVoList));

            }
        }
        private Integer computeScore(ApArticle apArticle) {
            Integer score = 0;
            if(apArticle.getLikes() != null){
                score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
            }
            if(apArticle.getViews() != null){
                score += apArticle.getViews();
            }
            if(apArticle.getComment() != null){
                score += apArticle.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
            }
            if(apArticle.getCollection() != null){
                score += apArticle.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
            }

            return score;
        }

        private ApArticle updateArticle(ArticleVisitStreamMess mess) {
            ApArticle apArticle = getById(mess.getArticleId());
            apArticle.setCollection(apArticle.getCollection()==null?0:apArticle.getCollection()+mess.getCollect());
            apArticle.setComment(apArticle.getComment()==null?0:apArticle.getComment()+mess.getComment());
            apArticle.setLikes(apArticle.getLikes()==null?0:apArticle.getLikes()+mess.getLike());
            apArticle.setViews(apArticle.getViews()==null?0:apArticle.getViews()+mess.getView());
            updateById(apArticle);
            return apArticle;

        }

        @Override
        public ResponseResult queryLikesAndConllections(Integer wmUserId, Date beginDate, Date endDate) {
            Map map = apArticleMapper.queryLikesAndConllections(wmUserId, beginDate, endDate);
            return ResponseResult.okResult(map);
        }

        @Override
        public PageResponseResult newPage(StatisticsDto dto) {
            return null;
        }

        @Override
        public PageResponseResult findNewsComments(ArticleCommentDto dto) {
            return null;
        }

        @Override
        public ResponseResult load2(ArticleHomeDto dto, Short type, boolean firstPage) {
            if(firstPage){
                String jsonStr = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
                if(StringUtils.isNotBlank(jsonStr)){
                    List<HotArticleVo> hotArticleVoList = JSON.parseArray(jsonStr, HotArticleVo.class);
                    ResponseResult responseResult = ResponseResult.okResult(hotArticleVoList);
                    return responseResult;
                }
            }
            return load(dto,type);
        }
    }
