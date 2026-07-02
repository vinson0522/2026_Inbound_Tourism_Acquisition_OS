package org.dromara.project.billing;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class BillingExceptionHandler {

    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    @ExceptionHandler(QuotaExceededException.class)
    public R<Void> handleQuotaExceeded(QuotaExceededException e) {
        log.warn("quota exceeded: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }
}
