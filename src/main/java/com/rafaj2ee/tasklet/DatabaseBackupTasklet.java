package com.rafaj2ee.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import com.rafaj2ee.BatchApplication;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import javax.sql.DataSource;
import java.util.List;

@Slf4j
public class DatabaseBackupTasklet implements Tasklet {

    private JdbcTemplate jdbcTemplate;
    private String backupSuffix = "_backup";

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void setBackupSuffix(String backupSuffix) {
        this.backupSuffix = backupSuffix;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        try {
            List<String> tables = getOriginalTables();
            processTables(tables);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro durante o backup: " + e.getMessage(), e);
        }
        return RepeatStatus.FINISHED;
    }

    private List<String> getOriginalTables() {
        return jdbcTemplate.queryForList(
            "SELECT name FROM sqlite_master " +
            "WHERE type='table' " +
            "AND name NOT LIKE 'sqlite_%' " +
            "AND name NOT LIKE ?", 
            String.class,
            "%" + backupSuffix
        );
    }

    private void processTables(List<String> tables) {
        for (String table : tables) {
            String backupTable = table + backupSuffix;
            createBackupTable(table, backupTable);
            copyTableData(table, backupTable);
        }
    }

    private void createBackupTable(String originalTable, String backupTable) {
        try {
            String createSql = jdbcTemplate.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name = ?", 
                String.class, 
                originalTable
            );
            
            String modifiedSql = createSql.replaceFirst("(?i)CREATE TABLE", "CREATE TABLE IF NOT EXISTS")
                                         .replaceFirst(originalTable, backupTable);
            
            jdbcTemplate.execute(modifiedSql);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criar tabela de backup para " + originalTable, e);
        }
    }

    private void copyTableData(String originalTable, String backupTable) {
        try {
            jdbcTemplate.update("DELETE FROM " + backupTable);
            jdbcTemplate.update("INSERT INTO " + backupTable + " SELECT * FROM " + originalTable);
        } catch (DataAccessException e) {
            throw new RuntimeException("Falha ao copiar dados de " + originalTable + " para " + backupTable, e);
        }
    }
}