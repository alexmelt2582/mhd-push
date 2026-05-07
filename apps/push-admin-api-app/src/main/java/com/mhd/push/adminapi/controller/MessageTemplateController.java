package com.mhd.push.adminapi.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.base.Throwables;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.exception.BusinessException;
import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import com.mhd.push.infra.mybatis.domain.PageParam;
import com.mhd.push.infra.mybatis.domain.PageResponse;
import com.mhd.push.infra.mybatis.domain.PageResultUtils;
import com.mhd.push.infra.persistence.entity.MessageTemplate;
import com.mhd.push.adminapi.domain.dto.MessageTemplateParam;
import com.mhd.push.adminapi.domain.dto.MessageTemplateSaveDTO;
import com.mhd.push.adminapi.service.MessageTemplateService;
import com.mhd.push.adminapi.utils.LoginUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模板管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/message/template")
@Validated
public class MessageTemplateController extends BaseController {
    @Resource
    private LoginUtils loginUtils;
    @Resource
    private MessageTemplateService messageTemplateService;
    @Value("${mhd.upload.crowd.path}")
    private String dataPath;

    /**
     * 分页查询模板列表。
     *
     * @param pageParam            分页参数
     * @param messageTemplateParam 查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    public PageResponse<MessageTemplate> queryList(@Valid PageParam pageParam, @Valid MessageTemplateParam messageTemplateParam) {
        if (loginUtils.needLogin()) {
            throw new BusinessException(ErrorCodeEnum.NO_LOGIN);
        }
        IPage<MessageTemplate> iPage = messageTemplateService.selectPageTemplateList(pageParam, messageTemplateParam);
        return PageResultUtils.build(iPage);
    }

    /**
     * 根据 ID 查询模板详情。
     *
     * @param id 模板 ID
     * @return 模板详情
     */
    @GetMapping("/query/{id}")
    public BaseResponse<MessageTemplate> queryById(@PathVariable("id") Long id) {
        return BaseResultUtils.successOfData(messageTemplateService.selectTemplateById(id));
    }

    /**
     * 新增模板。
     *
     * @param messageTemplateSaveDTO 模板保存参数
     * @return 新增后的模板ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> add(@RequestBody MessageTemplateSaveDTO messageTemplateSaveDTO) {
        Long templateId = messageTemplateService.insertTemplate(messageTemplateSaveDTO);
        if (templateId == null) {
            return BaseResultUtils.error();
        }
        return BaseResultUtils.successOfData(templateId);
    }

    /**
     * 更新模板。
     *
     * @param messageTemplateSaveDTO 模板保存参数
     * @return 执行结果
     */
    @PostMapping("/update")
    public BaseResponse<Void> update(@RequestBody MessageTemplateSaveDTO messageTemplateSaveDTO) {
        return toAjax(messageTemplateService.updateTemplate(messageTemplateSaveDTO));
    }

    /**
     * 根据 ID 删除模板。
     *
     * @param id 逗号分隔的模板 ID 列表
     * @return 执行结果
     */
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Void> deleteByIds(@PathVariable("id") String id) {
        if (CharSequenceUtil.isNotBlank(id)) {
            List<Long> idList = Arrays.stream(id.split(StrPool.COMMA)).map(Long::valueOf).collect(Collectors.toList());
            messageTemplateService.deleteByIds(idList);
        }
        return BaseResultUtils.success();
    }


    public record MessageTemplateTestRecord(
            @NotNull(message = "模板ID不能为空") Long id,
            @NotBlank(message = "接收者不能为空") String receiver,
            @NotBlank(message = "消息内容不能为空") String msgContent
    ) {
    }

    /**
     * 模板测试发送接口。
     *
     * @param record 测试发送参数
     * @return 执行结果
     */
    @PostMapping("/test")
    public BaseResponse<Void> test(@RequestBody @Valid MessageTemplateTestRecord record) {
        if (CharSequenceUtil.isBlank(record.msgContent()) || CharSequenceUtil.isBlank(record.receiver())) {
            throw new BusinessException(ErrorCodeEnum.CLIENT_BAD_PARAMETERS);
        }
        return BaseResultUtils.error(
                ErrorCodeEnum.SERVICE_ERROR,
                "模板测试发送仍依赖 public-api 发送链路，当前尚未迁移为 admin 侧共享能力，请先通过 public-api 调试发送。"
        );
    }

    ///**
    // * 获取需要测试的模板占位符，透出给Amis
    // */
    //@PostMapping("test/content")
    //public CommonAmisVo test(Long id) {
    //    MessageTemplate messageTemplate = messageTemplateService.queryById(id);
    //    return Convert4Amis.getTestContent(messageTemplate.getMsgContent());
    //}


    ///**
    // * 撤回接口（根据模板id撤回）
    // */
    //@PostMapping("recall/{id}")
    //public SendResponse recall(@PathVariable("id") String id) {
    //    SendRequest sendRequest = SendRequest.builder().code(BusinessCode.RECALL.getCode()).messageTemplateId(Long.valueOf(id)).build();
    //    SendResponse response = recallService.recall(sendRequest);
    //    if (!Objects.equals(response.getCode(), RespStatusEnum.SUCCESS.getCode())) {
    //        throw new CommonException(response.getMsg());
    //    }
    //    return response;
    //}


    ///**
    // * 启动模板的定时任务
    // */
    //@PostMapping("start/{id}")
    //public BasicResultVO start(@RequestBody @PathVariable("id") Long id) {
    //    return messageTemplateService.startCronTask(id);
    //}
    //
    ///**
    // * 暂停模板的定时任务
    // */
    //@PostMapping("stop/{id}")
    //public BasicResultVO stop(@RequestBody @PathVariable("id") Long id) {
    //    return messageTemplateService.stopCronTask(id);
    //}

    /**
     * 上传人群文件。
     *
     * @param file 人群文件
     * @return 上传结果
     */
    @PostMapping("upload")
    public Map<Object, Object> upload(@RequestParam("file") MultipartFile file) {
        String filePath = dataPath + IdUtil.fastSimpleUUID() + file.getOriginalFilename();
        try {
            // 1. 为本次上传生成隔离文件路径，避免不同上传相互覆盖。
            File localFile = new File(filePath);
            if (!localFile.exists()) {
                boolean res = localFile.mkdirs();
                if (!res) {
                    log.error("MessageTemplateController#upload fail! Failed to create folder.");
                    throw new BusinessException(ErrorCodeEnum.SERVICE_ERROR);
                }
            }
            file.transferTo(localFile);
        } catch (Exception e) {
            log.error("MessageTemplateController#upload fail! e:{},params{}", Throwables.getStackTraceAsString(e), JSON.toJSONString(file));
            throw new BusinessException(ErrorCodeEnum.SERVICE_ERROR);
        }
        return MapUtil.of(new String[][]{{"value", filePath}});
    }
}
