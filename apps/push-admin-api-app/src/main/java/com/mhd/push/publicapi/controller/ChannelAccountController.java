package com.mhd.push.publicapi.controller;

import cn.hutool.core.text.CharSequenceUtil;
import com.mhd.push.publicapi.domain.dto.ChannelAccountSaveDTO;
import com.mhd.push.publicapi.service.ChannelAccountService;
import com.mhd.push.common.constant.GlobalConstant;
import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import com.mhd.push.infra.persistence.entity.ChannelAccount;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 渠道账号管理接口
 *
 * @author zhao-hao-dong
 */
@Slf4j
@RestController
@RequestMapping("/account")
public class ChannelAccountController extends BaseController{
    @Resource
    private ChannelAccountService channelAccountService;

    /**
     * 所有的渠道账号信息
     */
    @GetMapping("/list")
    public BaseResponse<List<ChannelAccount>> list(String creator) {
        creator = CharSequenceUtil.isBlank(creator) ? GlobalConstant.DEFAULT_CREATOR : creator;
        return BaseResultUtils.successOfData(channelAccountService.queryByCreator(creator));
    }

    /**
     * 根据渠道标识查询渠道账号相关的信息
     */
    @GetMapping("/queryByChannelType")
    public BaseResponse<List<ChannelAccount>> query(Integer channelType, String creator) {
        creator = CharSequenceUtil.isBlank(creator) ? GlobalConstant.DEFAULT_CREATOR : creator;
        return BaseResultUtils.successOfData(channelAccountService.queryByChannelTypeAndCreator(channelType, creator));
    }

    /**
     * 创建渠道账号
     */
    @PostMapping("/save")
    public BaseResponse<Void> save(@Valid @RequestBody ChannelAccountSaveDTO channelAccountSaveDTO) {
        return toAjax(channelAccountService.insertAccount(channelAccountSaveDTO));
    }

    /**
     * 修改渠道账号
     */
    @PutMapping("/update")
    public BaseResponse<Void> update(@Valid @RequestBody ChannelAccountSaveDTO channelAccountSaveDTO) {
        return toAjax(channelAccountService.updateAccount(channelAccountSaveDTO));
    }

    /**
     * 根据Id列表删除
     */
    @DeleteMapping("/delete")
    public BaseResponse<Void> deleteByIds(@NotEmpty(message = "ID列表不能为空") @RequestBody Set<Long> ids) {
        channelAccountService.deleteByIds(ids);
        return BaseResultUtils.success();
    }
}
