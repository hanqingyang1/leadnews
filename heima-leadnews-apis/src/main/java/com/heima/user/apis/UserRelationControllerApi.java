package com.heima.user.apis;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.UserRelationDto;

/**
 * 关注
 */
public interface UserRelationControllerApi {

    ResponseResult follow(UserRelationDto dto);
}