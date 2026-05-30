package com.wjh.aicodegen.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.SkillRatingMapper;
import com.wjh.aicodegen.model.entity.SkillRating;
import com.wjh.aicodegen.service.SkillRatingService;
import org.springframework.stereotype.Service;

@Service
public class SkillRatingServiceImpl extends ServiceImpl<SkillRatingMapper, SkillRating> implements SkillRatingService {
}
