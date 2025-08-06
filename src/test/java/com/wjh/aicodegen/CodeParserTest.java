package com.wjh.aicodegen;

import com.wjh.aicodegen.ai.model.HtmlCodeResult;
import com.wjh.aicodegen.ai.model.MultiFileCodeResult;
import com.wjh.aicodegen.core.CodeParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CodeParserTest {

    @Test
    void parseHtmlCode() {
        String codeContent = """
                随便写一段描述：
                ```html
       <!DOCTYPE html>
                <html>
                <head>
                    <title>测试页面</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>
                ```
                随便写一段描述
                """;
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        System.out.println(result.getHtmlCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = "## 文件结构\n" +
                "\n" +
                "```html\n" +
                "<!-- index.html -->\n" +
                "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>新年倒计时</title>\n" +
                "    <link rel=\"stylesheet\" href=\"style.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <header>\n" +
                "            <h1>新年倒计时</h1>\n" +
                "            <p>距离2025年元旦还有</p>\n" +
                "        </header>\n" +
                "        \n" +
                "        <div class=\"countdown\">\n" +
                "            <div class=\"time-box\">\n" +
                "                <span id=\"days\">00</span>\n" +
                "                <span class=\"label\">天</span>\n" +
                "            </div>\n" +
                "            <div class=\"time-box\">\n" +
                "                <span id=\"hours\">00</span>\n" +
                "                <span class=\"label\">小时</span>\n" +
                "            </div>\n" +
                "            <div class=\"time-box\">\n" +
                "                <span id=\"minutes\">00</span>\n" +
                "                <span class=\"label\">分钟</span>\n" +
                "            </div>\n" +
                "            <div class=\"time-box\">\n" +
                "                <span id=\"seconds\">00</span>\n" +
                "                <span class=\"label\">秒</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"fireworks\">\n" +
                "            <div class=\"firework\"></div>\n" +
                "            <div class=\"firework\"></div>\n" +
                "            <div class=\"firework\"></div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script src=\"script.js\"></script>\n" +
                "</body>\n" +
                "</html>\n" +
                "```\n" +
                "\n" +
                "```css\n" +
                "/* style.css */\n" +
                "/* 基础样式重置 */\n" +
                "* {\n" +
                "    margin: 0;\n" +
                "    padding: 0;\n" +
                "    box-sizing: border-box;\n" +
                "}\n" +
                "\n" +
                "body {\n" +
                "    font-family: 'Arial', sans-serif;\n" +
                "    background: linear-gradient(135deg, #1a1a2e, #16213e);\n" +
                "    color: #fff;\n" +
                "    min-height: 100vh;\n" +
                "    display: flex;\n" +
                "    justify-content: center;\n" +
                "    align-items: center;\n" +
                "    overflow: hidden;\n" +
                "}\n" +
                "\n" +
                ".container {\n" +
                "    text-align: center;\n" +
                "    padding: 2rem;\n" +
                "    position: relative;\n" +
                "    z-index: 1;\n" +
                "}\n" +
                "\n" +
                "header h1 {\n" +
                "    font-size: 2.5rem;\n" +
                "    margin-bottom: 0.5rem;\n" +
                "    text-shadow: 0 0 10px rgba(255, 255, 255, 0.5);\n" +
                "}\n" +
                "\n" +
                "header p {\n" +
                "    font-size: 1.2rem;\n" +
                "    margin-bottom: 2rem;\n" +
                "    opacity: 0.8;\n" +
                "}\n" +
                "\n" +
                ".countdown {\n" +
                "    display: flex;\n" +
                "    justify-content: center;\n" +
                "    gap: 1.5rem;\n" +
                "    margin-bottom: 2rem;\n" +
                "}\n" +
                "\n" +
                ".time-box {\n" +
                "    background: rgba(255, 255, 255, 0.1);\n" +
                "    border-radius: 10px;\n" +
                "    padding: 1.5rem 1rem;\n" +
                "    min-width: 100px;\n" +
                "    backdrop-filter: blur(5px);\n" +
                "    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);\n" +
                "}\n" +
                "\n" +
                ".time-box span {\n" +
                "    display: block;\n" +
                "}\n" +
                "\n" +
                ".time-box span:first-child {\n" +
                "    font-size: 3rem;\n" +
                "    font-weight: bold;\n" +
                "    margin-bottom: 0.5rem;\n" +
                "}\n" +
                "\n" +
                ".time-box .label {\n" +
                "    font-size: 1rem;\n" +
                "    opacity: 0.8;\n" +
                "}\n" +
                "\n" +
                "/* 烟花动画 */\n" +
                ".firework {\n" +
                "    position: absolute;\n" +
                "    width: 5px;\n" +
                "    height: 5px;\n" +
                "    border-radius: 50%;\n" +
                "    box-shadow: 0 0 10px 5px rgba(255, 255, 255, 0.8);\n" +
                "    animation: fireworks 2s ease-out infinite;\n" +
                "    opacity: 0;\n" +
                "}\n" +
                "\n" +
                ".firework:nth-child(1) {\n" +
                "    top: 20%;\n" +
                "    left: 20%;\n" +
                "    animation-delay: 0.5s;\n" +
                "}\n" +
                "\n" +
                ".firework:nth-child(2) {\n" +
                "    top: 60%;\n" +
                "    left: 70%;\n" +
                "    animation-delay: 1.2s;\n" +
                "}\n" +
                "\n" +
                ".firework:nth-child(3) {\n" +
                "    top: 30%;\n" +
                "    left: 50%;\n" +
                "    animation-delay: 1.8s;\n" +
                "}\n" +
                "\n" +
                "@keyframes fireworks {\n" +
                "    0% {\n" +
                "        transform: translateY(0) scale(0);\n" +
                "        opacity: 1;\n" +
                "    }\n" +
                "    100% {\n" +
                "        transform: translateY(-100px) scale(1.5);\n" +
                "        opacity: 0;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "/* 响应式设计 */\n" +
                "@media (max-width: 768px) {\n" +
                "    .countdown {\n" +
                "        flex-wrap: wrap;\n" +
                "        gap: 1rem;\n" +
                "    }\n" +
                "    \n" +
                "    .time-box {\n" +
                "        min-width: 80px;\n" +
                "        padding: 1rem 0.5rem;\n" +
                "    }\n" +
                "    \n" +
                "    .time-box span:first-child {\n" +
                "        font-size: 2rem;\n" +
                "    }\n" +
                "    \n" +
                "    header h1 {\n" +
                "        font-size: 2rem;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "@media (max-width: 480px) {\n" +
                "    .countdown {\n" +
                "        gap: 0.5rem;\n" +
                "    }\n" +
                "    \n" +
                "    .time-box {\n" +
                "        min-width: 70px;\n" +
                "        padding: 0.8rem 0.3rem;\n" +
                "    }\n" +
                "    \n" +
                "    .time-box span:first-child {\n" +
                "        font-size: 1.5rem;\n" +
                "    }\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "```javascript\n" +
                "// script.js\n" +
                "document.addEventListener('DOMContentLoaded', function() {\n" +
                "    // 设置目标日期 - 2025年元旦\n" +
                "    const targetDate = new Date('January 1, 2025 00:00:00').getTime();\n" +
                "    \n" +
                "    // 更新倒计时\n" +
                "    function updateCountdown() {\n" +
                "        const now = new Date().getTime();\n" +
                "        const distance = targetDate - now;\n" +
                "        \n" +
                "        // 计算天、小时、分钟、秒\n" +
                "        const days = Math.floor(distance / (1000 * 60 * 60 * 24));\n" +
                "        const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));\n" +
                "        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));\n" +
                "        const seconds = Math.floor((distance % (1000 * 60)) / 1000);\n" +
                "        \n" +
                "        // 更新显示\n" +
                "        document.getElementById('days').textContent = formatTime(days);\n" +
                "        document.getElementById('hours').textContent = formatTime(hours);\n" +
                "        document.getElementById('minutes').textContent = formatTime(minutes);\n" +
                "        document.getElementById('seconds').textContent = formatTime(seconds);\n" +
                "        \n" +
                "        // 如果倒计时结束\n" +
                "        if (distance < 0) {\n" +
                "            clearInterval(countdownInterval);\n" +
                "            document.querySelector('header h1').textContent = '新年快乐！';\n" +
                "            document.querySelector('header p').textContent = '2025年元旦已到来！';\n" +
                "            document.querySelector('.countdown').style.display = 'none';\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    // 格式化时间显示，确保两位数\n" +
                "    function formatTime(time) {\n" +
                "        return time < 10 ? `0${time}` : time;\n" +
                "    }\n" +
                "    \n" +
                "    // 立即执行一次，避免初始延迟\n" +
                "    updateCountdown();\n" +
                "    \n" +
                "    // 每秒更新一次\n" +
                "    const countdownInterval = setInterval(updateCountdown, 1000);\n" +
                "    \n" +
                "    // 添加点击烟花效果\n" +
                "    document.addEventListener('click', function(e) {\n" +
                "        createFirework(e.clientX, e.clientY);\n" +
                "    });\n" +
                "    \n" +
                "    function createFirework(x, y) {\n" +
                "        const firework = document.createElement('div');\n" +
                "        firework.className = 'firework';\n" +
                "        firework.style.left = x + 'px';\n" +
                "        firework.style.top = y + 'px';\n" +
                "        document.body.appendChild(firework);\n" +
                "        \n" +
                "        // 动画结束后移除元素\n" +
                "        setTimeout(() => {\n" +
                "            firework.remove();\n" +
                "        }, 2000);\n" +
                "    }\n" +
                "});\n" +
                "```";
        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        System.out.println("CSS代码："+result.getCssCode());
        System.out.println("HTML代码："+result.getHtmlCode());
        System.out.println("JS代码："+result.getJsCode());

    }
}
