package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import org.springframework.web.bind.annotation.RequestBody;

public interface WmNewsService extends IService<WmNews> {

    /**
     * conditional query article list
     * @param dto
     * @return
     */
    public ResponseResult findList(@RequestBody WmNewsPageReqDto dto);

    public ResponseResult submitNews(WmNewsDto dto);

    /**
     * shelving and unshelving of the article
     * @param dto
     * @return
     */
    public ResponseResult downOrUp(@RequestBody WmNewsDto dto);

    public ResponseResult findList(@RequestBody NewsAuthDto dto);

    ResponseResult findWmNewsVo(Integer id);

    ResponseResult updateStatus(Short status,NewsAuthDto dto);

    ResponseResult collect(Integer id);

    ResponseResult deleteNews(Integer id);
}
