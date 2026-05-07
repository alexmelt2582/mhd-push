package com.mhd.push.adminapi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhd.push.infra.mybatis.domain.PageParam;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.infra.persistence.entity.MessageTemplate;
import com.mhd.push.adminapi.domain.dto.MessageTemplateParam;
import com.mhd.push.adminapi.domain.dto.MessageTemplateSaveDTO;

import java.util.Collection;

/**
 * @author zhao-hao-dong
 **/
public interface MessageTemplateService extends IService<MessageTemplate> {
    /**
     * 分页查询消息模板列表
     *
     * @param pageParam            分页参数
     * @param messageTemplateParam 查询条件
     * @return 消息模板分页列表
     */
    IPage<MessageTemplate> selectPageTemplateList(PageParam pageParam, MessageTemplateParam messageTemplateParam);

    /**
     * 根据模板ID查询模板信息
     *
     * @param id 模板ID
     */
    MessageTemplate selectTemplateById(Long id);

    /**
     * 新增模板信息
     *
     * @param messageTemplateSaveDTO 模板信息
     * @return 新增后的模板ID，失败时返回 null
     */
    Long insertTemplate(MessageTemplateSaveDTO messageTemplateSaveDTO);

    /**
     * 修改模板信息
     *
     * @param messageTemplateSaveDTO 模板信息
     * @return 结果
     */
    int updateTemplate(MessageTemplateSaveDTO messageTemplateSaveDTO);

    /**
     * 根据id列表删除
     *
     * @param ids 模板ID列表
     */
    void deleteByIds(Collection<Long> ids);

    /**
     * 启动模板的定时任务
     *
     * @param id
     * @return
     */
    BasicResultVO startCronTask(Long id);

    /**
     * 暂停模板的定时任务
     *
     * @param id
     * @return
     */
    BasicResultVO stopCronTask(Long id);
}
