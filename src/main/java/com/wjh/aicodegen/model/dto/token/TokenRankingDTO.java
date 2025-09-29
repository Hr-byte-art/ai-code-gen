package com.wjh.aicodegen.model.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Token消耗排行榜DTO
 *
 * @author AI Assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRankingDTO {

    /**
     * 排行榜列表
     */
    private List<UserTokenSummaryDTO> rankings;

    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 当前页码
     */
    private Integer currentPage;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 排行榜类型说明
     */
    private String rankingType;
}
