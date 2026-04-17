package com.mhd.push.web.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.constant.GlobalConstant;
import com.mhd.push.common.enums.AuditStatus;
import com.mhd.push.common.enums.MessageStatus;
import com.mhd.push.common.mybatis.domain.PageParam;
import com.mhd.push.common.mybatis.util.MybatisPlusUtils;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.support.domain.entity.MessageTemplate;
import com.mhd.push.support.mapper.MessageTemplateMapper;
import com.mhd.push.web.domain.dto.MessageTemplateParam;
import com.mhd.push.web.domain.dto.MessageTemplateSaveDTO;
import com.mhd.push.web.service.MessageTemplateService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author zhao-hao-dong
 **/
@Service
public class MessageTemplateServiceImpl extends ServiceImpl<MessageTemplateMapper, MessageTemplate> implements MessageTemplateService {
    @Override
    public IPage<MessageTemplate> selectPageTemplateList(PageParam pageParam, MessageTemplateParam messageTemplateParam) {
        Page<MessageTemplate> page = MybatisPlusUtils.buildPage(pageParam, messageTemplateParam);
        LambdaQueryWrapper<MessageTemplate> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper
                .eq(MessageTemplate::getName, messageTemplateParam);
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public MessageTemplate selectTemplateById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public int insertTemplate(MessageTemplateSaveDTO messageTemplateSaveDTO) {
        MessageTemplate messageTemplate = BeanUtil.toBean(messageTemplateSaveDTO, MessageTemplate.class);
        messageTemplate.setFlowId(CharSequenceUtil.EMPTY)
                .setMsgStatus(MessageStatus.INIT.getCode()).setAuditStatus(AuditStatus.WAIT_AUDIT.getCode())
                .setCreator(CharSequenceUtil.isBlank(messageTemplate.getCreator()) ? GlobalConstant.DEFAULT_CREATOR : messageTemplate.getCreator())
                .setUpdator(CharSequenceUtil.isBlank(messageTemplate.getUpdator()) ? GlobalConstant.DEFAULT_UPDATOR : messageTemplate.getUpdator())
                .setTeam(CharSequenceUtil.isBlank(messageTemplate.getTeam()) ? GlobalConstant.DEFAULT_TEAM : messageTemplate.getTeam())
                .setAuditor(CharSequenceUtil.isBlank(messageTemplate.getAuditor()) ? GlobalConstant.DEFAULT_AUDITOR : messageTemplate.getAuditor())
                .setCreated(Math.toIntExact(DateUtil.currentSeconds()))
                .setIsDeleted(CommonConstant.FALSE)
                .setUpdated(Math.toIntExact(DateUtil.currentSeconds()));
        return baseMapper.insert(messageTemplate);
    }

    @Override
    public int updateTemplate(MessageTemplateSaveDTO messageTemplateSaveDTO) {
        MessageTemplate messageTemplate = BeanUtil.toBean(messageTemplateSaveDTO, MessageTemplate.class);
        messageTemplate.setUpdator(messageTemplate.getUpdator())
                .setMsgStatus(MessageStatus.INIT.getCode()).setAuditStatus(AuditStatus.WAIT_AUDIT.getCode());

        // 从数据库查询并注入 定时任务 ID
        MessageTemplate dbMsg = selectTemplateById(messageTemplate.getId());
        if (Objects.nonNull(dbMsg) && Objects.nonNull(dbMsg.getCronTaskId())) {
            messageTemplate.setCronTaskId(dbMsg.getCronTaskId());
        }

        //if (Objects.nonNull(messageTemplate.getCronTaskId()) && TemplateType.CLOCKING.getCode().equals(messageTemplate.getTemplateType())) {
        //    XxlJobInfo xxlJobInfo = xxlJobUtils.buildXxlJobInfo(messageTemplate);
        //    cronTaskService.saveCronTask(xxlJobInfo);
        //    cronTaskService.stopCronTask(messageTemplate.getCronTaskId());
        //}
        messageTemplate.setUpdated(Math.toIntExact(DateUtil.currentSeconds()));
        return baseMapper.updateById(messageTemplate);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        if(CollUtil.isEmpty(ids)) {
            return;
        }
        List<MessageTemplate> messageTemplates = baseMapper.selectByIds(ids);
        if(CollUtil.isEmpty(messageTemplates)) {
            return;
        }
        for (MessageTemplate messageTemplate : messageTemplates) {
            if(Objects.nonNull(messageTemplate.getCronTaskId()) && messageTemplate.getCronTaskId() > 0) {
                // TODO删除 xxljob
            }
        }
        baseMapper.deleteByIds(ids);
    }

    @Override
    public BasicResultVO startCronTask(Long id) {
        //// 1.获取消息模板的信息
        //MessageTemplate messageTemplate = messageTemplateDao.findById(id).orElse(null);
        //if (Objects.isNull(messageTemplate)) {
        //    return BasicResultVO.fail();
        //}
        //
        //// 2.动态创建或更新定时任务
        //XxlJobInfo xxlJobInfo = xxlJobUtils.buildXxlJobInfo(messageTemplate);
        //
        //// 3.获取taskId(如果本身存在则复用原有任务，如果不存在则得到新建后任务ID)
        //Integer taskId = messageTemplate.getCronTaskId();
        //BasicResultVO basicResultVO = cronTaskService.saveCronTask(xxlJobInfo);
        //if (Objects.isNull(taskId) && RespStatusEnum.SUCCESS.getCode().equals(basicResultVO.getStatus()) && Objects.nonNull(basicResultVO.getData())) {
        //    taskId = Integer.valueOf(String.valueOf(basicResultVO.getData()));
        //}
        //
        //// 4. 启动定时任务
        //if (Objects.nonNull(taskId)) {
        //    cronTaskService.startCronTask(taskId);
        //    MessageTemplate clone = ObjectUtil.clone(messageTemplate).setMsgStatus(MessageStatus.RUN.getCode()).setCronTaskId(taskId).setUpdated(Math.toIntExact(DateUtil.currentSeconds()));
        //    messageTemplateDao.save(clone);
        //    return BasicResultVO.success();
        //}
        return BasicResultVO.fail();
    }

    @Override
    public BasicResultVO stopCronTask(Long id) {
        //// 1.修改模板状态
        //MessageTemplate messageTemplate = messageTemplateDao.findById(id).orElse(null);
        //if (Objects.isNull(messageTemplate)) {
        //    return BasicResultVO.fail();
        //}
        //MessageTemplate clone = ObjectUtil.clone(messageTemplate).setMsgStatus(MessageStatus.STOP.getCode()).setUpdated(Math.toIntExact(DateUtil.currentSeconds()));
        //messageTemplateDao.save(clone);
        //
        //// 2.暂停定时任务
        //return cronTaskService.stopCronTask(clone.getCronTaskId());
        return BasicResultVO.fail();
    }
}
