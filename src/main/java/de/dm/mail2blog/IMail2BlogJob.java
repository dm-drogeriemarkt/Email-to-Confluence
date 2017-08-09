package de.dm.mail2blog;

import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;

public interface IMail2BlogJob {
    JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest);
}
