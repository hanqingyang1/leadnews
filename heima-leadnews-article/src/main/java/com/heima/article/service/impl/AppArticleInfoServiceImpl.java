package com.heima.article.service.impl;

import com.heima.article.service.AppArticleInfoService;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.article.pojos.ApCollection;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.crawler.core.parse.ZipUtils;
import com.heima.model.mappers.app.*;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserFollow;
import com.heima.utils.common.BurstUtils;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AppArticleInfoServiceImpl implements AppArticleInfoService{

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;
    @Autowired
    private ApBehaviorEntryMapper apBehaviorEntryMapper;
    @Autowired
    private ApLikesBehaviorMapper apLikesBehaviorMapper;
    @Autowired
    private ApUnlikesBehaviorMapper apUnlikesBehaviorMapper;
    @Autowired
    private ApAuthorMapper apAuthorMapper;
    @Autowired
    private ApCollectionMapper apCollectionMapper;
    @Autowired
    private ApUserFollowMapper apUserFollowMapper;

    @Override
    public ResponseResult getArticleInfo(Integer articleId) {

        if(articleId == null || articleId < 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Map<String, Object> data = new HashMap<>();
        ApArticleConfig apArticleConfig = apArticleConfigMapper.selectByArticleId(articleId);
        if(apArticleConfig == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }else if (!apArticleConfig.getIsDelete()){
            ApArticleContent apArticleContent = apArticleContentMapper.selectByArticleId(articleId);
            String content = ZipUtils.gunzip(apArticleContent.getContent());
            apArticleContent.setContent(content);
            data.put("content",apArticleContent);
        }
        data.put("config",apArticleConfig);
        return ResponseResult.okResult(data);
    }

    /**
     * 加载文章详情的初始化配置信息，比如关注、喜欢、不喜欢、阅读位置等
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {
        //获取用户id
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null && dto.getEquipmentId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //获取用户id
        Long UserId = null;
        if(user != null){
            UserId = user.getId();
        }
        //查询用户行为实体
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryMapper.selectByUserIdOrEquipmentId(UserId, dto.getEquipmentId());
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //封装返回参数
        boolean isUnlike = false,isLike = false,isCollention = false,isFollow = false;
        //封装burst
        String burst = BurstUtils.groudOne(apBehaviorEntry.getId());
        //根据entryId和articleId查询收藏表，存在说明已收藏，否则没有收藏
        ApCollection apCollection = apCollectionMapper.selectForEntryId(burst, apBehaviorEntry.getId(), dto.getArticleId(), ApCollection.Type.ARTICLE.getCode());
        if(apCollection != null){
            isCollention = true;
        }
        //根据entryId和articleId查询点赞表，有数据说明已经点赞，否则没有点赞
        ApLikesBehavior apLikesBehavior = apLikesBehaviorMapper.selectLastLike(burst, apBehaviorEntry.getId(), dto.getArticleId(), ApLikesBehavior.Type.ARTICLE.getCode());
        if(apLikesBehavior != null && apLikesBehavior.getOperation() == ApLikesBehavior.Operation.LIKE.getCode()){
            isLike = true;
        }

        //根据entryId和articleId查询不喜欢表，有数据说明不喜欢，否侧喜欢
        ApUnlikesBehavior apUnlikesBehavior = apUnlikesBehaviorMapper.selectLastUnLike(apBehaviorEntry.getEntryId(),dto.getArticleId());
        if(apUnlikesBehavior != null && apUnlikesBehavior.getType() == ApUnlikesBehavior.Type.UNLIKE.getCode()){
            isUnlike = true;
        }
        //根据authorId查询用户信息
        ApAuthor apAuthor = apAuthorMapper.selectById(dto.getAuthorId());

        //根据当前用户的userId和app账号查询关注表，有数据说明已关注
        if(user != null && apAuthor != null && apAuthor.getUserId() !=null){
            ApUserFollow apUserFollow = apUserFollowMapper.selectByFollowId(BurstUtils.groudOne(user.getId()), user.getId(), apAuthor.getUserId().intValue());
            if(apUserFollow != null){
                isFollow = true;
            }
        }

        Map<String,Object> data = new HashMap<>();
        data.put("isfollow",isFollow);
        data.put("islike",isLike);
        data.put("isunlike",isUnlike);
        data.put("iscollection",isCollention);
        return ResponseResult.okResult(data);
    }
}
