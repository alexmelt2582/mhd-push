package com.mhd.push.publicapi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhd.push.publicapi.domain.dto.ChannelAccountSaveDTO;
import com.mhd.push.publicapi.service.ChannelAccountService;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.constant.GlobalConstant;
import com.mhd.push.infra.persistence.entity.ChannelAccount;
import com.mhd.push.infra.persistence.mapper.ChannelAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class ChannelAccountServiceImpl extends ServiceImpl<ChannelAccountMapper, ChannelAccount>
    implements ChannelAccountService {

    @Override
    public List<ChannelAccount> queryByChannelTypeAndCreator(Integer channelType, String creator) {
        LambdaQueryWrapper<ChannelAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(ChannelAccount::getSendChannel, channelType)
                .eq(ChannelAccount::getCreator, creator);
        return list(queryWrapper);
    }

    @Override
    public List<ChannelAccount> queryByCreator(String creator) {
        LambdaQueryWrapper<ChannelAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(ChannelAccount::getCreator, creator);
        return list(queryWrapper);
    }

    @Override
    public int insertAccount(ChannelAccountSaveDTO channelAccountSaveDTO) {
        ChannelAccount channelAccount = BeanUtil.toBean(channelAccountSaveDTO, ChannelAccount.class);
        channelAccount.setCreated(Math.toIntExact(DateUtil.currentSeconds()));
        channelAccount.setIsDeleted(CommonConstant.FALSE);
        channelAccount.setCreator(CharSequenceUtil.isBlank(channelAccount.getCreator()) ? GlobalConstant.DEFAULT_CREATOR : channelAccount.getCreator());
        channelAccount.setUpdated(Math.toIntExact(DateUtil.currentSeconds()));
        log.debug("新增渠道账号成功：{}", channelAccount);
        return baseMapper.insert(channelAccount);
    }

    @Override
    public int updateAccount(ChannelAccountSaveDTO channelAccountSaveDTO) {
        ChannelAccount channelAccount = BeanUtil.toBean(channelAccountSaveDTO, ChannelAccount.class);
        channelAccount.setCreator(CharSequenceUtil.isBlank(channelAccount.getCreator()) ? GlobalConstant.DEFAULT_CREATOR : channelAccount.getCreator());
        channelAccount.setUpdated(Math.toIntExact(DateUtil.currentSeconds()));
        return baseMapper.updateById(channelAccount);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        if(CollUtil.isEmpty(ids)) {
            return;
        }
        removeByIds(ids);
    }
}




