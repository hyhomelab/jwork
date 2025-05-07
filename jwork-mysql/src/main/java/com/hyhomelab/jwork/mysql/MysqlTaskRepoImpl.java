package com.hyhomelab.jwork.mysql;

import com.google.gson.Gson;
import com.hyhomelab.jwork.Task;
import com.hyhomelab.jwork.Trigger;
import com.hyhomelab.jwork.exception.TaskExistedException;
import com.hyhomelab.jwork.repo.TaskRepo;
import com.hyhomelab.jwork.value.TaskStatus;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/5/7 08:26
 */
public class MysqlTaskRepoImpl implements TaskRepo {

    static final String DefaultTableName = "jwork";
    private static final Logger log = LoggerFactory.getLogger(MysqlTaskRepoImpl.class);

    private final DataSource dataSource;
    private final String tableName;

    @Data
    public static final class TriggerData {
        private String cls;
        private String data;
    }

    public MysqlTaskRepoImpl(String tableName, DataSource dataSource) {
        this.tableName = tableName;
        this.dataSource = dataSource;
    }

    public MysqlTaskRepoImpl(DataSource dataSource) {
        this.tableName = DefaultTableName;
        this.dataSource = dataSource;
    }

    private int doUpdate(String sql, List<Object> params) throws SQLException {
        log.debug("sql: {}", sql);
        log.debug("params: {}", params);
        try (
                var conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            for (var i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            int rows = ps.executeUpdate();
            log.debug("result: {}", rows);
            return rows;

        }
    }

    private <T> T queryOne(String sql, List<Object> params, ResultSetMapper<T> mapper) {
        log.debug("sql: {}", sql);
        log.debug("params: {}", params);

        try (
                var conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            for (var i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    log.debug("result: {}", 1);
                    return mapper.map(rs);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private <T> List<T> queryList(String sql, List<Object> params, ResultSetMapper<T> mapper) {
        log.debug("sql: {}", sql);
        log.debug("params: {}", params);

        var result = new ArrayList<T>();
        try (
                var conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            for (var i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                log.debug("result: {}", result.size());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }


    @Override
    public void create(Task task) throws TaskExistedException {
        String sql = "INSERT INTO %s (task_id, queue, `group`, status, next_time_sec, data, `trigger`, retry_times, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)".formatted(tableName);

        List<Object> params = new ArrayList<>();
        params.add(task.getTaskId());
        params.add(task.getQueue());
        params.add(task.getGroup());
        params.add(task.getStatus().getValue());
        params.add(task.getNextTimeSec());
        params.add(task.getData());

        // trigger
        String triggerData = toTriggerData(task.getTrigger());
        params.add(triggerData);
        params.add(0); // retry_times

        Timestamp now = new Timestamp(Instant.now().toEpochMilli());
        params.add(now); // create_time
        params.add(now); // update_time

        try {
            doUpdate(sql, params);
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getMessage().contains("Duplicate")) {
                throw new TaskExistedException("taskId=%s".formatted(task.getTaskId()));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String toTriggerData(Trigger trigger) {
        if (trigger == null) {
            return new Gson().toJson(new TriggerData());
        }
        var triggerJson = new Gson().toJson(trigger);
        var triggerData = new TriggerData();
        triggerData.setCls(trigger.getClass().getName());
        triggerData.setData(triggerJson);
        return new Gson().toJson(triggerData);
    }

    @Override
    public boolean swapToRunning(String taskId, TaskStatus currentStatus, TaskStatus runningStatus, int maxRetryTimes) {
        String sql = """
                update %s set `status`=?, update_time=?
                where task_id=? and `status`=? and retry_times < ?
                """.formatted(tableName);

        List<Object> params = new ArrayList<>();
        params.add(runningStatus.getValue());                             // status
        params.add(new Timestamp(Instant.now().toEpochMilli()));         // update_time
        params.add(taskId);                                               // WHERE task_id
        params.add(currentStatus.getValue());                             // AND status
        params.add(maxRetryTimes);                                        // AND retry_times <=

        try {
            return doUpdate(sql, params) > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveSuccessResult(String taskId, TaskStatus taskStatus, String msg) {
        String sql = """
                update %s set `status`=?,`result`=?, update_time=?
                where task_id=?
                """.formatted(tableName);

        List<Object> params = new ArrayList<>();
        params.add(taskStatus.getValue());                                // status
        params.add(msg);                                                  // data/message
        params.add(new Timestamp(Instant.now().toEpochMilli()));         // update_time
        params.add(taskId);                                               // WHERE task_id

        try {
            doUpdate(sql, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void changeStatusTo(String taskId, TaskStatus taskStatus) {
        String sql = """
                update %s set `status`=?, update_time=?
                where task_id=?
                """.formatted(tableName);

        List<Object> params = new ArrayList<>();
        params.add(taskStatus.getValue());                                // SET status = ?
        params.add(new Timestamp(Instant.now().toEpochMilli()));         // SET update_time = ?
        params.add(taskId);                                               // WHERE task_id = ?

        try {
            doUpdate(sql, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Task> queryQueueTasksBefore(String queueName, TaskStatus taskStatus, long beforeSeconds, Integer scanLimitNum) {
        String sql = """
                SELECT id, task_id, queue, `group`, status, next_time_sec, data, `trigger`, retry_times, create_time, update_time, result FROM %s
                where `queue`=? and `status`=? and next_time_sec <= ? limit ?
                """.formatted(tableName);

        List<Object> params = new ArrayList<>();
        params.add(queueName);          // String: queueName
        params.add(taskStatus.getValue());  // String: taskStatus
        params.add(beforeSeconds);      // Long: beforeSeconds
        params.add(scanLimitNum);       // Int: scanLimitNum

        return queryList(sql, params, this::dbToTask);
    }

    @Override
    public void saveFailedResult(String taskId, TaskStatus taskStatus, Integer retryTimes, String message) {
        String sql = """
                update %s set `status`=?,`result`=?, update_time=?, retry_times=?
                where task_id=?
                """.formatted(tableName);

        List<Object> params = new ArrayList<>();
        params.add(taskStatus.getValue());                                // status
        params.add(message);                                              // data/message
        params.add(new Timestamp(Instant.now().toEpochMilli()));         // update_time
        params.add(retryTimes + 1);                                       // retry_times
        params.add(taskId);                                               // WHERE task_id

        try {
            doUpdate(sql, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Task getByTaskId(String taskId) {
        String sql = "SELECT id, task_id, queue, `group`, status, next_time_sec, data, `trigger`, retry_times, create_time, update_time, result FROM %s WHERE task_id = ?".formatted(tableName);

        List<Object> params = new ArrayList<>();
        params.add(taskId);

        return queryOne(sql, params, this::dbToTask);
    }

    private Task dbToTask(ResultSet rs) throws SQLException, ClassNotFoundException {
        Task t = new Task();
        t.setId(rs.getLong("id"));
        t.setTaskId(rs.getString("task_id"));
        t.setQueue(rs.getString("queue"));
        t.setGroup(rs.getString("group"));
        t.setStatus(TaskStatus.fromValue(rs.getString("status")));
        t.setNextTimeSec(rs.getLong("next_time_sec"));
        t.setData(rs.getString("data"));

        Trigger trigger = fromTriggerData(rs.getString("trigger"));
        t.setTrigger(trigger);

        t.setRetryTimes(rs.getInt("retry_times"));
        t.setResult(rs.getString("result"));
        return t;
    }

    private Trigger fromTriggerData(String trigger) throws ClassNotFoundException {
        if (trigger != null) {
            var triggerData = new Gson().fromJson(trigger, TriggerData.class);
            if (triggerData.getCls() != null && !triggerData.getCls().isBlank()) {
                Class<?> cls = Class.forName(triggerData.getCls());
                return (Trigger) new Gson().fromJson(triggerData.getData(), cls);
            }
        }
        return null;
    }

    @Override
    public void triggerToPending(String taskId, TaskStatus taskStatus, long nextTimeSec, Trigger trigger) {
        String sql = """
                update %s set `status`=?, next_time_sec=?, trigger=?, update_time=?
                where task_id=?
                """.formatted(tableName);

        List<Object> params = new ArrayList<>();
        params.add(taskStatus.getValue());                                // status
        params.add(nextTimeSec);                                          // next_time
        params.add(toTriggerData(trigger));                               // trigger
        params.add(new Timestamp(Instant.now().toEpochMilli()));         // update_time
        params.add(taskId);                                               // WHERE task_id

        try {
            doUpdate(sql, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resetTo(String taskId, TaskStatus taskStatus, long nextTimeSec, int resetRetryTimes) {
        String sql = """
                update %s set `status`=?, next_time_sec=?, retry_times=?, update_time=?
                where task_id=?
                """.formatted(tableName);
        List<Object> params = new ArrayList<>();
        params.add(taskStatus.getValue());                              // status
        params.add(nextTimeSec);                                        // next_time_sec
        params.add(resetRetryTimes);                                    // retry_times
        params.add(new Timestamp(Instant.now().toEpochMilli()));        // update_time
        params.add(taskId);                                             // where task_id

        try {
            doUpdate(sql, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
