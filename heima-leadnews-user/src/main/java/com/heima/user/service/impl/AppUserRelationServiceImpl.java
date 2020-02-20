package com.heima.user.service.impl;


import com.heima.common.zookeeper.Sequences;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mappers.app.ApAuthorMapper;
import com.heima.model.mappers.app.ApUserFanMapper;
import com.heima.model.mappers.app.ApUserFollowMapper;
import com.heima.model.mappers.app.ApUserMapper;
import com.heima.model.user.dtos.UserRelationDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserFan;
import com.heima.model.user.pojos.ApUserFollow;
import com.heima.user.service.AppFollowBehaviorService;
import com.heima.user.service.AppUserRelationService;
import com.heima.utils.common.BurstUtils;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@SuppressWarnings("ALL")
@Service
public class AppUserRelationServiceImpl implements AppUserRelationService {


    @Autowired
    private ApUserFollowMapper apUserFollowMapper;
    @Autowired
    private ApUserFanMapper apUserFanMapper;
    @Autowired
    private ApUserMapper apUserMapper;
    @Autowired
    private ApAuthorMapper apAuthorMapper;
    @Autowired
    private Sequences sequences;
    @Autowired
    private AppFollowBehaviorService appFollowBehaviorService;


    /**
     * 关注
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult follow(UserRelationDto dto) {

        if (dto.getOperation() == null || dto.getOperation() < 0 || dto.getOperation() > 1) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "无效的Operation参数");
        }
        //获取followId
        Integer followId = dto.getUserId();
        if (followId == null && dto.getAuthorId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "userId和authorId不能同时为空");
        } else if (followId == null) {
            ApAuthor apAuthor = apAuthorMapper.selectById(dto.getAuthorId());
            followId = apAuthor.getUserId().intValue();
        }
        if (followId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "关注人不存在");
        } else {
            ApUser user = AppThreadLocalUtils.getUser();
            if (user != null) {
                //判断当前用户是否已经关注
                if (dto.getOperation() == 0) {

                    //关注操作
                    //保存 ap_user_follow ap_user_fan 用户关注行为
                    return followByUserId(user, followId, dto.getArticleId());
                } else {
                    //取消关注
                    //删除ap_user_follow ap_user_fan
                    return followCancelByUserId(user, followId);
                }
            } else {
                return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
            }

        }

    }

    /**
     * 处理关注逻辑
     *
     * @param user
     * @param followId
     * @param articleId
     * @return
     */
    private ResponseResult followByUserId(ApUser user, Integer followId, Integer articleId) {

        ApUser apUser = apUserMapper.selectById(followId);
        if (apUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "关注人不存在");
        }
        ApUserFollow apUserFollow = apUserFollowMapper.selectByFollowId(BurstUtils.groudOne(user.getId()), user.getId(), followId);
        if (apUserFollow == null) {
            ApUserFan apUserFan = apUserFanMapper.selectByFansId(BurstUtils.groudOne(followId), followId, user.getId());
            if (apUserFan == null) {
                apUserFan = new ApUserFan();
                apUserFan.setId(sequences.sequenceApUserFan());
                apUserFan.setUserId(followId);
                apUserFan.setFansId(user.getId());
                apUserFan.setFansName(user.getName());
                apUserFan.setLevel((short) 0);
                apUserFan.setIsDisplay(true);
                apUserFan.setIsShieldComment(false);
                apUserFan.setIsShieldLetter(false);
                apUserFan.setCreatedTime(new Date());
                apUserFan.setBurst(BurstUtils.encrypt(apUserFan.getId(), apUserFan.getUserId()));
                apUserFanMapper.insert(apUserFan);
            }
            apUserFollow = new ApUserFollow();
            apUserFollow.setId(sequences.sequenceApUserFollow());
            apUserFollow.setUserId(user.getId());
            apUserFollow.setFollowId(followId);
            apUserFollow.setFollowName(apUser.getName());
            apUserFollow.setCreatedTime(new Date());
            apUserFollow.setLevel((short) 0);
            apUserFollow.setIsNotice(true);
            apUserFollow.setBurst(BurstUtils.encrypt(apUserFollow.getId(), apUserFollow.getUserId()));
            // 记录关注行为
            FollowBehaviorDto dto = new FollowBehaviorDto();
            dto.setFollowId(followId);
            dto.setArticleId(articleId);
            appFollowBehaviorService.saveFollowBehavior(dto);
            return ResponseResult.okResult(apUserFollowMapper.insert(apUserFollow));

        } else {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "已关注");
        }

    }

    /**
     * 处理取消关注逻辑
     *
     * @param user
     * @param followId
     * @return
     */
    private ResponseResult followCancelByUserId(ApUser user, Integer followId) {
        ApUserFollow auf = apUserFollowMapper.selectByFollowId(BurstUtils.groudOne(user.getId()), user.getId(), followId);
        if (auf == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "未关注");
        } else {
            ApUserFan fan = apUserFanMapper.selectByFansId(BurstUtils.groudOne(followId), followId, user.getId());
            if (fan != null) {
                apUserFanMapper.deleteByFansId(BurstUtils.groudOne(followId), followId, user.getId());
            }
            return ResponseResult.okResult(apUserFollowMapper.deleteByFollowId(BurstUtils.groudOne(user.getId()), user.getId(), followId));
        }
    }


}
