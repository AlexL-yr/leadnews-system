package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
    @Autowired
    private WmNewsMapper wmNewsMapper;

    @Override
    @Async // this method is an async method
    public void autoScanWmNews(Integer id) {
        // query the article
        WmNews wmNews = wmNewsMapper.selectById(id);
        if(wmNews ==null){
            throw new RuntimeException("article not exist");
        }
        if(wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            Map<String, Object> textAndImages = handleTextAndImages(wmNews);
            boolean isSensitive = handleSensitiveScan((String) textAndImages.get("content"), wmNews);
            if(!isSensitive) return;
        }

        if(wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            Map<String, Object> textAndImages = handleTextAndImages(wmNews);

            /*// review the content
            boolean isTextScan = handleTextScan((String) textAndImages.get("content"), wmNews);
            if (!isTextScan) {
                return;
            }*/

            // review the image
            boolean isImageScan = handleImageScan((List<String>) textAndImages.get("images"), wmNews);
            if (!isImageScan) return;
        }
            ResponseResult responseResult = saveAppArticle(wmNews);
            if(!responseResult.getCode().equals(200)){
                throw new RuntimeException("WmNewsAutoScanServiceImpl- fail to save the article");
            }
            wmNews.setArticleId((Long) responseResult.getData());

        // review successfully, update the status
        updateWmNews(wmNews,(short) 9, "approval successful");
    }

    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;
    private boolean handleSensitiveScan(String content, WmNews wmNews) {
        boolean flag = true;
        // get all the sensitive words
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());
        // initialize the sensitive word database
        SensitiveWordUtil.initMap(sensitiveList);

        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if(map.size()>0){
            updateWmNews(wmNews, (short) 2, "The content is in violation of regulations");
            flag = false;
        }

        return flag;
    }

    @Autowired
    private IArticleClient articleClient;
    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;
    private ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto dto = new ArticleDto();

        //property copy
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
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());

        ResponseResult responseResult = articleClient.saveArticle(dto);
        return responseResult;
    }

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private GreenImageScan greenImageScan;

    @Autowired
    private Tess4jClient tess4jClient;
    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        boolean flag = true;
        if(images==null ||images.size()==0) return flag;

        images = images.stream().distinct().collect(Collectors.toList());

        List<byte[]> imageList = new ArrayList<>();


            try {
                for (String image : images) {
                    byte[] bytes = fileStorageService.downLoadFile(image);

                    // covert byte[] to BufferedImage
                    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                    BufferedImage bufferedImage = ImageIO.read(in);

                    String result = tess4jClient.doOCR(bufferedImage);
                    boolean isSensitive = handleSensitiveScan(result, wmNews);
                    if(!isSensitive){
                        return isSensitive;
                    }
                    imageList.add(bytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }



        /*try {
            Map map = greenImageScan.imageScan(imageList);
            if (map != null) {
                if (map.get("suggestion").equals("block")) {
                    flag = false;
                    updateWmNews(wmNews, 2, "There is content in violation of regulations in the current article");
                }
                if (map.get("suggestion").equals("review")) {
                    flag = false;
                    updateWmNews(wmNews, 3, "There is uncertain content in the current article");
                }
            }
        }catch(Exception e){
            flag = false;
            e.printStackTrace();

        }*/
        return flag;
    }

    @Autowired
    private GreenTextScan greenTextScan;
    private boolean handleTextScan(String content, WmNews wmNews) {
        boolean flag = true;
        if (StringUtils.isBlank(wmNews.getTitle()) || StringUtils.isBlank(content)){
            return flag;
        }
        try {
            Map map = greenTextScan.greeTextScan(content);
            if(map!=null){
                if(map.get("suggestion").equals("block")){
                    flag = false;
                    updateWmNews(wmNews, 2, "There is content in violation of regulations in the current article");
                }
                if(map.get("suggestion").equals("review")){
                    flag = false;
                    updateWmNews(wmNews, 3, "There is uncertain content in the current article");
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;

    }

    private void updateWmNews(WmNews wmNews, int status, String reason) {
        wmNews.setStatus((short) status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    // fetch the text and image from content
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {
        StringBuilder stringBuilder = new StringBuilder();

        List<String> images = new ArrayList<>();

        if(StringUtils.isNotBlank(wmNews.getContent())){
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(),Map.class);
            for(Map map : maps){
                if(map.get("type").equals("text")){
                    stringBuilder.append(map.get("value"));
                }
                if(map.get("type").equals("image")){
                    images.add((String) map.get("value"));
                }
            }
        }
        if(StringUtils.isNotBlank(wmNews.getImages())){
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("content",stringBuilder.toString());
        resultMap.put("images",images);
        return resultMap;
    }
}
