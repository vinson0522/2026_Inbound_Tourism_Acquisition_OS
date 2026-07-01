package org.dromara.aiclient.model;

import lombok.Data;

import java.util.Map;

@Data
public class LandingModule {

    private String key;

    private Map<String, Object> content;
}
