package com.mhd.push.adminapi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mhd.push.adminapi.domain.dto.ChannelAccountSaveDTO;
import com.mhd.push.infra.persistence.entity.ChannelAccount;

import java.util.Collection;
import java.util.List;

public interface ChannelAccountService extends IService<ChannelAccount> {

    /**
     * 根据渠道标识和creator查询列表信息
     */
    List<ChannelAccount> queryByChannelTypeAndCreator(Integer channelType, String creator);

    /**
     * 根据creator查询列表信息
     */
    List<ChannelAccount> queryByCreator(String creator);

    /**
     * 创建渠道账号信息
     */
    int insertAccount(ChannelAccountSaveDTO channelAccountSaveDTO);

    /**
     * 修改渠道账号信息
     */
    int updateAccount(ChannelAccountSaveDTO channelAccountSaveDTO);

    /**
     * 根据id列表删除
     */
    void deleteByIds(Collection<Long> ids);
}
