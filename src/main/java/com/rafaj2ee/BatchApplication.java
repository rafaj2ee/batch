package com.rafaj2ee;

import org.springframework.batch.core.BatchStatus;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BatchApplication {

    public static void main(String[] args) throws Exception {
    	String xml =  "spring-batch.xml";
		if(args.length > 0 ) {
			xml = args[0];
		}
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(xml)) {
            
            JobLauncher jobLauncher = context.getBean(JobLauncher.class);
            Job job = context.getBean("startJob", Job.class);
            
            JobParametersBuilder parametersBuilder = new JobParametersBuilder();

            if(args.length > 0 ) {
            	for(String argument : args) {
            		if(argument.indexOf("=")!=-1) {
	            		String argumentArray[] = argument.split("=");
	            		parametersBuilder.addString(argumentArray[0], argumentArray[1]); 
            		}
            	}
            }
            JobParameters jobParameters = parametersBuilder.toJobParameters();
            log.info(jobParameters.toString());
            JobExecution execution = jobLauncher.run(job, jobParameters);
            log.info("Start Date "+execution.getCreateTime()+" End Date "+execution.getEndTime());
            execution.getStepExecutions().forEach(stepExecution -> {
                log.info("Summary "+stepExecution.getSummary());
            });

            System.exit(SpringBatchExitCode.getExitCode(execution));
        }
    }
}

class SpringBatchExitCode {
    public static int getExitCode(JobExecution jobExecution) {
        return jobExecution.getStatus() == BatchStatus.COMPLETED ? 0 : 1;
    }
}
