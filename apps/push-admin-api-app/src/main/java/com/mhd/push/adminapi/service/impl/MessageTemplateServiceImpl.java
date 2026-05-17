package com.mhd.push.adminapi.service.impl;

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
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.domain.model.template.TemplateContentDefinition;
import com.mhd.push.domain.model.template.TemplateVariableDefinition;
import com.mhd.push.domain.utils.TemplateContentDefinitionUtils;
import com.mhd.push.common.mybatis.domain.PageParam;
import com.mhd.push.common.mybatis.util.MybatisPlusUtils;
import com.mhd.push.infra.persistence.entity.MessageTemplate;
import com.mhd.push.infra.persistence.mapper.MessageTemplateMapper;
import com.mhd.push.infra.service.MessageTemplateCacheService;
import com.mhd.push.adminapi.domain.dto.MessageTemplateParam;
import com.mhd.push.adminapi.domain.dto.MessageTemplateSaveDTO;
import com.mhd.push.adminapi.service.MessageTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 模板管理服务实现。
 */
@Service
public class MessageTemplateServiceImpl extends ServiceImpl<MessageTemplateMapper, MessageTemplate> implements MessageTemplateService {
    @Resource
    private MessageTemplateCacheService messageTemplateCacheService;

    /**
     * 分页查询模板列表。
     *
     * @param pageParam            分页参数
     * @param messageTemplateParam 查询条件
     * @return 模板分页结果
     */
    @Override
    public IPage<MessageTemplate> selectPageTemplateList(PageParam pageParam, MessageTemplateParam messageTemplateParam) {
        Page<MessageTemplate> page = MybatisPlusUtils.buildPage(pageParam, messageTemplateParam);
        LambdaQueryWrapper<MessageTemplate> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper
                .eq(MessageTemplate::getName, messageTemplateParam);
        IPage<MessageTemplate> resultPage = baseMapper.selectPage(page, queryWrapper);
        fillTemplateDefinitions(resultPage.getRecords());
        return resultPage;
    }

    /**
     * 按 ID 查询单个模板。
     *
     * @param id 模板 ID
     * @return 模板详情
     */
    @Override
    public MessageTemplate selectTemplateById(Long id) {
        MessageTemplate messageTemplate = messageTemplateCacheService.getTemplate(id);
        fillTemplateDefinition(messageTemplate);
        return messageTemplate;
    }

    /**
     * 新增模板。
     *
     * @param messageTemplateSaveDTO 模板保存参数
     * @return 新增后的模板ID，失败时返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertTemplate(MessageTemplateSaveDTO messageTemplateSaveDTO) {
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
        int result = baseMapper.insert(messageTemplate);
        if (result > 0) {
            messageTemplateCacheService.refresh(messageTemplate);
            return messageTemplate.getId();
        }
        return null;
    }

    /**
     * 更新模板。
     *
     * @param messageTemplateSaveDTO 模板保存参数
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTemplate(MessageTemplateSaveDTO messageTemplateSaveDTO) {
        MessageTemplate messageTemplate = BeanUtil.toBean(messageTemplateSaveDTO, MessageTemplate.class);
        messageTemplate.setUpdator(messageTemplate.getUpdator())
                .setMsgStatus(MessageStatus.INIT.getCode()).setAuditStatus(AuditStatus.WAIT_AUDIT.getCode());

        // 从数据库查询并注入定时任务 ID，避免更新时丢失原任务关系。
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
        int result = baseMapper.updateById(messageTemplate);
        if (result > 0) {
            messageTemplateCacheService.refresh(baseMapper.selectById(messageTemplate.getId()));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<MessageTemplate> messageTemplates = baseMapper.selectByIds(ids);
        if (CollUtil.isEmpty(messageTemplates)) {
            return;
        }
        for (MessageTemplate messageTemplate : messageTemplates) {
            if (Objects.nonNull(messageTemplate.getCronTaskId()) && messageTemplate.getCronTaskId() > 0) {
                // TODO删除 xxljob
            }
        }
        baseMapper.deleteByIds(ids);
        ids.forEach(messageTemplateCacheService::evict);
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

    /**
     * 为单个模板回填结构化模板内容。
     *
     * @param messageTemplate 模板实体
     */
    private void fillTemplateDefinition(MessageTemplate messageTemplate) {
        if (messageTemplate == null || CharSequenceUtil.isBlank(messageTemplate.getMsgContent())) {
            return;
        }
        TemplateContentDefinition contentDefinition = TemplateContentDefinitionUtils.parse(messageTemplate.getMsgContent());
        List<TemplateVariableDefinition> variables = TemplateContentDefinitionUtils.mergeDefinitions(contentDefinition);
        messageTemplate.setContent(contentDefinition.getContent());
        messageTemplate.setContentParamsSchema(contentDefinition.getContentParamsSchema());
        messageTemplate.setExtraParamsSchema(contentDefinition.getExtraParamsSchema());
        messageTemplate.setVariables(variables);
    }

    /**
     * 为模板列表批量回填结构化模板内容。
     *
     * @param messageTemplates 模板列表
     */
    private void fillTemplateDefinitions(List<MessageTemplate> messageTemplates) {
        if (messageTemplates == null || messageTemplates.isEmpty()) {
            return;
        }
        for (MessageTemplate messageTemplate : messageTemplates) {
            fillTemplateDefinition(messageTemplate);
        }
    }
}
