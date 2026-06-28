package com.wjh.aicodegen.core.saver;

import cn.hutool.core.io.FileUtil;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;

import java.io.File;

/**
 * 生成项目工作区。
 *
 * AI 工具先写 staging，校验通过后再提交到正式目录，避免半截文件污染可用版本。
 */
public final class GeneratedProjectWorkspace {

    private GeneratedProjectWorkspace() {
    }

    public static File stagingDir(String dirName) {
        return new File(AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + ".staging", dirName);
    }

    public static File finalDir(String dirName) {
        return new File(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName);
    }

    public static File resolveWritableDir(String dirName) {
        File stagingDir = stagingDir(dirName);
        File finalDir = finalDir(dirName);
        if (!stagingDir.exists() && finalDir.exists()) {
            FileUtil.copyContent(finalDir, stagingDir, true);
        }
        FileUtil.mkdir(stagingDir);
        return stagingDir;
    }

    public static void resetStaging(String dirName) {
        File stagingDir = stagingDir(dirName);
        if (stagingDir.exists()) {
            FileUtil.del(stagingDir);
        }
    }

    public static File resolveReadableDir(String dirName) {
        File stagingDir = stagingDir(dirName);
        if (stagingDir.exists()) {
            return stagingDir;
        }
        return finalDir(dirName);
    }

    public static File commit(String dirName) {
        File stagingDir = stagingDir(dirName);
        if (!stagingDir.exists() || !stagingDir.isDirectory()) {
            return finalDir(dirName);
        }
        File finalDir = finalDir(dirName);
        File backupDir = new File(finalDir.getParentFile(), ".backup" + File.separator + dirName + "-" + System.currentTimeMillis());
        boolean backupCreated = false;
        try {
            if (finalDir.exists()) {
                FileUtil.mkdir(backupDir.getParentFile());
                boolean backedUp = finalDir.renameTo(backupDir);
                if (!backedUp) {
                    FileUtil.copyContent(finalDir, backupDir, true);
                    FileUtil.del(finalDir);
                }
                backupCreated = true;
            }
            boolean moved = stagingDir.renameTo(finalDir);
            if (!moved) {
                FileUtil.copyContent(stagingDir, finalDir, true);
                FileUtil.del(stagingDir);
            }
            if (!finalDir.exists() || !finalDir.isDirectory()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交生成产物失败: " + dirName);
            }
            FileUtil.del(backupDir);
            return finalDir;
        } catch (Exception e) {
            FileUtil.del(finalDir);
            if (backupCreated && backupDir.exists()) {
                boolean restored = backupDir.renameTo(finalDir);
                if (!restored) {
                    FileUtil.copyContent(backupDir, finalDir, true);
                    FileUtil.del(backupDir);
                }
            }
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交生成产物失败: " + e.getMessage());
        }
    }
}