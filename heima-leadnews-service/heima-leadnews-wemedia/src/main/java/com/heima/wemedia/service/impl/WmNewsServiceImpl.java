package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.pojos.*;
import com.heima.wemedia.mapper.*;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsTaskService;
import org.apache.commons.lang3.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        dto.checkParam();

        WmUser user = WmThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // query by the status
        if(dto.getStatus()!=null){
            lambdaQueryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }
        // query by the  channel
        if(dto.getChannelId()!=null){
            lambdaQueryWrapper.eq(WmNews::getChannelId,dto.getChannelId());
        }
        // query by the date
        if(dto.getBeginPubDate()!=null && dto.getEndPubDate()!=null){
            lambdaQueryWrapper.between(WmNews::getPublishTime,dto.getBeginPubDate(),dto.getEndPubDate());
        }
        // fuzzy query by the keyword
        if(StringUtils.isNotBlank(dto.getKeyword())){
            lambdaQueryWrapper.like(WmNews::getTitle,dto.getKeyword());
        }
        // query the current user's article
        lambdaQueryWrapper.eq(WmNews::getUserId,user.getId());
        lambdaQueryWrapper.orderByDesc(WmNews::getCreatedTime);

        page = page(page,lambdaQueryWrapper);

        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        if(dto == null || dto.getContent()==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto,wmNews);
        if(dto.getImages()!=null && dto.getImages().size()>0){
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);
        }
        saveOrUpdateWmNews(wmNews);
        if(dto.getStatus().equals(WmNews.Status.NORMAL.getCode())){
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        List<String> materials = extractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(materials,wmNews.getId());

        saveRelativeInfoForCover(dto,wmNews,materials);
//        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());

        wmNewsTaskService.addNewsToTask(wmNews.getId(),wmNews.getPublishTime());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
        List<String> images = dto.getImages();
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            if(materials.size()>=3){
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            }else if(materials.size()>1 && materials.size()<3){
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            }else{
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }

            if(images!=null && images.size()>=0){
                wmNews.setImages(StringUtils.join(images,","));
            }
            updateById(wmNews);
        }
        if(images!=null && images.size()>0){
            saveRelativeInfo(images,wmNews.getId(),WemediaConstants.WM_COVER_REFERENCE);
        }
    }

    /**
     * handel the relationship between images and materials
     * @param materials
     * @param newsId
     */
    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials,newsId,WemediaConstants.WM_CONTENT_REFERENCE);
    }

    @Autowired
    private WmMaterialMapper wmMaterialMapper;
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
        if (materials != null && !materials.isEmpty()) {

            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));

            if (dbMaterials == null || dbMaterials.size() == 0) {
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }

            if (materials.size() != dbMaterials.size()) {
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }
            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());

            wmNewsMaterialMapper.saveRelations(idList, newsId, type);
        }
    }

    private List<String> extractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);
        for(Map map : maps){
            if(map.get("type").equals("image")){
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }
        return materials;
    }

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    private void saveOrUpdateWmNews(WmNews wmNews){
        // complete the property
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short)1);

        if(wmNews.getId()==null){
            save(wmNews);
        }else {
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,wmNews.getId()));
            updateById(wmNews);
        }
    }
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;
    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {
        // check the parameter
        if(dto.getId()==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // query the article, determine whether it exists and has been published
        WmNews wmNews = getById(dto.getId());
        if(wmNews==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        if(!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"Only published articles can be operated");
        }
        // update the article and enable status
        if(dto.getEnable()!=null && dto.getEnable()>-1 && dto.getEnable()<2){
            update(Wrappers.<WmNews>lambdaUpdate().set(WmNews::getEnable,dto.getEnable())
                    .eq(WmNews::getId,wmNews.getId()));

            if(wmNews.getArticleId()!=null){
                // send the message, inform article service to update the article configuration
                Map<String, Object> map = new HashMap<>();
                map.put("articleId",wmNews.getArticleId());
                map.put("enable",dto.getEnable());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,JSON.toJSONString(map));
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult findList(NewsAuthDto dto) {
        dto.checkParam();
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if(dto.getStatus()==null){
            lambdaQueryWrapper.select();
        }
        else{
            lambdaQueryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }
        lambdaQueryWrapper.like(WmNews::getTitle,dto.getTitle());
        page = page(page,lambdaQueryWrapper);
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult findWmNewsVo(Integer id) {
        WmNews wmNews = getById(id);
        return ResponseResult.okResult(wmNews);
    }

    @Autowired
    private WmNewsMapper wmNewsMapper;
    @Override
    public ResponseResult updateStatus(Short status, NewsAuthDto dto) {
        dto.checkParam();
        WmNews wmNews = wmNewsMapper.selectOne(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getId,dto.getId()));
        if(status.equals(WemediaConstants.WM_NEWS_AUTH_PASS)){
            ResponseResult responseResult = saveAppArticle(wmNews);
            wmNews.setStatus(WemediaConstants.WM_NEWS_AUTH_PASS);
            wmNews.setReason("Manual review approved");
            wmNewsMapper.updateById(wmNews);
            wmNews.setArticleId((Long) responseResult.getData());

            if(!responseResult.getCode().equals(200)){
                throw new RuntimeException("Failed to publish article");
            }
            else if(status.equals(WemediaConstants.WM_NEWS_AUTH_FAIL)){
                if(dto.getMsg()==null){
                    wmNews.setReason("");
                }else {
                    wmNews.setReason(dto.getMsg());
                }
                wmNews.setStatus(WemediaConstants.WM_NEWS_AUTH_FAIL);
                wmNewsMapper.updateById(wmNews);
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult collect(Integer id) {
        if(id==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(id);
        return ResponseResult.okResult(wmNews);
    }

    @Override
    public ResponseResult deleteNews(Integer id) {
        if(id==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(id);
        if(wmNews==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        if(wmNews.getStatus()==9){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"Only deleted articles can be operated");
        }
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;

    @Qualifier("com.heima.apis.article.IArticleClient")
    private IArticleClient articleClient;
    private ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto dto = new ArticleDto();
        BeanUtils.copyProperties(wmNews,dto);

        dto.setLayout(wmNews.getType());
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if(wmChannel!=null){
            dto.setChannelName(wmChannel.getName());
        }
        dto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if(wmUser!=null){
            dto.setAuthorName(wmUser.getName());
        }

        if(wmNews.getArticleId()!=null){
            dto.setId(wmNews.getArticleId().longValue());
        }
        dto.setCreatedTime(new Date());
        ResponseResult responseResult = articleClient.saveArticle(dto);
        return responseResult;
    }

}
