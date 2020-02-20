package com.heima.model.mappers.app;

import com.heima.model.article.pojos.ApHotWords;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ApHotWordsMapper {

    /**
     * 查询今日热词
     *
     * @param hotDate
     * @return
     */
    List<ApHotWords> queryByHotDate(@Param("hotDate") String hotDate);
}