package com.mhd.push.adminapi.controller;

import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;

/**
 * web层通用数据处理
 *
 * @author zhao-hao-dong
 */
public class BaseController {
    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected BaseResponse<Void> toAjax(int rows) {
        return rows > 0 ? BaseResultUtils.success() : BaseResultUtils.error();
    }

    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected BaseResponse<Void> toAjax(boolean result) {
        return result ? BaseResultUtils.success() : BaseResultUtils.error();
    }
}
