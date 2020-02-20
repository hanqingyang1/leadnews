package com.heima.behavior.service.impl;

import com.heima.behavior.service.AppShowBehaviorService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.behavior.dtos.ShowBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApShowBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mappers.app.ApBehaviorEntryMapper;
import com.heima.model.mappers.app.ApShowBehaviorMapper;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppShowBehaviorServiceImpl implements AppShowBehaviorService {

    @Autowired
    private ApBehaviorEntryMapper apBehaviorEntryMapper;

    @Autowired
    private ApShowBehaviorMapper apShowBehaviorMapper;

    @Override
    public ResponseResult saveShowBehavior(ShowBehaviorDto dto) {
        //获取用户id 或设备id
        ApUser user = AppThreadLocalUtils.getUser();
        if(user ==null && dto.getEquipmentId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        Long userId = null;
        //根绝用户id 或设备id 获取用户实体
        if(user != null){
            userId = user.getId();
        }

        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryMapper.selectByUserIdOrEquipmentId(userId,dto.getEquipmentId());
//        Integer entryId = null;
//        if(apBehaviorEntry != null){
//            entryId = apBehaviorEntry.getEntryId();
//        }
        if (apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //获取前台传过来的文章列表ID
        List<ApArticle> articles = dto.getArticleIds();
        List<Integer> articleIds = articles.stream().map(ApArticle::getId).collect(Collectors.toList());
        //根据实体id 和文章列表id 查询APP 行为表
        List<ApShowBehavior> apShowBehaviors = apShowBehaviorMapper.selectListByEntryIdAndArticleIds(apBehaviorEntry.getEntryId(),articleIds);
        //过滤数据删除，已存在的文章列表
        apShowBehaviors.forEach(item -> {
            Integer articleId = item.getArticleId();
            articleIds.remove(articleId);
        });
        //保存操作
        if(!CollectionUtils.isEmpty(articleIds)){
            apShowBehaviorMapper.saveShowBehavior(articleIds,apBehaviorEntry.getEntryId());
        }
        return ResponseResult.okResult(0);
    }
}
