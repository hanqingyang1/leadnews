package com.heima.behavior.service.impl;

import com.heima.behavior.service.AppReadBehaviorService;
import com.heima.common.zookeeper.Sequences;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApReadBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mappers.app.ApBehaviorEntryMapper;
import com.heima.model.mappers.app.ApReadBehaviorMapper;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.common.BurstUtils;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@SuppressWarnings("ALL")
@Service
public class AppReadBehaviorServiceImpl implements AppReadBehaviorService {

    @Autowired
    private ApReadBehaviorMapper apReadBehaviorMapper;
    @Autowired
    private ApBehaviorEntryMapper apBehaviorEntryMapper;
    @Autowired
    private Sequences sequences;
    /**
     * 保存阅读行为
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveReadBehavior(ReadBehaviorDto dto) {
        ApUser user = AppThreadLocalUtils.getUser();
        if(user ==null && dto.getEquipmentId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        Long userId = null;
        if(user != null){
            userId = user.getId();
        }
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryMapper.selectByUserIdOrEquipmentId(userId, dto.getEquipmentId());
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApReadBehavior apReadBehavior = apReadBehaviorMapper.selectByEntryId(BurstUtils.groudOne(apBehaviorEntry.getId()), apBehaviorEntry.getId(), dto.getArticleId());
        boolean isInsert = false;
        if(apReadBehavior == null){
            apReadBehavior = new ApReadBehavior();
            apReadBehavior.setId(sequences.sequenceApReadBehavior());
            isInsert = true;
        }
        apReadBehavior.setEntryId(apBehaviorEntry.getId());
        apReadBehavior.setCount(dto.getCount());
        apReadBehavior.setPercentage(dto.getPercentage());
        apReadBehavior.setArticleId(dto.getArticleId());
        apReadBehavior.setLoadDuration(dto.getLoadDuration());
        apReadBehavior.setReadDuration(dto.getReadDuration());
        apReadBehavior.setCreatedTime(new Date());
        apReadBehavior.setUpdatedTime(new Date());
        apReadBehavior.setBurst(BurstUtils.encrypt(apReadBehavior.getId(),apReadBehavior.getEntryId()));
        // 插入
        if(isInsert){
            return ResponseResult.okResult(apReadBehaviorMapper.insert(apReadBehavior));
        }else {
            // 更新
            return ResponseResult.okResult(apReadBehaviorMapper.update(apReadBehavior));
        }

    }
}
