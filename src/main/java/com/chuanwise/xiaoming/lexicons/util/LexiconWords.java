package com.chuanwise.xiaoming.lexicons.util;

import com.chuanwise.xiaoming.api.util.CommandWords;

public class LexiconWords extends CommandWords {
    public static final String LEXICONS_REGEX = "(词库|问答|词条|lexicon)";

    public static final String GLOBAL_LEXICONS_REGEX = GLOBAL_REGEX + LEXICONS_REGEX;

    public static final String GROUP_LEXICONS_REGEX = GROUP_REGEX + LEXICONS_REGEX;

    public static final String PERSONAL_LEXICONS_REGEX = PERSONAL_REGEX + LEXICONS_REGEX;

    public static final String ILLEGAL_REGEX = "(敏感|ban)(词|word)";

    public static final String RESTRICT_REGEX = "(重定向|restrict|equals)";

    public static final String SEND_METHOD_REGEX = "(发送方法|send-method|method|send)";

    public static final String REVERSE_REGEX = "(反转|反向|翻转|reverse)";
}
