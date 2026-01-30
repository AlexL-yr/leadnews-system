package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

public interface ArticleSearchService {
    /**
     * es article paging retrieval
     * @param dto
     * @return
     */
    public ResponseResult search(@RequestBody UserSearchDto dto) throws IOException;
}
