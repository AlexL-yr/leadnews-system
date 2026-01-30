package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmSensitiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper,WmSensitive> implements WmSensitiveService {
    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;
    @Override
    public ResponseResult delete(Integer id) {
        if(wmSensitiveMapper.selectOne(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getId,id))==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        wmSensitiveMapper.delete(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getId,id));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult list(SensitiveDto dto) {
        dto.checkParam();
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmSensitive> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(WmSensitive::getSensitives,dto.getName());
        page = page(page,lambdaQueryWrapper);
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult insert(WmSensitive wmSensitive) {
        Integer count = wmSensitiveMapper.selectCount(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getSensitives,wmSensitive.getSensitives()));
        if(count>0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"Created");
        }else{
            wmSensitive.setCreatedTime(new Date());
            wmSensitiveMapper.insert(wmSensitive);
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult update(WmSensitive wmSensitive) {
        if(wmSensitiveMapper.selectOne(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getId,wmSensitive.getId()))==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        wmSensitiveMapper.updateById(wmSensitive);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
