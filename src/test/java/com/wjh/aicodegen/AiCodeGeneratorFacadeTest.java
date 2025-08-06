package com.wjh.aicodegen;

import com.wjh.aicodegen.core.AiCodeGeneratorFacade;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
@Slf4j
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("任务记录网站", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }
    @Test
    void generateAndSaveCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("请帮我制作一个 倒计时页面（生日倒计时（名字：小宝的生日倒计时），生日：农历：12月17日），希望倒计时是实时跳动的，不是刷新一次才修改一下时间,精美一点", CodeGenTypeEnum.MULTI_FILE);
//         阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        log.info("生成代码结果：{}", result);
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        log.info("生成代码结果：{}", completeContent);

    }

}
