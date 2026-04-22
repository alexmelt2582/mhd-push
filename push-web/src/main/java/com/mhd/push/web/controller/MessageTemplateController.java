package com.mhd.push.web.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.base.Throwables;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.mybatis.domain.PageParam;
import com.mhd.push.common.mybatis.domain.PageResponse;
import com.mhd.push.common.mybatis.domain.PageResultUtils;
import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import com.mhd.push.support.domain.entity.MessageTemplate;
import com.mhd.push.web.api.domain.MessageParam;
import com.mhd.push.web.api.domain.SendRequest;
import com.mhd.push.web.api.domain.SendResponse;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.web.api.service.SendService;
import com.mhd.push.web.domain.dto.MessageTemplateParam;
import com.mhd.push.web.domain.dto.MessageTemplateSaveDTO;
import com.mhd.push.web.exception.BusinessException;
import com.mhd.push.web.service.MessageTemplateService;
import com.mhd.push.web.utils.LoginUtils;
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
 * @author zhao-hao-dong

 **/
@Slf4j
@RestController
@RequestMapping("/messageTemplate")
@Validated
public class MessageTemplateController extends BaseController{
    @Resource
    private LoginUtils loginUtils;
    @Resource
    private MessageTemplateService messageTemplateService;
    @Resource
    private SendService sendService;
    @Value("${mhd.upload.crowd.path}")
    private String dataPath;

    /**
     * 分页列表数据
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
     * 根据Id查找
     */
    @GetMapping("/query/{id}")
    public BaseResponse<MessageTemplate> queryById(@PathVariable("id") Long id) {
        return BaseResultUtils.successOfData(messageTemplateService.selectTemplateById(id));
    }

    /**
     * 新增模板
     */
    @PostMapping("/add")
    public BaseResponse<Void> add(@RequestBody MessageTemplateSaveDTO messageTemplateSaveDTO) {
        return toAjax(messageTemplateService.insertTemplate(messageTemplateSaveDTO));
    }

    /**
     * 如果Id存在，则修改
     * 如果Id不存在，则保存
     */
    @PostMapping("/update")
    public BaseResponse<Void> update(@RequestBody MessageTemplateSaveDTO messageTemplateSaveDTO) {
        return toAjax(messageTemplateService.updateTemplate(messageTemplateSaveDTO));
    }

    /**
     * 根据Id删除
     * id多个用逗号分隔开
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
    ) {}

    /**
     * 测试发送接口
     */
    @PostMapping("/test")
    public BaseResponse<SendResponse> test(@RequestBody @Valid MessageTemplateTestRecord record) {
        Map<String, String> variables;
        try {
            // 增加异常处理，防止 JSON 格式错误导致服务崩溃
            variables = JSON.parseObject(record.msgContent(), new TypeReference<>() {
            });
        } catch (Exception e) {
            // 这里可以根据你的全局异常处理机制，抛出自定义业务异常
            throw new BusinessException(ErrorCodeEnum.CLIENT_BAD_PARAMETERS);
        }

        // 链式调用保持不变，逻辑清晰
        MessageParam messageParam = MessageParam.builder()
                .receiver(record.receiver())
                .templateParams(variables)
                .build();

        SendRequest sendRequest = SendRequest.builder()
                .code(SendTypeEnum.SEND.getCode())
                .templateId(record.id())
                .messageParam(messageParam)
                .build();

        SendResponse response = sendService.send(sendRequest);
        return BaseResultUtils.successOfData(response);
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
     * 上传人群文件
     */
    @PostMapping("upload")
    public Map<Object, Object> upload(@RequestParam("file") MultipartFile file) {
        String filePath = dataPath + IdUtil.fastSimpleUUID() + file.getOriginalFilename();
        try {
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
