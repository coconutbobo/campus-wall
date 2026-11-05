package com.campuswall.util;

import java.util.regex.Pattern;

// 敏感信息启发式检测：只拦截比较确定的手机号和身份证号格式。
// 这不是完美的识别，学号格式各校不同、宿舍号也很难靠正则区分，所以它只是第一道防线，真正的处理还是靠举报和管理。
// 注意：联系方式字段是用户主动留给对方的，不走这个检测，正文和评论才走。
public class SensitiveInfoDetector {
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD = Pattern.compile("\\d{17}[\\dXx]");

    public static boolean containsSensitiveInfo(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return PHONE.matcher(text).find() || ID_CARD.matcher(text).find();
    }
}
