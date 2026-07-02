package org.dromara.project.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class KeywordScoreBatchBo {

    private List<Long> keywordIds;

    private Boolean useRag;
}
