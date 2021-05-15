package com.chuanwise.xiaoming.lexicons.config;

import com.chuanwise.xiaoming.api.limit.UserCallLimiter;
import com.chuanwise.xiaoming.core.limit.UserCallLimiterImpl;
import com.chuanwise.xiaoming.core.preserve.JsonFilePreservable;

/**
 * @author Chuanwise
 */
public class GroupPersonalCallManager extends JsonFilePreservable {
    UserCallLimiter groupPersonalCall = new UserCallLimiterImpl();

    public UserCallLimiter getGroupPersonalCall() {
        return groupPersonalCall;
    }

    public void setGroupPersonalCall(UserCallLimiter groupPersonalCall) {
        this.groupPersonalCall = groupPersonalCall;
    }
}
