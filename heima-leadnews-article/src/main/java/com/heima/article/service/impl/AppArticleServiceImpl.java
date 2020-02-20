package com.heima.article.service.impl;

import com.heima.article.service.AppArticleService;
import com.heima.common.article.constans.ArticleConstans;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.mappers.app.ApArticleMapper;
import com.heima.model.mappers.app.ApUserArticleListMapper;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserArticleList;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Service
public class AppArticleServiceImpl implements AppArticleService {


    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private ApUserArticleListMapper apUserArticleListMapper;

    private static final Short MAX_PAGE_SIZE = 50;
    /**
     * 加载文章
     * @param dto
     * @param type 加载类型
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        //参数校验
        if(dto == null){
            dto = new ArticleHomeDto();
        }
        //如果最大时间为空设置为当前时间
        if(null == dto.getMaxBehotTime()){
            dto.setMaxBehotTime(new Date());
        }
        if(null == dto.getMinBehotTime()){
            dto.setMinBehotTime(new Date());
        }

        Integer size = dto.getSize();
        if(null == size || size <= 0){
            size = 20;
        }
        size = Math.min(size,MAX_PAGE_SIZE);
        dto.setSize(size);

        //文章频道校验
        if(StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstans.DEDAULT_TAG);
        }
        //校验类型
        if(!type.equals(ArticleConstans.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstans.LOADTYPE_LOAD_NEW)){
            type = ArticleConstans.LOADTYPE_LOAD_MORE;
        }
        //获取用户信息
        ApUser user = AppThreadLocalUtils.getUser();

        //判断用户是否存在
        if(user != null){
            //存在加载推荐信息
            List<ApArticle> articles = getUserArticle(user,dto,type);
            return ResponseResult.okResult(articles);
        }else{
            //不存在加载默认信息
            List<ApArticle> articles = getDefaultArticle(dto,type);
            return ResponseResult.okResult(articles);
        }

    }

    /**
     * 获取默认的文章信息
     * @param dto
     * @param type
     * @return
     */
    private List<ApArticle> getDefaultArticle(ArticleHomeDto dto, Short type) {

        return apArticleMapper.loadArticleListByLocation(dto,type);

    }

    /**
     * 获取推荐信息
     * @param user
     * @param dto
     * @param type
     * @return
     */
    private List<ApArticle> getUserArticle(ApUser user, ArticleHomeDto dto, Short type) {

        List<ApUserArticleList> list = apUserArticleListMapper.loadArticleIdListByUser(user,dto,type);
        if(!CollectionUtils.isEmpty(list)){
            return apArticleMapper.loadArticleListByIdList(list);
        }else{
            return getDefaultArticle(dto,type);
        }
    }
}
