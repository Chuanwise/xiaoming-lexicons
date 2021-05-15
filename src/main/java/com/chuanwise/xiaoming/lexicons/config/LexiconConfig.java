package com.chuanwise.xiaoming.lexicons.config;

import com.chuanwise.xiaoming.api.limit.CallLimitConfig;
import com.chuanwise.xiaoming.core.limit.CallLimitConfigImpl;
import com.chuanwise.xiaoming.core.preserve.JsonFilePreservable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Data
public class LexiconConfig extends JsonFilePreservable {
    List<Pattern> illegalWords = new ArrayList<>();

    CallLimitConfig groupPersonalCall = new CallLimitConfigImpl();

    public boolean isLegal(String key) {
        if (Objects.nonNull(illegalWords)) {
            for (Pattern regex: illegalWords) {
                if (regex.matcher(key).find()) {
                    return false;
                }
            }
        }
        return true;
    }
}
