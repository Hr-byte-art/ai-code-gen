package com.wjh.aicodegen;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.wjh.aicodegen.utils.WebScreenshotUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        ImgUtil.compress(file, file1, 0.5f);
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

    @Test
    void test1() {
        String mermaid = "flowchart TD\n" +
                "\t__START__((start))\n" +
                "\t__END__((stop))\n" +
                "\timage_collector(\"image_collector\")\n" +
                "\tprompt_enhancer(\"prompt_enhancer\")\n" +
                "\trouter(\"router\")\n" +
                "\tcode_generator(\"code_generator\")\n" +
                "\tproject_builder(\"project_builder\")\n" +
                "\t__START__:::__START__ --> image_collector:::image_collector\n" +
                "\timage_collector:::image_collector --> prompt_enhancer:::prompt_enhancer\n" +
                "\tprompt_enhancer:::prompt_enhancer --> router:::router\n" +
                "\trouter:::router --> code_generator:::code_generator\n" +
                "\tcode_generator:::code_generator --> project_builder:::project_builder\n" +
                "\tproject_builder:::project_builder --> __END__:::__END__\n" +
                "\n" +
                "\tclassDef ___START__ fill:black,stroke-width:1px,font-size:xx-small;\n" +
                "\tclassDef ___END__ fill:black,stroke-width:1px,font-size:xx-small;";
        byte[] bytes = mermaid.getBytes();
        byte[] zlib = ZipUtil.zlib(bytes, 9);
        String base64 = Base64.encode(zlib);
        base64 = StrUtil.replace(base64, "+", "-");
        base64 = StrUtil.replace(base64, "/", "_");

        // 直接请求 PNG 格式
        HttpResponse execute = HttpUtil.createGet("https://kroki.io/mermaid/png/" + base64).execute();

        try {
            String outPutPath1 = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "mermaid" + File.separator + "1.png";
            FileUtil.writeBytes(execute.bodyBytes(), outPutPath1);
            log.info("PNG saved to: {}", outPutPath1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}