package com.wjh.aicodegen.service;

import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.entity.AppSchemaRecord;

import java.util.List;

/**
 * 应用 Schema 记录 服务层。
 *
 * @author 王哈哈
 */
public interface AppSchemaRecordService extends IService<AppSchemaRecord> {

    /**
     * 记录应用创建的表
     *
     * @param appId     应用ID
     * @param tableName 表名（含前缀）
     */
    void recordTable(Long appId, String tableName);

    /**
     * 获取应用的所有表记录
     *
     * @param appId 应用ID
     * @return 表记录列表
     */
    List<AppSchemaRecord> getByAppId(Long appId);

    /**
     * 删除应用的所有表记录（数据库表也会被 DROP）
     *
     * @param appId 应用ID
     */
    void dropAndRemoveByAppId(Long appId);
}
