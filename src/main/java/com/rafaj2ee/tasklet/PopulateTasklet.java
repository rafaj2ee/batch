package com.rafaj2ee.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PopulateTasklet implements Tasklet {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Integer interval;

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Integer fromId = (Integer) chunkContext.getStepContext().getStepExecutionContext().get("fromId");
        Integer toId = (Integer) chunkContext.getStepContext().getStepExecutionContext().get("toId");

        try {
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

            for (int i = fromId; i <= toId; i++) {
                jdbcTemplate.update("INSERT INTO counter (count, name) VALUES (?, ?)", i, "name" + i);
                contribution.incrementWriteCount(1);
                if (i % interval == 0 && i != fromId) {
                    transactionManager.commit(status);
                    log.info("Committed up to " + i);

                    // Inicie uma nova transação após o commit
                    status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                }
            }

            // Commit final após o loop
            transactionManager.commit(status);
            log.info("Final commit after processing IDs from " + fromId + " to " + toId);

        } catch (Exception e) {
            throw new RuntimeException("Erro durante a inserção: " + e.getMessage(), e);
        }
        return RepeatStatus.FINISHED;
    }
}
