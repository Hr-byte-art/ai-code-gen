package com.wjh.aicodegen;

import cn.hutool.core.img.ImgUtil;
import com.wjh.aicodegen.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    @Test
    void test() {
        Path path = Paths.get("D:/桌面");
        String string = path.getFileName().toString();

        String string1 = Paths.get(System.getProperty("user.dir")).toString();
        log.info(string1);

        Path path1 = Paths.get(string1);
        Path test = path.resolve("test");
        log.info(test.toString());
    }
}
