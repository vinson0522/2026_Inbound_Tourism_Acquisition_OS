package org.dromara.project.billing;

import lombok.Getter;

/**
 * 套餐额度不足 — HTTP 402 · body code {@link #CODE}.
 */
@Getter
public class QuotaExceededException extends RuntimeException {

    public static final int CODE = 40201;
    public static final String DEFAULT_MESSAGE = "套餐额度不足，请升级";

    private final int code;

    public QuotaExceededException() {
        super(DEFAULT_MESSAGE);
        this.code = CODE;
    }

    public QuotaExceededException(String message) {
        super(message);
        this.code = CODE;
    }
}
