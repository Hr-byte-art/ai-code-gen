package com.wjh.aicodegen.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 抽象代码文件保存器 - 模板方法模式
 *
 * @author 王哈哈
 */
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存根目录
    protected static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

//     //文件保存根目录
//    protected static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";



    /**
     * 模板方法：保存代码的标准流程（使用 appId）
     *
     * @param result 代码结果对象
     * @param appId  应用 ID
     * @return 保存的目录
     */
    public final File saveCode(T result, Long appId) {
        // 1. 验证输入
        validateInput(result);
        // 2. 构建正式目录和临时目录
        String baseDirPath = buildUniqueDirPath(appId);
        String stagingDirPath = buildStagingDirPath(appId);
        File baseDir = new File(baseDirPath);
        File stagingDir = new File(stagingDirPath);
        FileUtil.del(stagingDir);
        FileUtil.mkdir(stagingDir);
        try {
            // 3. 先写入临时目录，避免半截产物覆盖上一次成功版本
            saveFiles(result, stagingDirPath);
            validateSavedFiles(stagingDir);
            // 4. 原子提交：同卷 rename 优先，失败时回退复制
            FileUtil.del(baseDir);
            boolean moved = stagingDir.renameTo(baseDir);
            if (!moved) {
                FileUtil.copyContent(stagingDir, baseDir, true);
                FileUtil.del(stagingDir);
            }
            return baseDir;
        } finally {
            if (stagingDir.exists()) {
                FileUtil.del(stagingDir);
            }
        }
    }

    /**
     * 构建基于 appId 的目录路径
     *
     * @param appId 应用 ID
     * @return 目录路径
     */
    protected final String buildUniqueDir(Long appId) {
        String dirPath = buildUniqueDirPath(appId);
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    protected final String buildUniqueDirPath(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        }
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        return FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
    }

    protected final String buildStagingDirPath(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        }
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        return FILE_SAVE_ROOT_DIR + File.separator + ".staging" + File.separator + uniqueDirName;
    }


    /**
     * 模板方法：保存代码的标准流程(弃用，当前使用APPId取代雪花算法构建唯一目录)
     *
     * @param result 代码结果对象
     * @return 保存的目录
     */
    public final File saveCode(T result) {
        // 1. 验证输入
        validateInput(result);
        // 2. 构建唯一目录
        String baseDirPath = buildUniqueDir();
        // 3. 保存文件（具体实现由子类提供）
        saveFiles(result, baseDirPath);
        // 4. 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param result 代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 构建唯一目录路径(弃用，当前使用APPId取代雪花算法)
     *
     * @return 目录路径
     */
    protected final String buildUniqueDir() {
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件的工具方法
     *
     * @param dirPath  目录路径
     * @param filename 文件名
     * @param content  文件内容
     */
    protected final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 提交正式目录前校验临时目录中的实际文件。
     */
    protected void validateSavedFiles(File stagingDir) {
        // 默认不做额外校验，具体类型可覆盖
    }

    /**
     * 获取代码类型（由子类实现）
     *
     * @return 代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件的具体实现（由子类实现）
     *
     * @param result      代码结果对象
     * @param baseDirPath 基础目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
