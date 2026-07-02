package org.dromara.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 套餐周期 Job — snail-job 默认关闭时仍启用 {@code @Scheduled}。
 */
@Configuration
@EnableScheduling
public class BillingSchedulingConfig {
}
