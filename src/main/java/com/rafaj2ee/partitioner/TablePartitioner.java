package com.rafaj2ee.partitioner;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TablePartitioner implements Partitioner {

    private JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE '%_backup'", 
            String.class
        );
        
        Map<String, ExecutionContext> partitionMap = new HashMap<>();
        for (String table : tables) {
            ExecutionContext context = new ExecutionContext();
            context.putString("tableName", table);
            context.putString("backupTable", table + "_backup");
            partitionMap.put("partition_" + table, context);
        }
        return partitionMap;
    }
}