package com.wjh.aicodegen.skill;

import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.service.CodeSkillService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 启动时同步：文件 → 数据库
 * 文件是 source of truth，数据库是运行时缓存
 */
@Slf4j
@Component
public class SkillSyncService {

    @Resource
    private SkillFileLoader fileLoader;

    @Resource
    private CodeSkillService codeSkillService;

    /**
     * 应用启动后自动同步
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        try {
            List<CodeSkill> fileSkills = fileLoader.loadAll();
            if (fileSkills.isEmpty()) {
                log.info("未发现文件化 Skill，跳过同步");
                return;
            }

            List<CodeSkill> dbSkills = codeSkillService.list();

            Map<String, CodeSkill> fileMap = fileSkills.stream()
                    .collect(Collectors.toMap(CodeSkill::getSkillKey, s -> s));
            Map<String, CodeSkill> dbMap = dbSkills.stream()
                    .collect(Collectors.toMap(CodeSkill::getSkillKey, s -> s));

            int created = 0, updated = 0, skipped = 0, deleted = 0;

            // 文件中有 → INSERT 或 UPDATE
            for (var entry : fileMap.entrySet()) {
                String key = entry.getKey();
                CodeSkill fileSkill = entry.getValue();
                CodeSkill dbSkill = dbMap.get(key);

                if (dbSkill == null) {
                    // 新增
                    codeSkillService.save(fileSkill);
                    created++;
                    log.info("新增 Skill: {}", key);
                } else if (dbSkill.getContentHash() == null ||
                        !dbSkill.getContentHash().equals(fileSkill.getContentHash())) {
                    // 内容变更 → UPDATE
                    fileSkill.setId(dbSkill.getId());
                    fileSkill.setCreateTime(dbSkill.getCreateTime());
                    codeSkillService.updateById(fileSkill);
                    updated++;
                    log.info("更新 Skill: {}", key);
                } else {
                    skipped++;
                }
            }

            // DB 中有但文件中无（且 source=file）→ 软删除
            for (var entry : dbMap.entrySet()) {
                String key = entry.getKey();
                CodeSkill dbSkill = entry.getValue();
                if (!fileMap.containsKey(key) && "file".equals(dbSkill.getSource())) {
                    dbSkill.setIsDelete(1);
                    codeSkillService.updateById(dbSkill);
                    deleted++;
                    log.info("软删除 Skill (文件已移除): {}", key);
                }
            }

            // 同步完成后清除缓存
            codeSkillService.clearCache();

            log.info("Skill 同步完成: 新增={}, 更新={}, 跳过={}, 删除={}", created, updated, skipped, deleted);
        } catch (Exception e) {
            log.error("Skill 同步失败: {}", e.getMessage(), e);
        }
    }
}
