package com.wjh.aicodegen.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.SkillInstallMapper;
import com.wjh.aicodegen.model.entity.SkillInstall;
import com.wjh.aicodegen.service.SkillInstallService;
import org.springframework.stereotype.Service;

@Service
public class SkillInstallServiceImpl extends ServiceImpl<SkillInstallMapper, SkillInstall> implements SkillInstallService {
}
