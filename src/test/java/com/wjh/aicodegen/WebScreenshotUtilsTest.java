package com.wjh.aicodegen;

import cn.hutool.core.img.ImgUtil;
import com.wjh.aicodegen.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@Slf4j
@SpringBootTest
public class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String testUrl = "https://www.douyin.com/?recommend=1";
         String webPageScreenshot = WebScreenshotUtils.saveWebPageScreenshot(testUrl);
        Assertions.assertNotNull(webPageScreenshot);
    }

    @Test
    void compressPictures() {
        File file = new File("D:/桌面/微信图片_20250814102407_5.jpg");
        File file1 = new File("D:/桌面/微信图片_20250814102407_5_compress.jpg");
        ImgUtil.compress(file , file1 , 0.5f);
    }
}
