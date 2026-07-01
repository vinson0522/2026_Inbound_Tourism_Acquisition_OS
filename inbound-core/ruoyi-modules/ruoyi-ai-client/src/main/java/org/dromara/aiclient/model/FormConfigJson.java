package org.dromara.aiclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FormConfigJson {

    private List<String> fields;

    @JsonProperty("submit_label")
    private String submitLabel;

    @JsonProperty("whatsapp_link")
    private String whatsappLink;

    @JsonProperty("whatsapp_label")
    private String whatsappLabel;
}
