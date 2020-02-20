package com.heima.article.service;

import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.common.dtos.ResponseResult;

public interface AppArticleService {


    ResponseResult load(ArticleHomeDto dto, Short type);
}
