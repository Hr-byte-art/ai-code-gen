package com.wjh.aicodegen.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * @author 木子宸
 */
@Data
@Description("生成html代码结果")
public class HtmlCodeResult {
    @Description("html代码")
    private String htmlCode;
    @Description("生成的代码描述")
    private String description;
}
