package com.hyhomelab.jwork;

import lombok.Data;

import java.util.List;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/5/7 16:51
 */
@Data
public class Config {
    private int concurrentNum;
    private int scanIntervalSec;
    private String prefix; // 前缀
    private List<String> onlyAllowQueues;
}
