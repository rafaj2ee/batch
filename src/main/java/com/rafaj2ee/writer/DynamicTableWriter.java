package com.rafaj2ee.writer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

public class DynamicTableWriter implements ItemWriter<Map<String, Object>> {
	
    private JdbcTemplate jdbcTemplate;
    private String tableName;
    private String originalTable;
    private static Map<String,String> map = new HashMap();
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setOriginalTable(String originalTable) {
        this.originalTable = originalTable;
    }

    private void createBackupTableIfNeeded() {
        if(!map.containsKey(tableName)) {
	    	// Obter schema da tabela original
	        String createSql = jdbcTemplate.queryForObject(
	            "SELECT sql FROM sqlite_master WHERE type='table' AND name = ?",
	            String.class,
	            originalTable
	        );
	        
	        // Modificar para criação condicional
	        String createBackupSql = createSql.replaceFirst("(?i)CREATE TABLE " + originalTable,
	                                                      "CREATE TABLE IF NOT EXISTS " + tableName);
	        // Executar criação da tabela
	        jdbcTemplate.execute(createBackupSql);
	
	        jdbcTemplate.execute("DELETE FROM "+tableName);
	        map.put(tableName, originalTable);
        }

    }

    @Override
    public void write(List<? extends Map<String, Object>> items) throws Exception {
        createBackupTableIfNeeded();
        if (!items.isEmpty()) {
            List<String> columns = items.get(0).keySet().stream()
                                      .collect(Collectors.toList());
            
            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                StringUtils.collectionToCommaDelimitedString(columns),
                StringUtils.collectionToCommaDelimitedString(
                    columns.stream()
                          .map(c -> "?")
                          .collect(Collectors.toList())
                )
            );

            jdbcTemplate.batchUpdate(sql, items.stream()
                .map(item -> columns.stream()
                                  .map(item::get)
                                  .toArray())
                .collect(Collectors.toList()));
        }
    }
}