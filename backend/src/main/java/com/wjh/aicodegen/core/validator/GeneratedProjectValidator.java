package com.wjh.aicodegen.core.validator;

import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;

/**
 * 生成产物完整性校验器。
 *
 * 职责只判断“产物是否能进入下一阶段”，不负责修复、不负责保存。
 */
@Slf4j
@Component
public class GeneratedProjectValidator {

    public void validateStaticProject(File projectDir, CodeGenTypeEnum codeGenType) {
        requireDirectory(projectDir, "生成目录不存在");
        if (CodeGenTypeEnum.HTML.equals(codeGenType)) {
            validateHtmlProject(projectDir);
            return;
        }
        if (CodeGenTypeEnum.MULTI_FILE.equals(codeGenType)) {
            validateMultiFileProject(projectDir);
            return;
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的静态产物类型: " + codeGenType);
    }

    public void validateBuildProject(File projectDir, String buildStrategy) {
        requireDirectory(projectDir, "生成目录不存在");
        if (buildStrategy == null || buildStrategy.isBlank() || "none".equals(buildStrategy)) {
            return;
        }
        switch (buildStrategy) {
            case "vue" -> validateVueProject(projectDir);
            case "react" -> validateReactProject(projectDir);
            case "nextjs" -> validateNextProject(projectDir);
            case "fullstack" -> validateFullstackProject(projectDir);
            default -> log.info("构建策略 {} 暂无专用校验，跳过结构校验", buildStrategy);
        }
    }

    private void validateHtmlProject(File projectDir) {
        File index = requireFile(projectDir, "index.html");
        String html = readLower(index);
        requireTrue(html.contains("<!doctype html") || html.contains("<html"), "index.html 缺少 HTML 文档结构");
        requireTrue(html.contains("</html>"), "index.html 未闭合 html 标签");
        requireTrue(html.contains("<head") && html.contains("</head>"), "index.html head 不完整");
        requireTrue(html.contains("<body") && html.contains("</body>"), "index.html body 不完整");
        requireTrue(!html.contains("```"), "index.html 包含 Markdown 代码块标记");
    }

    private void validateMultiFileProject(File projectDir) {
        File index = requireFile(projectDir, "index.html");
        File css = requireFile(projectDir, "style.css");
        File js = requireFile(projectDir, "script.js");
        validateHtmlProject(projectDir);
        String html = readLower(index);
        requireTrue(html.contains("style.css"), "index.html 未引用 style.css");
        requireTrue(html.contains("script.js"), "index.html 未引用 script.js");
        requireTrue(!readLower(css).contains("```"), "style.css 包含 Markdown 代码块标记");
        requireTrue(!readLower(js).contains("```"), "script.js 包含 Markdown 代码块标记");
    }

    private void validateVueProject(File projectDir) {
        requireFile(projectDir, "package.json");
        requireFile(projectDir, "index.html");
        requireAnyFile(projectDir, "src/main.ts", "src/main.js");
        requireAnyFile(projectDir, "src/App.vue", "src/app.vue");
        String packageJson = readLower(new File(projectDir, "package.json"));
        requireTrue(packageJson.contains("vue"), "package.json 缺少 vue 依赖");
        requireTrue(packageJson.contains("vite"), "package.json 缺少 vite 依赖");
    }

    private void validateReactProject(File projectDir) {
        requireFile(projectDir, "package.json");
        requireFile(projectDir, "index.html");
        requireAnyFile(projectDir, "src/main.tsx", "src/main.jsx", "src/main.ts", "src/main.js");
        requireAnyFile(projectDir, "src/App.tsx", "src/App.jsx", "src/App.ts", "src/App.js");
        String packageJson = readLower(new File(projectDir, "package.json"));
        requireTrue(packageJson.contains("react"), "package.json 缺少 react 依赖");
        requireTrue(packageJson.contains("vite"), "package.json 缺少 vite 依赖");
    }

    private void validateNextProject(File projectDir) {
        requireFile(projectDir, "package.json");
        requireAnyFile(projectDir, "app/page.tsx", "app/page.jsx", "src/app/page.tsx", "src/app/page.jsx",
                "pages/index.tsx", "pages/index.jsx", "src/pages/index.tsx", "src/pages/index.jsx");
        String packageJson = readLower(new File(projectDir, "package.json"));
        requireTrue(packageJson.contains("next"), "package.json 缺少 next 依赖");
    }

    private void validateFullstackProject(File projectDir) {
        File frontendDir = requireDirectory(new File(projectDir, "frontend"), "全栈项目缺少 frontend 目录");
        File serverDir = requireDirectory(new File(projectDir, "server"), "全栈项目缺少 server 目录");
        validateVueProject(frontendDir);
        requireFile(serverDir, "package.json");
        requireAnyFile(serverDir, "index.js", "index.ts", "src/index.js", "src/index.ts");
        String packageJson = readLower(new File(serverDir, "package.json"));
        requireTrue(packageJson.contains("express"), "后端 package.json 缺少 express 依赖");
    }

    private File requireDirectory(File dir, String message) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, message);
        }
        return dir;
    }

    private File requireFile(File baseDir, String relativePath) {
        File file = new File(baseDir, relativePath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缺少必要文件: " + relativePath);
        }
        return file;
    }

    private void requireAnyFile(File baseDir, String... relativePaths) {
        for (String relativePath : relativePaths) {
            File file = new File(baseDir, relativePath);
            if (file.exists() && file.isFile()) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缺少必要文件: " + String.join(", ", relativePaths));
    }

    private void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, message);
        }
    }

    private String readLower(File file) {
        try {
            return Files.readString(file.toPath()).toLowerCase();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败: " + file.getName());
        }
    }
}