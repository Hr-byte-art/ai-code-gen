package com.wjh.aicodegen.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.utils.ResultUtils;
import com.wjh.aicodegen.mcp.McpTool;
import com.wjh.aicodegen.mcp.McpToolRegistry;
import com.wjh.aicodegen.model.entity.McpServer;
import com.wjh.aicodegen.service.McpServerService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * MCP 服务器管理接口
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {

    @Resource
    private McpToolRegistry mcpToolRegistry;

    @Resource
    private McpServerService mcpServerService;

    /**
     * 获取所有 MCP 服务器配置（管理员）
     */
    @GetMapping("/admin/list")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<McpServer>> listServers() {
        List<McpServer> servers = mcpServerService.list(
                QueryWrapper.create().orderBy("create_time", false));
        return ResultUtils.success(servers);
    }

    /**
     * 添加 MCP 服务器（管理员）
     */
    @PostMapping("/admin/add")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> addServer(@RequestBody McpServerRequest req) {
        McpServer server = McpServer.builder()
                .name(req.getName())
                .url(req.getUrl())
                .description(req.getDescription())
                .transport(req.getTransport() != null ? req.getTransport() : "sse")
                .isActive(1)
                .toolCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();
        mcpServerService.save(server);

        // 自动连接并发现工具
        try {
            mcpToolRegistry.registerServer(server.getUrl(), server.getName());
            int toolCount = mcpToolRegistry.getToolsByServer(server.getName()).size();
            server.setToolCount(toolCount);
            mcpServerService.updateById(server);
        } catch (Exception e) {
            log.warn("MCP 服务器连接失败: name={}, error={}", server.getName(), e.getMessage());
        }

        return ResultUtils.success(server.getId());
    }

    /**
     * 删除 MCP 服务器（管理员）
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> deleteServer(@RequestParam Long id) {
        McpServer server = mcpServerService.getById(id);
        if (server == null) return ResultUtils.success(false);

        mcpToolRegistry.unregisterServer(server.getName());
        mcpServerService.removeById(id);
        return ResultUtils.success(true);
    }

    /**
     * 刷新 MCP 服务器工具（管理员）
     */
    @PostMapping("/admin/refresh")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Integer> refreshServer(@RequestParam Long id) {
        McpServer server = mcpServerService.getById(id);
        if (server == null) return ResultUtils.success(0);

        mcpToolRegistry.refreshServer(server.getName());
        int toolCount = mcpToolRegistry.getToolsByServer(server.getName()).size();
        server.setToolCount(toolCount);
        server.setUpdateTime(LocalDateTime.now());
        mcpServerService.updateById(server);

        return ResultUtils.success(toolCount);
    }

    /**
     * 获取所有 MCP 工具（管理员）
     */
    @GetMapping("/admin/tools")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Collection<McpTool>> listTools() {
        return ResultUtils.success(mcpToolRegistry.getAllTools());
    }

    /**
     * 测试调用 MCP 工具（管理员）
     */
    @PostMapping("/admin/test-call")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<String> testCall(@RequestBody TestCallRequest req) {
        String result = mcpToolRegistry.callTool(req.getToolName(), req.getParams());
        return ResultUtils.success(result);
    }

    @Data
    static class McpServerRequest {
        private String name;
        private String url;
        private String description;
        private String transport;
    }

    @Data
    static class TestCallRequest {
        private String toolName;
        private String params;
    }
}
