package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.service.ApUserSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ArticleSearchServiceImpl implements com.heima.search.service.ArticleSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private ApUserSearchService apUserSearchService;
    @Override
    public ResponseResult search(UserSearchDto dto) throws IOException {

        if(dto==null|| StringUtils.isBlank(dto.getSearchWords())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtil.getUser();
        // asynchronous call save search records
        if(user!=null && dto.getFromIndex()==0){
            apUserSearchService.insert(dto.getSearchWords(), user.getId());
        }

        // set the query condition
        SearchRequest searchRequest = new SearchRequest("app_info_article");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // keyword split and query
        QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(dto.getSearchWords()).field("title").field("content").defaultOperator(Operator.OR);

        // boolean query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(queryStringQueryBuilder);


        // query the date that is less than mindate
        Date minTime = dto.getMinBehotTime();
        if (minTime == null) {
            minTime = new Date();
        }
        RangeQueryBuilder rangeQueryBuilder =
                QueryBuilders.rangeQuery("publishTime").lt(minTime.getTime());
        boolQueryBuilder.filter(rangeQueryBuilder);

        // pagination query
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(dto.getPageSize());

        // query in descending order of publish time
        searchSourceBuilder.sort("publishTime", SortOrder.DESC);

        // set highlight title
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>");
        highlightBuilder.postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);

        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        List<Map> list= new ArrayList<>();

        SearchHit[] hits = searchResponse.getHits().getHits();
        for(SearchHit hit: hits){
            String json = hit.getSourceAsString();
            Map map = JSON.parseObject(json, Map.class);

            // handle highlight
            if(hit.getHighlightFields()!=null && hit.getHighlightFields().size()>0){
                Text[] titles = hit.getHighlightFields().get("title").getFragments();
                String title = StringUtils.join(titles);
                // highlight title
                map.put("h_title", title);
            }
            else {
                // original title
                map.put("h_title", map.get("title"));
            }
            list.add(map);
        }
        return ResponseResult.okResult(list);

    }
}
