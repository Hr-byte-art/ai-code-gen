package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 英文词典查询工具
 * 使用 Free Dictionary API（无需认证）
 */
@Slf4j
@Component
public class DictionaryLookupTool extends PublicApiTool {

    private static final String API = "https://api.dictionaryapi.dev/api/v2/entries/en/";

    @Tool("查询英文单词的释义、音标、词性和例句。适用于英语学习、词典类应用。")
    public String lookup(@P("英文单词") String word) {
        setupMonitorContext();

        if (word == null || word.isBlank()) {
            return "请输入要查询的单词";
        }

        String trimmedWord = word.trim().toLowerCase();
        String responseBody = doGetWithCache(API + trimmedWord);

        if (responseBody == null) {
            return "查询失败，请稍后重试";
        }

        try {
            JSONArray arr = JSONUtil.parseArray(responseBody);
            if (arr.isEmpty()) {
                return "未找到单词「" + trimmedWord + "」的释义";
            }

            JSONObject entry = arr.getJSONObject(0);
            StringBuilder sb = new StringBuilder();
            sb.append("## ").append(entry.getStr("word", trimmedWord)).append("\n\n");

            // 音标
            JSONArray phonetics = entry.getJSONArray("phonetics");
            if (phonetics != null) {
                for (int i = 0; i < phonetics.size(); i++) {
                    JSONObject phonetic = phonetics.getJSONObject(i);
                    String text = phonetic.getStr("text");
                    if (text != null && !text.isBlank()) {
                        sb.append("音标: ").append(text).append("\n");
                        break;
                    }
                }
            }
            sb.append("\n");

            // 释义
            JSONArray meanings = entry.getJSONArray("meanings");
            if (meanings != null) {
                for (int i = 0; i < meanings.size(); i++) {
                    JSONObject meaning = meanings.getJSONObject(i);
                    String partOfSpeech = meaning.getStr("partOfSpeech", "");
                    sb.append("**").append(partOfSpeech).append("**\n");

                    JSONArray definitions = meaning.getJSONArray("definitions");
                    if (definitions != null) {
                        int limit = Math.min(3, definitions.size());
                        for (int j = 0; j < limit; j++) {
                            JSONObject def = definitions.getJSONObject(j);
                            sb.append(j + 1).append(". ").append(def.getStr("definition", "")).append("\n");
                            String example = def.getStr("example");
                            if (example != null && !example.isBlank()) {
                                sb.append("   例: ").append(example).append("\n");
                            }
                        }
                    }
                    sb.append("\n");
                }
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.error("解析词典响应失败: word={}, error={}", trimmedWord, e.getMessage());
            return "解析词典数据失败";
        }
    }

    @Override
    public String getToolName() {
        return "dictionaryLookup";
    }

    @Override
    public String getDisplayName() {
        return "词典查询";
    }

    @Override
    public String generateToolExecutedResult(cn.hutool.json.JSONObject arguments) {
        String word = arguments.getStr("word", "");
        return String.format("✅ %s → `%s`", getDisplayName(), word);
    }
}
