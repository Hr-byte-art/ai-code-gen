package com.wjh.aicodegen.core.saver;

import com.wjh.aicodegen.ai.model.HtmlCodeResult;
import com.wjh.aicodegen.ai.model.MultiFileCodeResult;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存执行器
 * 根据代码生成类型执行相应的保存逻辑
 *
 * @author yupi
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate HTML_CODE_FILE_SAVER = new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate MULTI_FILE_CODE_FILE_SAVER = new MultiFileCodeFileSaverTemplate();

    /**
     * 执行代码保存（使用 appId）
     *
     * @param codeResult  代码结果对象
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 保存的目录
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
        return switch (codeGenType) {
            case HTML -> HTML_CODE_FILE_SAVER.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> MULTI_FILE_CODE_FILE_SAVER.saveCode((MultiFileCodeResult) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }


    /**
     * 执行代码保存（弃用，使用AppId代替原有的雪花算法构建唯一路径）
     *
     * @param codeResult  代码结果对象
     * @param codeGenType 代码生成类型
     * @return 保存的目录
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML -> HTML_CODE_FILE_SAVER.saveCode((HtmlCodeResult) codeResult);
            case MULTI_FILE -> MULTI_FILE_CODE_FILE_SAVER.saveCode((MultiFileCodeResult) codeResult);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }
}
