package com.heima.user.service.impl;

import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApFollowBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mappers.app.ApBehaviorEntryMapper;
import com.heima.model.mappers.app.ApFollowBehaviorMapper;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.AppFollowBehaviorService;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AppFollowBehaviorServiceImpl implements AppFollowBehaviorService {


    @Autowired
    ApBehaviorEntryMapper apBehaviorEntryMapper;
    @Autowired
    ApFollowBehaviorMapper apFollowBehaviorMapper;
    /**
     * 保存用户关注行为
     * @param dto
     * @return
     */
    @Override
    @Async
    public ResponseResult saveFollowBehavior(FollowBehaviorDto dto) {
        ApUser user = AppThreadLocalUtils.getUser();
        Long userId = null;
        if(user != null){
            userId = user.getId();
        }
        if(userId == null && dto.getEquipmentId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //查询用户实体信息
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryMapper.selectByUserIdOrEquipmentId(userId, dto.getEquipmentId());
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //封装用户关注表信息
        ApFollowBehavior apFollowBehavior = new ApFollowBehavior();
        apFollowBehavior.setArticleId(dto.getArticleId());
        apFollowBehavior.setEntryId(apBehaviorEntry.getId());
        apFollowBehavior.setFollowId(dto.getFollowId());
        apFollowBehavior.setCreatedTime(new Date());

        int insert = apFollowBehaviorMapper.insert(apFollowBehavior);
        return ResponseResult.okResult(insert);
    }
}
