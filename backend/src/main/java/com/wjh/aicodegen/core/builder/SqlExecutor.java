package com.wjh.aicodegen.core.builder;

import com.wjh.aicodegen.service.AppSchemaRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 执行器
 * 解析 AI 生成的 schema.sql，自动补表名前缀后执行
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class SqlExecutor {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Lazy
    @Resource
    private AppSchemaRecordService appSchemaRecordService;

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "(CREATE\\s+TABLE\\s+)(IF\\s+NOT\\s+EXISTS\\s+)?(`?\\w+`?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> ALLOWED_STATEMENT_PREFIXES = List.of(
            "CREATE TABLE",
            "CREATE INDEX",
            "CREATE UNIQUE INDEX",
            "ALTER TABLE",
            "INSERT INTO",
            "DROP TABLE",
            "SET "
    );

    /**
     * 执行 schema.sql 文件
     *
     * @param schemaPath schema.sql 文件路径
     * @param tablePrefix 表名前缀，如 "project_123_"
     * @param appId 应用ID（用于记录 schema）
     * @return 创建的表名列表
     */
    public List<String> executeSchema(Path schemaPath, String tablePrefix, Long appId) throws IOException {
        String sql = Files.readString(schemaPath);
        List<String> tables = executeSql(sql, tablePrefix);
        // 记录到 schema_record
        for (String table : tables) {
            appSchemaRecordService.recordTable(appId, tablePrefix + table);
        }
        return tables;
    }

    /**
     * 执行 SQL 内容，自动补表名前缀
     *
     * @param sql SQL 内容
     * @param tablePrefix 表名前缀
     * @return 创建的表名列表
     */
    public List<String> executeSql(String sql, String tablePrefix) {
        List<String> createdTables = new ArrayList<>();

        List<String> statements = splitSqlStatements(sql);

        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!isExecutableSchemaStatement(trimmed)) {
                log.warn("跳过非 schema SQL 片段: {}", previewSql(trimmed));
                continue;
            }

            // 替换表名：在 CREATE TABLE 后面加上前缀
            String processed = addTablePrefix(trimmed, tablePrefix);

            // 提取表名用于记录
            Matcher matcher = CREATE_TABLE_PATTERN.matcher(processed);
            if (matcher.find()) {
                String tableName = matcher.group(3).replace("`", "");
                createdTables.add(tableName);
            }

            try {
                jdbcTemplate.execute(processed);
                log.info("SQL 执行成功: {}", previewSql(processed));
            } catch (Exception e) {
                log.error("SQL 执行失败: {}, 错误: {}", previewSql(processed), e.getMessage());
                throw new RuntimeException("SQL 执行失败: " + e.getMessage(), e);
            }
        }

        return createdTables;
    }

    /**
     * 给 CREATE TABLE 语句的表名添加前缀
     */
    private String addTablePrefix(String sql, String tablePrefix) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(3).replace("`", "");
            // 如果表名已经有前缀，不再添加
            if (tableName.startsWith(tablePrefix)) {
                return sql;
            }
            String newTableName = tablePrefix + tableName;
            // 保留反引号（如果有）
            if (matcher.group(3).startsWith("`")) {
                newTableName = "`" + newTableName + "`";
            }
            return matcher.replaceAll("$1$2" + newTableName);
        }
        return sql;
    }

    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                current.append(c);
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inBacktick) {
                if (c == '-' && next == '-') {
                    current.append(c).append(next);
                    i++;
                    inLineComment = true;
                    continue;
                }
                if (c == '#') {
                    current.append(c);
                    inLineComment = true;
                    continue;
                }
                if (c == '/' && next == '*') {
                    current.append(c).append(next);
                    i++;
                    inBlockComment = true;
                    continue;
                }
                if (c == ';') {
                    addStatement(statements, current);
                    continue;
                }
            }

            current.append(c);
            if (c == '\'' && !inDoubleQuote && !inBacktick && !isEscaped(sql, i)) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote && !inBacktick && !isEscaped(sql, i)) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }
        }
        addStatement(statements, current);
        return statements;
    }

    private void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }

    private boolean isEscaped(String text, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    private boolean isExecutableSchemaStatement(String sql) {
        String normalized = stripLeadingComments(sql).trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return ALLOWED_STATEMENT_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    private String stripLeadingComments(String sql) {
        String remaining = sql.stripLeading();
        boolean changed;
        do {
            changed = false;
            if (remaining.startsWith("--")) {
                int lineEnd = findLineEnd(remaining);
                remaining = lineEnd < 0 ? "" : remaining.substring(lineEnd).stripLeading();
                changed = true;
            } else if (remaining.startsWith("#")) {
                int lineEnd = findLineEnd(remaining);
                remaining = lineEnd < 0 ? "" : remaining.substring(lineEnd).stripLeading();
                changed = true;
            } else if (remaining.startsWith("/*")) {
                int commentEnd = remaining.indexOf("*/");
                remaining = commentEnd < 0 ? "" : remaining.substring(commentEnd + 2).stripLeading();
                changed = true;
            }
        } while (changed);
        return remaining;
    }

    private int findLineEnd(String text) {
        int lf = text.indexOf('\n');
        int cr = text.indexOf('\r');
        if (lf < 0) {
            return cr;
        }
        if (cr < 0) {
            return lf;
        }
        return Math.min(lf, cr);
    }

    private String previewSql(String sql) {
        String normalized = sql.replaceAll("\\s+", " ").trim();
        return normalized.substring(0, Math.min(100, normalized.length()));
    }

    /**
     * 删除项目的所有表
     *
     * @param tablePrefix 表名前缀
     * @param tableNames 要删除的表名列表
     */
    public void dropTables(String tablePrefix, List<String> tableNames) {
        for (String tableName : tableNames) {
            try {
                String fullTableName = tablePrefix + tableName;
                jdbcTemplate.execute("DROP TABLE IF EXISTS `" + fullTableName + "`");
                log.info("删除表成功: {}", fullTableName);
            } catch (Exception e) {
                log.error("删除表失败: {}, 错误: {}", tableName, e.getMessage());
            }
        }
    }
}
