package org.dromara.aiclient.model;

import lombok.Data;

import java.util.List;

@Data
public class StageKeywords {

    private String stage;

    private List<GeneratedKeyword> keywords;
}
