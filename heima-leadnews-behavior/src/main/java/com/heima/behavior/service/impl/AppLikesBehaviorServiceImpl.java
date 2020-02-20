package com.heima.behavior.service.impl;

import com.heima.behavior.service.AppLikesBehaviorService;
import com.heima.common.zookeeper.Sequences;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mappers.app.ApBehaviorEntryMapper;
import com.heima.model.mappers.app.ApLikesBehaviorMapper;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.common.BurstUtils;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@SuppressWarnings("ALL")
@Service
public class AppLikesBehaviorServiceImpl implements AppLikesBehaviorService {

    @Autowired
    private ApBehaviorEntryMapper apBehaviorEntryMapper;

    @Autowired
    private ApLikesBehaviorMapper apLikesBehaviorMapper;

    @Autowired
    private Sequences sequences;

    @Override
    public ResponseResult saveLikesBehavior(LikesBehaviorDto dto) {
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null && dto.getEquipmentId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        Long userId = null;
        if(user != null){
            userId = user.getId();
        }
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryMapper.selectByUserIdOrEquipmentId(userId, dto.getEquipmentId());
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.TOKEN_INVALID);
        }

        ApLikesBehavior apLikesBehavior = new ApLikesBehavior();
        apLikesBehavior.setId(sequences.sequenceApLikes());
        apLikesBehavior.setBehaviorEntryId(apBehaviorEntry.getId());
        apLikesBehavior.setOperation(dto.getOperation());
        apLikesBehavior.setEntryId(dto.getEntryId());
        apLikesBehavior.setType(dto.getType());
        apLikesBehavior.setCreatedTime(new Date());
        apLikesBehavior.setBurst(BurstUtils.encrypt(apLikesBehavior.getId(),apLikesBehavior.getBehaviorEntryId()));
        int insert = apLikesBehaviorMapper.insert(apLikesBehavior);
        return ResponseResult.okResult(insert);
    }
}
