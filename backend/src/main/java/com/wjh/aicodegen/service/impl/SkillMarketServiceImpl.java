package com.wjh.aicodegen.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.model.entity.SkillInstall;
import com.wjh.aicodegen.model.entity.SkillRating;
import com.wjh.aicodegen.service.CodeSkillService;
import com.wjh.aicodegen.service.SkillInstallService;
import com.wjh.aicodegen.service.SkillMarketService;
import com.wjh.aicodegen.service.SkillRatingService;
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
    private SkillInstallService skillInstallService;

    @Resource
    private SkillRatingService skillRatingService;

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
        if (isInstalled(skillId, userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已安装该技能");
        }

        CodeSkill sourceSkill = codeSkillService.getById(skillId);
        if (sourceSkill == null || sourceSkill.getIsPublic() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "技能不存在或未发布");
        }

        SkillInstall install = SkillInstall.builder()
                .skillId(skillId)
                .userId(userId)
                .installTime(LocalDateTime.now())
                .isDelete(0)
                .build();
        skillInstallService.save(install);

        sourceSkill.setUseCount((sourceSkill.getUseCount() == null ? 0 : sourceSkill.getUseCount()) + 1);
        codeSkillService.updateById(sourceSkill);

        log.info("技能已安装: skillId={}, userId={}", skillId, userId);
    }

    @Override
    @Transactional
    public void uninstall(Long skillId, Long userId) {
        SkillInstall install = skillInstallService.getOne(
                QueryWrapper.create()
                        .where("skill_id = ?", skillId)
                        .and("user_id = ?", userId));

        if (install == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未安装该技能");
        }

        skillInstallService.removeById(install.getId());

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

        SkillRating existing = skillRatingService.getOne(
                QueryWrapper.create()
                        .where("skill_id = ?", skillId)
                        .and("user_id = ?", userId));

        if (existing != null) {
            existing.setScore(score);
            existing.setComment(comment);
            existing.setUpdateTime(LocalDateTime.now());
            skillRatingService.updateById(existing);
        } else {
            SkillRating rating = SkillRating.builder()
                    .skillId(skillId)
                    .userId(userId)
                    .score(score)
                    .comment(comment)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDelete(0)
                    .build();
            skillRatingService.save(rating);
        }

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
        List<SkillInstall> installs = skillInstallService.list(
                QueryWrapper.create().where("user_id = ?", userId));

        List<Long> skillIds = installs.stream()
                .map(SkillInstall::getSkillId)
                .collect(Collectors.toList());

        if (skillIds.isEmpty()) return List.of();

        return codeSkillService.listByIds(skillIds);
    }

    @Override
    public boolean isInstalled(Long skillId, Long userId) {
        return skillInstallService.count(
                QueryWrapper.create()
                        .where("skill_id = ?", skillId)
                        .and("user_id = ?", userId)) > 0;
    }

    private void updateRatingAvg(Long skillId) {
        List<SkillRating> ratings = skillRatingService.list(
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
