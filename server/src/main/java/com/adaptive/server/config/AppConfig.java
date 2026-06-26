package com.adaptive.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOriginsStr;

    /**
     * Shared RestTemplate for outbound calls to the AI question + tutor microservices
     * (the only two consumers of this bean — ML and Email build their own RestTemplates).
     * Explicit timeouts, matching {@link com.adaptive.server.service.MLClusteringService}'s
     * dedicated client, ensure a hung microservice call fails fast instead of blocking the
     * request thread forever and holding its DB connection (open-in-view) until the Hikari
     * pool is exhausted — which takes the whole app (including login) down.
     *
     * connect 10s tolerates a cold microservice waking up; read 60s is generous for an LLM
     * response yet bounds any hang so the connection is always released.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Bounded executor for background AI question pre-generation. Kept deliberately small
     * (max 2 threads) so background top-ups can't exhaust the 512MB/0.5-CPU instance or
     * flood the Python microservice. Overflow tasks queue; if the queue fills, the calling
     * thread runs the task itself (CallerRuns) rather than dropping a refill.
     */
    @Bean("aiPrewarmExecutor")
    public TaskExecutor aiPrewarmExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-prewarm-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOriginsStr.split(","))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
