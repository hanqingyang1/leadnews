package com.heima.user.service;

import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface AppFollowBehaviorService {

    //保存用户关注行为数据
    ResponseResult saveFollowBehavior(FollowBehaviorDto dto);
}
