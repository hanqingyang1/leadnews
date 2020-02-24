package com.heima.model.mappers.app;

import com.heima.model.user.pojos.ApUser;
import org.apache.ibatis.annotations.Param;

public interface ApUserMapper {
    ApUser selectById(Long id);

    ApUser selectByApPhone(@Param("phone") String phone);
}