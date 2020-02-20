package com.heima.login.apis;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.pojos.ApUser;
import org.springframework.web.bind.annotation.RequestBody;

public interface LoginControllerApi {

    public ResponseResult login(@RequestBody ApUser user);
}