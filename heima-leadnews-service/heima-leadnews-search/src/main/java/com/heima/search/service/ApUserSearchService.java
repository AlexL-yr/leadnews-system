package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;

public interface ApUserSearchService {

    /**
     * save user search records
     * @param keyword get from dto
     * @param userId get from local thread
     */
    public void insert(String keyword,Integer userId);

    public ResponseResult findUserSearch();

    public ResponseResult delUserSearch(HistorySearchDto dto);
}
