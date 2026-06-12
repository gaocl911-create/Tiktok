package org.dromara.creator.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.creator.domain.CmContentPost;

/**
 * Content post mapper.
 */
public interface CmContentPostMapper extends BaseMapperPlus<CmContentPost, CmContentPost> {

    /**
     * Find content by (platform, platform_content_id) including soft-deleted rows.
     * <p>
     * Used by the discovery flow so that a content the user has soft-deleted will
     * NOT be re-created by future creator scans (soft delete = blacklist).
     * <p>
     * MyBatis-Plus auto-injected {@code selectOne()} adds {@code WHERE del_flag='0'},
     * which would let deleted items reappear; this method bypasses that filter.
     */
    @Select("SELECT * FROM cm_content_post WHERE platform = #{platform} "
        + "AND platform_content_id = #{platformContentId} LIMIT 1")
    CmContentPost selectIncludingDeleted(@Param("platform") String platform,
                                          @Param("platformContentId") String platformContentId);
}
