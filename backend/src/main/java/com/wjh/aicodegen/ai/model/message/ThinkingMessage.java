package com.wjh.aicodegen.ai.model.message;

import com.wjh.aicodegen.model.enums.StreamMessageTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AI 思考消息（用于展示 reasoning_content）
 * @author 王哈哈
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ThinkingMessage extends StreamMessage {

    private String data;

    public ThinkingMessage(String data) {
        super(StreamMessageTypeEnum.THINKING.getValue());
        this.data = data;
    }
}
