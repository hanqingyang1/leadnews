package com.heima.model.mappers.app;

import com.heima.model.behavior.pojos.ApShowBehavior;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ApShowBehaviorMapper {

    List<ApShowBehavior> selectListByEntryIdAndArticleIds(@Param("entryId") Integer entryId,@Param("articleIds") List<Integer> articleIds);

    void saveShowBehavior(@Param("articleIds") List<Integer> articleIds,@Param("entryId") Integer entryId);
}
