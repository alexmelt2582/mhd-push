package com.mhd.push.infra.mybatis.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 排序查询参数
 *
 * @author zhao-hao-dong
 **/
@Setter
@Getter
@ToString
public class SortableParam {
    private List<String> sortingFields;

    public static class SortingField {
        /**
         * 顺序 - 升序
         */
        public static final String ORDER_ASC = "asc";
        /**
         * 顺序 - 降序
         */
        public static final String ORDER_DESC = "desc";
        /**
         * 字段
         */
        private String field;
        /**
         * 顺序
         */
        private String order;

    }
}
