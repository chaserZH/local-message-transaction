package com.charserzh.lmt.core.config.job;

import com.charserzh.lmt.core.callback.LTCallback;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Data
public class LmtArrayTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LmtArrayTask.class);

    private List<StatusTransactionRecordEntity> tasks;

    private LTCallback ltCallback;

    public LmtArrayTask(List<StatusTransactionRecordEntity> tasks, LTCallback ltCallback) {
        this.tasks = tasks;
        this.ltCallback = ltCallback;
    }

    @Override
    public void run() {
        if (this.tasks == null || this.tasks.isEmpty()) {
            log.warn("tasks is empty");
            return;
        }
        this.tasks.forEach(task -> {
            log.info("task start, taskId:{}", task.getId());
            this.ltCallback.callback(task);
        });
    }
}
