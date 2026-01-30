package com.heima.model.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskTypeEnum {

    NEWS_SCAN_TIME(1001, 1,"review articles periodically"),
    REMOTEERROR(1002, 2,"third-party API call failed, retrying");
    private final int taskType; // corresponding to specific business
    private final int priority; // different levels of business
    private final String desc; //  description message
}