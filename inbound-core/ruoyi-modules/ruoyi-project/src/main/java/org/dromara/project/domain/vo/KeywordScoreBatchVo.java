package org.dromara.project.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KeywordScoreBatchVo {

    private int scoredCount;

    private List<KeywordScoreVo> results = new ArrayList<>();
}
