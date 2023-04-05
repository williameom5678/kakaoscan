package com.kakaoscan.profile.domain.batch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Log4j2
public class JobRunner {

    private final JobLauncher jobLauncher;

    private final Job job;

    public void run() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("date", new Date())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            ExitStatus exitStatus = jobExecution.getExitStatus();
            if (exitStatus.equals(ExitStatus.FAILED)) {
                log.error("job failed");
                for (Throwable ex : jobExecution.getAllFailureExceptions()) {
                    log.error(ex.getMessage(), ex);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
