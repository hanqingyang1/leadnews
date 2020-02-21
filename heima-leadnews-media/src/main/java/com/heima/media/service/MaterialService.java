package com.heima.media.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.media.dtos.WmMaterialDto;
import com.heima.model.media.dtos.WmMaterialListDto;
import org.springframework.web.multipart.MultipartFile;

public interface MaterialService {
    /**
	* 上传图片接口*  
	* @param multipartFile*  
	* @return*  
	*/  
	ResponseResult uploadPicture(MultipartFile multipartFile);

	/**
	 * 上传图片
	 * @param wmMaterialDto
	 * @return
	 */
	ResponseResult delPicture(WmMaterialDto wmMaterialDto);

	/**
	 * 分页查询列表
	 * @param dto
	 * @return
	 */
	ResponseResult findList(WmMaterialListDto dto);

	/**
	 * 收藏或取消收藏
	 * @param dto
	 * @param type
	 * @return
	 */
	ResponseResult changeUserMaterialStatus(WmMaterialDto dto,Short type);
}