package com.chuanwise.xiaoming.lexicons;

import com.chuanwise.xiaoming.api.limit.CallLimitConfig;
import com.chuanwise.xiaoming.api.limit.UserCallLimiter;
import com.chuanwise.xiaoming.api.limit.UserCallRecord;
import com.chuanwise.xiaoming.api.user.XiaomingUser;
import com.chuanwise.xiaoming.api.util.TimeUtil;
import com.chuanwise.xiaoming.core.plugin.XiaomingPluginImpl;
import com.chuanwise.xiaoming.lexicons.interactor.LexiconCommandInteactor;
import com.chuanwise.xiaoming.lexicons.config.GroupPersonalCallManager;
import com.chuanwise.xiaoming.lexicons.config.LexiconConfig;
import com.chuanwise.xiaoming.lexicons.data.LexiconData;
import com.chuanwise.xiaoming.lexicons.data.LexiconItem;
import lombok.Data;
import lombok.Getter;
import net.mamoe.mirai.message.data.At;

import java.io.File;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * 小明的词库插件
 */
@Data
public class LexiconPlugin extends XiaomingPluginImpl {
    @Getter
    static LexiconPlugin INSTANCE;
    static final Random random = new Random();

    LexiconData data;
    LexiconConfig config;
    GroupPersonalCallManager limiter;

    @Override
    public void onEnable() {
        INSTANCE = this;
        getDataFolder().mkdirs();

        loadData();
        loadConfig();
        afterLoad();
    }

    public LexiconConfig getConfig() {
        return config;
    }

    public LexiconData getData() {
        return data;
    }

    public void loadData() {
        data = loadFileOrProduce(LexiconData.class, new File(getDataFolder(), "lexicons.json"), LexiconData::new);
    }

    public void loadConfig() {
        config = loadFileOrProduce(
                LexiconConfig.class,
                new File(getDataFolder(), "configurations.json"),
                LexiconConfig::new
        );
        limiter = loadFileOrProduce(
                GroupPersonalCallManager.class,
                new File(getDataFolder(), "limits.json"),
                () -> {
                    GroupPersonalCallManager recorder = new GroupPersonalCallManager();
                    recorder.getGroupPersonalCall().setConfig(config.getGroupPersonalCall());
                    return recorder;
                }
        );
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public void afterLoad() {
        getXiaomingBot().getInteractorManager().register(new LexiconCommandInteactor(), this);
    }

    /**
     * 对每一条消息，检查是否是词库中的成员
     * @param user
     * @return
     */
    @Override
    public boolean onMessage(XiaomingUser user) {
        long qq = user.getQQ();
        String key = user.getMessage();
        final UserCallLimiter limiter = this.limiter.getGroupPersonalCall();

        // 找到的随机回复
        String randomReply = null;
        // 该回答是否是在私人词库里找到的
        boolean fromPersonal = false;
        boolean fromGroup = false;
        boolean privateSend = false;

        // 先在私人词库里找
        LexiconItem userValue = data.getUserValue(qq, key);
        if (Objects.nonNull(userValue) && !userValue.getAnswers().isEmpty()) {
            randomReply = userValue.getAnswers().toArray(new String[0])[random.nextInt(userValue.getAnswers().size())];
            privateSend = userValue.isPrivateSend();
            fromPersonal = true;
        }
        // 再到群词库里找
        if (Objects.isNull(randomReply) && user.inGroup()) {
            LexiconItem groupValue = data.getGroupValue(user.getGroup().getId(), key);
            if (Objects.nonNull(groupValue) && !groupValue.getAnswers().isEmpty()) {
                randomReply = groupValue.getAnswers().toArray(new String[0])[random.nextInt(groupValue.getAnswers().size())];
                privateSend = groupValue.isPrivateSend();
                fromGroup = true;
            }
        }
        // 最后去公共词条里找
        if (Objects.isNull(randomReply)) {
            LexiconItem globalValue = data.getGlobalValue(key);
            if (Objects.nonNull(globalValue) && !globalValue.getAnswers().isEmpty()) {
                randomReply = globalValue.getAnswers().toArray(new String[0])[random.nextInt(globalValue.getAnswers().size())];
                privateSend = globalValue.isPrivateSend();
            }
        }

        // 没找到时结束
        if (Objects.isNull(randomReply)) {
            return false;
        }

        // 群内调用私人词库时受到限制
        if (user.inGroup() && fromPersonal) {
            final CallLimitConfig limitConfig = config.getGroupPersonalCall();
            limiter.addCallRecord(qq);
            if (limiter.isTooManySoUncallable(qq) && limiter.shouldNotice(qq)) {
                final UserCallRecord userCallRecord = limiter.getOrPutCallRecords(qq);
                user.sendPrivateMessage("你最近在群内经常触发私人词条哦，最近{}已经触发了{}次了，休息一下吧（仍旧可以私聊小明或在群内触发公共词条哦）。\n" +
                                "{}再试试在群内调用私人词条吧",
                        TimeUtil.toTimeString(limitConfig.getPeriod()));
                userCallRecord.updateLastNoticeTime();
                this.limiter.save();
            }
        }

        // 如果有 @ 消息
        final String atKey = "[@]";
        if (randomReply.contains(atKey)) {
            if (user.inGroup()) {
                randomReply = randomReply.replaceAll(Pattern.quote(atKey), new At(user.getQQ()).serializeToMiraiCode());
            } else {
                randomReply = randomReply.replaceAll(Pattern.quote(atKey), "[@你]");
            }
        }

        if (privateSend) {
            user.sendPrivateMessage(randomReply);
        } else {
            if (user.inGroup()) {
                user.sendGroupMessage(randomReply);
            } else {
                user.sendMessage(randomReply);
            }
        }
        return true;
    }
}