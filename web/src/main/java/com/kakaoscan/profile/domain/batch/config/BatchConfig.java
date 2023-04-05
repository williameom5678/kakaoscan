package com.kakaoscan.profile.domain.batch.config;

import com.kakaoscan.profile.domain.batch.processor.UserLogProcessor;
import com.kakaoscan.profile.domain.batch.reader.UserLogReader;
import com.kakaoscan.profile.domain.batch.writer.UserLogWriter;
import com.kakaoscan.profile.domain.entity.UserLog;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class BatchConfig {
    public static final int BATCH_SIZE = 20;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserLogReader userLogReader;
    private final UserLogProcessor userLogProcessor;
    private final UserLogWriter userLogWriter;

    @Bean
    public Step userLogStep() {
        return stepBuilderFactory.get("userLogStep")
                .<UserLog, UserLog>chunk(BATCH_SIZE)
                .reader(userLogReader)
                .processor(userLogProcessor)
                .writer(userLogWriter)
                .build();
    }

    @Bean
    public Job userLogJob() {
        return jobBuilderFactory.get("userLogJob")
                .incrementer(new RunIdIncrementer())
                .start(userLogStep())
                .build();
    }
}
