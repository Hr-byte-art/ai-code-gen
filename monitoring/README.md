# 监控配置

## Prometheus 指标

应用已内置以下自定义指标（通过 `/api/actuator/prometheus` 暴露）：

### Agent 指标
| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `agent_execution_total` | Counter | agent_name, status | Agent 执行总次数 |
| `agent_execution_duration_seconds` | Timer | agent_name | Agent 执行耗时（含 P50/P95/P99） |
| `agent_token_usage_total` | Counter | agent_name, token_type | Agent Token 消耗 |

### Skill 指标
| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `skill_usage_total` | Counter | skill_key | Skill 使用次数 |
| `skill_generation_duration_seconds` | Timer | skill_key | Skill 代码生成耗时 |

### 工具调用指标
| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `tool_call_total` | Counter | tool_name, status | 工具调用次数 |
| `tool_call_duration_seconds` | Timer | tool_name | 工具调用耗时 |

### 应用指标
| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `app_deploy_total` | Counter | code_gen_type, status | 应用部署次数 |
| `code_generation_total` | Counter | code_gen_type, status | 代码生成次数 |

## Grafana 面板

### 导入步骤

1. 打开 Grafana → Dashboards → Import
2. 上传 `grafana-dashboard.json`
3. 选择 Prometheus 数据源
4. 点击 Import

### 面板包含

- **概览统计**：代码生成/Agent执行/部署/工具调用总次数
- **Agent 执行**：耗时分布、按状态的执行次数
- **Skill 使用**：使用分布饼图、生成耗时、按类型统计
- **工具调用**：Top 10 调用次数、Top 10 耗时
- **系统监控**：JVM 内存、HTTP 请求速率
- **Token 消耗**：按 Agent 的 Token 使用趋势

## 告警规则（参考）

```yaml
# Prometheus alerting rules
groups:
  - name: ai-code-gen
    rules:
      # Agent 执行失败率过高
      - alert: AgentExecutionHighErrorRate
        expr: rate(agent_execution_total{status="error"}[5m]) / rate(agent_execution_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Agent 执行错误率超过 10%"

      # 代码生成失败
      - alert: CodeGenerationFailed
        expr: increase(code_generation_total{status="error"}[5m]) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "代码生成失败"

      # Agent 执行耗时过长
      - alert: AgentExecutionSlow
        expr: histogram_quantile(0.95, rate(agent_execution_duration_seconds_bucket[5m])) > 60
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Agent P95 执行耗时超过 60 秒"

      # JVM 内存使用率过高
      - alert: HighJvmMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM 堆内存使用率超过 85%"
```
