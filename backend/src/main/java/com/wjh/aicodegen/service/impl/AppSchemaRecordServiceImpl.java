package com.wjh.aicodegen.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.core.builder.SqlExecutor;
import com.wjh.aicodegen.mapper.AppSchemaRecordMapper;
import com.wjh.aicodegen.model.entity.AppSchemaRecord;
import com.wjh.aicodegen.service.AppSchemaRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用 Schema 记录 服务实现。
 *
 * @author 王哈哈
 */
@Service
@Slf4j
public class AppSchemaRecordServiceImpl extends ServiceImpl<AppSchemaRecordMapper, AppSchemaRecord>
        implements AppSchemaRecordService {

    @Resource
    private SqlExecutor sqlExecutor;

    @Override
    public void recordTable(Long appId, String tableName) {
        AppSchemaRecord record = AppSchemaRecord.builder()
                .appId(appId)
                .tableName(tableName)
                .createTime(LocalDateTime.now())
                .build();
        save(record);
        log.info("记录应用表: appId={}, tableName={}", appId, tableName);
    }

    @Override
    public List<AppSchemaRecord> getByAppId(Long appId) {
        return list(QueryWrapper.create().where("app_id = ?", appId));
    }

    @Override
    public void dropAndRemoveByAppId(Long appId) {
        List<AppSchemaRecord> records = getByAppId(appId);
        if (records.isEmpty()) {
            return;
        }

        List<String> tableNames = records.stream()
                .map(AppSchemaRecord::getTableName)
                .collect(Collectors.toList());

        // DROP TABLE
        for (String tableName : tableNames) {
            try {
                sqlExecutor.dropTables("", List.of(tableName));
            } catch (Exception e) {
                log.warn("删除表失败: {}, 错误: {}", tableName, e.getMessage());
            }
        }

        // 删除记录
        remove(QueryWrapper.create().where("app_id = ?", appId));
        log.info("清理应用 schema: appId={}, 删除了 {} 个表", appId, tableNames.size());
    }
}
