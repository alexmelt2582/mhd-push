package com.mhd.push.common.pipeline;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 责任链执行模板
 *
 * @author zhao-hao-dong
 */
@Getter
@Setter
public class ProcessTemplate {
    private List<BusinessProcess> processList;
}
