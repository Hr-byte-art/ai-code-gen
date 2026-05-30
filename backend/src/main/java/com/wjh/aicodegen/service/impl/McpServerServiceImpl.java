package com.wjh.aicodegen.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.McpServerMapper;
import com.wjh.aicodegen.model.entity.McpServer;
import com.wjh.aicodegen.service.McpServerService;
import org.springframework.stereotype.Service;

@Service
public class McpServerServiceImpl extends ServiceImpl<McpServerMapper, McpServer> implements McpServerService {
}
