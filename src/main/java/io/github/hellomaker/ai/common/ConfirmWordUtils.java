package io.github.hellomaker.ai.common;

import java.util.HashSet;

public class ConfirmWordUtils {

    final static HashSet<String> confirmWords = new HashSet<>();
    static {
        confirmWords.add("confirm");
        confirmWords.add("yes");
        confirmWords.add("ok");
        confirmWords.add("continue");
        confirmWords.add("go on");
        confirmWords.add("execute");
        confirmWords.add("确认");
        confirmWords.add("确定");
        confirmWords.add("是");
        confirmWords.add("继续");
        confirmWords.add("可以");
        confirmWords.add("是的");
        confirmWords.add("没问题");
        confirmWords.add("执行");
    }

    public static boolean isSimpleConfirmMeaning(String words) {
        return words != null && confirmWords.contains(words);
    }

}