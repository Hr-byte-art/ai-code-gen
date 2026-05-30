package com.wjh.aicodegen.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.mapper.SkillInstallMapper;
import com.wjh.aicodegen.mapper.SkillRatingMapper;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.model.entity.SkillInstall;
import com.wjh.aicodegen.model.entity.SkillRating;
import com.wjh.aicodegen.service.CodeSkillService;
import com.wjh.aicodegen.service.SkillMarketService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SkillMarketServiceImpl implements SkillMarketService {

    @Resource
    private CodeSkillService codeSkillService;

    @Resource
    private SkillInstallMapper skillInstallMapper;

    @Resource
    private SkillRatingMapper skillRatingMapper;

    @Override
    @Transactional
    public void publish(Long skillId, Long userId) {
        CodeSkill skill = codeSkillService.getById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "技能不存在");
        if (!skill.getAuthorId().equals(userId)) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能发布自己创建的技能");

        skill.setIsPublic(1);
        codeSkillService.updateById(skill);
        log.info("技能已发布到市场: skillId={}, userId={}", skillId, userId);
    }

    @Override
    @Transactional
    public void unpublish(Long skillId, Long userId) {
        CodeSkill skill = codeSkillService.getById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "技能不存在");
        if (!skill.getAuthorId().equals(userId)) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能取消自己发布的技能");

        skill.setIsPublic(0);
        codeSkillService.updateById(skill);
        log.info("技能已从市场下架: skillId={}, userId={}", skillId, userId);
    }

    @Override
    @Transactional
    public void install(Long skillId, Long userId) {
        // 检查是否已安装
        if (isInstalled(skillId, userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已安装该技能");
        }

        CodeSkill sourceSkill = codeSkillService.getById(skillId);
        if (sourceSkill == null || sourceSkill.getIsPublic() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "技能不存在或未发布");
        }

        // 创建安装记录
        SkillInstall install = SkillInstall.builder()
                .skillId(skillId)
                .userId(userId)
                .installTime(LocalDateTime.now())
                .isDelete(0)
                .build();
        skillInstallMapper.insert(install);

        // 增加使用次数
        sourceSkill.setUseCount((sourceSkill.getUseCount() == null ? 0 : sourceSkill.getUseCount()) + 1);
        codeSkillService.updateById(sourceSkill);

        log.info("技能已安装: skillId={}, userId={}", skillId, userId);
    }

    @Override
    @Transactional
    public void uninstall(Long skillId, Long userId) {
        SkillInstall install = skillInstallMapper.selectOne(
                QueryWrapper.create()
                        .where("skill_id = ?", skillId)
                        .and("user_id = ?", userId));

        if (install == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未安装该技能");
        }

        skillInstallMapper.deleteById(install.getId());

        // 减少使用次数
        CodeSkill skill = codeSkillService.getById(skillId);
        if (skill != null && skill.getUseCount() != null && skill.getUseCount() > 0) {
            skill.setUseCount(skill.getUseCount() - 1);
            codeSkillService.updateById(skill);
        }

        log.info("技能已卸载: skillId={}, userId={}", skillId, userId);
    }

    @Override
    @Transactional
    public void rate(Long skillId, Long userId, int score, String comment) {
        if (score < 1 || score > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分范围为 1-5");
        }

        CodeSkill skill = codeSkillService.getById(skillId);
        if (skill == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "技能不存在");

        // 检查是否已评分
        SkillRating existing = skillRatingMapper.selectOne(
                QueryWrapper.create()
                        .where("skill_id = ?", skillId)
                        .and("user_id = ?", userId));

        if (existing != null) {
            // 更新评分
            existing.setScore(score);
            existing.setComment(comment);
            existing.setUpdateTime(LocalDateTime.now());
            skillRatingMapper.updateById(existing);
        } else {
            // 新增评分
            SkillRating rating = SkillRating.builder()
                    .skillId(skillId)
                    .userId(userId)
                    .score(score)
                    .comment(comment)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDelete(0)
                    .build();
            skillRatingMapper.insert(rating);
        }

        // 更新平均评分
        updateRatingAvg(skillId);

        log.info("技能已评分: skillId={}, userId={}, score={}", skillId, userId, score);
    }

    @Override
    public List<CodeSkill> listPublicSkills() {
        return codeSkillService.list(QueryWrapper.create()
                .where("is_public = ?", 1)
                .and("is_active = ?", 1)
                .orderBy("use_count", false));
    }

    @Override
    public List<CodeSkill> getInstalledSkills(Long userId) {
        List<SkillInstall> installs = skillInstallMapper.selectList(
                QueryWrapper.create().where("user_id = ?", userId));

        List<Long> skillIds = installs.stream()
                .map(SkillInstall::getSkillId)
                .collect(Collectors.toList());

        if (skillIds.isEmpty()) return List.of();

        return codeSkillService.listByIds(skillIds);
    }

    @Override
    public boolean isInstalled(Long skillId, Long userId) {
        return skillInstallMapper.selectCount(
                QueryWrapper.create()
                        .where("skill_id = ?", skillId)
                        .and("user_id = ?", userId)) > 0;
    }

    private void updateRatingAvg(Long skillId) {
        List<SkillRating> ratings = skillRatingMapper.selectList(
                QueryWrapper.create().where("skill_id = ?", skillId));

        if (ratings.isEmpty()) return;

        double avg = ratings.stream().mapToInt(SkillRating::getScore).average().orElse(0);
        CodeSkill skill = codeSkillService.getById(skillId);
        if (skill != null) {
            skill.setRatingAvg(Math.round(avg * 10) / 10.0);
            skill.setRatingCount(ratings.size());
            codeSkillService.updateById(skill);
        }
    }
}
