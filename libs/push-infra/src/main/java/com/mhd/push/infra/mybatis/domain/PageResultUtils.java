package com.mhd.push.infra.mybatis.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mhd.push.common.enums.ErrorCodeEnum;

import java.util.List;

/**
 * 分页结果工具类
 *
 * @author zhao-hao-dong
 **/
public class PageResultUtils {

    private PageResultUtils() {
    }

    /**
     * 根据列表数据、总记录数构建表格分页数据对象
     *
     * @param list  列表数据
     * @param total 总记录数
     */
    public static <T> PageResponse<T> build(List<T> list, long total) {
        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setCode(ErrorCodeEnum.SUCCESS.getCode());
        pageResponse.setMessage(ErrorCodeEnum.SUCCESS.getMessage());
        pageResponse.setData(new PageResponse.PageInfo<>(list, total));
        return pageResponse;
    }

    /**
     * 根据分页对象构建表格分页数据对象
     *
     * @param page 分页对象
     */
    public static <T> PageResponse<T> build(IPage<T> page) {
        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setCode(ErrorCodeEnum.SUCCESS.getCode());
        pageResponse.setMessage(ErrorCodeEnum.SUCCESS.getMessage());
        pageResponse.setData(new PageResponse.PageInfo<>(page.getRecords(), page.getTotal()));
        return pageResponse;
    }
}
