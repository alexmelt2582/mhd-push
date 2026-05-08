package com.mhd.push.common.mybatis.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回对象
 *
 * @author zhao-hao-dong
 **/
@Getter
@Setter
@ToString
public class PageResponse<T> implements Serializable {

    private String code;
    private PageInfo<T> data;
    private String message;

    @AllArgsConstructor
    static class PageInfo<T> {
        /**
         * 分页数据
         */
        private List<T> list;
        /**
         * 分页中数据总数
         */
        private Long total;
    }
}