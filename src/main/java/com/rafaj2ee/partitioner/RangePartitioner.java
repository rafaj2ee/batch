package com.rafaj2ee.partitioner;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class RangePartitioner implements Partitioner {
	private Integer size;
	
    public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	@Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();

        int range = size / gridSize;
        Integer fromId = 1;
        Integer toId = range;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putInt("fromId", fromId);
            context.putInt("toId", toId);
            result.put("partition" + i, context);
            fromId = toId + 1;
            toId += range;
        }

        return result;
    }
}
