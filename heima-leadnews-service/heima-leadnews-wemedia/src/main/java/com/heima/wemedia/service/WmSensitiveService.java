package com.heima.wemedia.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;

public interface WmSensitiveService {
    ResponseResult delete(Integer id);

    ResponseResult list(SensitiveDto dto);

    ResponseResult insert(WmSensitive wmSensitive);

    ResponseResult update(WmSensitive wmSensitive);
}
