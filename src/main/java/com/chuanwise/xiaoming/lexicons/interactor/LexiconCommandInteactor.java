package com.chuanwise.xiaoming.lexicons.interactor;

import com.chuanwise.xiaoming.api.annotation.FilterParameter;
import com.chuanwise.xiaoming.api.annotation.Filter;
import com.chuanwise.xiaoming.api.annotation.GroupInteractor;
import com.chuanwise.xiaoming.api.annotation.RequirePermission;
import com.chuanwise.xiaoming.api.limit.CallLimitConfig;
import com.chuanwise.xiaoming.api.permission.PermissionManager;
import com.chuanwise.xiaoming.api.user.XiaomingUser;
import com.chuanwise.xiaoming.api.util.TimeUtil;
import com.chuanwise.xiaoming.core.interactor.command.CommandInteractorImpl;
import com.chuanwise.xiaoming.lexicons.LexiconPlugin;
import com.chuanwise.xiaoming.lexicons.config.LexiconConfig;
import com.chuanwise.xiaoming.lexicons.data.LexiconData;
import com.chuanwise.xiaoming.lexicons.data.LexiconItem;
import com.chuanwise.xiaoming.lexicons.util.LexiconWords;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Chuanwise
 */
public class LexiconCommandInteactor extends CommandInteractorImpl {
    public LexiconCommandInteactor() {
        enableUsageCommand(LexiconWords.LEXICONS_REGEX);
    }

    @Override
    public void onIllegalUser(XiaomingUser user) {
        user.sendPrivateWarn("你还不能使用小明词库哦，赶快私聊小明「使用词库」吧");
    }

    /**
     * 查看敏感词
     * @param user 查看者
     */
    @Filter(LexiconWords.ILLEGAL_REGEX)
    @RequirePermission("lexicon.illegal.look")
    public void onLookIllegalWords(XiaomingUser user) {
        final LexiconConfig config = LexiconPlugin.getINSTANCE().getConfig();
        final List<Pattern> illegalWords = config.getIllegalWords();
        if (illegalWords.isEmpty()) {
            user.sendMessage("小明词库中并没有设置任何违禁词哦");
        } else {
            user.sendMessage("小明词库中的违禁词有：{}", illegalWords);
        }
    }

    /**
     * 添加敏感词
     */
    @Filter(LexiconWords.NEW_REGEX + LexiconWords.ILLEGAL_REGEX + " {remain}")
    @RequirePermission("lexicon.illegal.add")
    public void onAddIllegalWords(XiaomingUser user,
                                  @FilterParameter("remain") final String regex) {
        final LexiconConfig config = LexiconPlugin.getINSTANCE().getConfig();
        final List<Pattern> illegalWords = config.getIllegalWords();
        if (regex.isEmpty()) {
            user.sendMessage("不可以添加空的违禁词哦");
        } else {
            boolean quote = false;
            Pattern pattern = null;
            try {
                pattern = Pattern.compile(regex);
            } catch (Exception exception) {
                pattern = Pattern.compile(Pattern.quote(regex));
                exception.printStackTrace();
                quote = true;
            }
            if (illegalWords.contains(pattern)) {
                user.sendError("{}已经是违禁词了哦", regex);
            } else {
                illegalWords.add(pattern);
                config.save();
                if (quote) {
                    user.sendMessage("成功添加新的违禁词：{}，但它不符合正则语法，小明已将其原文存储啦", regex);
                } else {
                    user.sendMessage("成功添加新的违禁词：{}", regex);
                }
            }
        }
    }

    /**
     * 删除敏感词
     */
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.ILLEGAL_REGEX + " {remain}")
    @RequirePermission("lexicon.illegal.remove")
    public void onRemoveIllegalWords(XiaomingUser user,
                                     @FilterParameter("remain") final String regex) {
        final LexiconConfig config = LexiconPlugin.getINSTANCE().getConfig();
        final List<Pattern> illegalWords = config.getIllegalWords();
        if (regex.isEmpty()) {
            user.sendMessage("要删除的敏感词为空哦");
        } else {
            boolean removed = false;
            for (Pattern illegalWord : illegalWords) {
                if (illegalWord.pattern().equals(regex)) {
                    illegalWords.remove(illegalWord);
                    removed = true;
                    break;
                }
            }
            if (removed) {
                config.save();
                user.sendMessage("成功删除了敏感词：{}", regex);
            } else {
                user.sendMessage("没有找到敏感词：", regex);
            }
        }
    }

    /**
     * 重载词库
     * @param user
     */
    @Filter(LexiconWords.RELOAD_REGEX + LexiconWords.LEXICONS_REGEX)
    @RequirePermission("lexicon.reload")
    public void onReload(XiaomingUser user) {
        LexiconPlugin.getINSTANCE().loadConfig();
        LexiconPlugin.getINSTANCE().loadData();
        user.sendMessage("成功重载词库数据");
    }

    /**
     * 查看全局词条中的某个项目
     */
    @Filter(LexiconWords.GLOBAL_LEXICONS_REGEX + " {key}")
    @RequirePermission("lexicon.global.look")
    public void onLookGlobalReply(XiaomingUser user,
                                  @FilterParameter("key") final String key) {
        LexiconItem globalValue = LexiconPlugin.getINSTANCE().getData().getGlobalValue(key);
        if (Objects.isNull(globalValue)) {
            user.sendMessage("公共词库中没有词条：{}", key);
        } else {
            StringBuilder builder = new StringBuilder("公共词条 " + key + " 中的随机回答有：");
            int index = 1;
            for (String reply : globalValue.getAnswers()) {
                builder.append("\n").append(index++).append("、").append(reply);
            }
            user.sendMessage(builder.toString());
        }
    }

    /**
     * 查看所有的全局词条
     */
    @Filter(LexiconWords.GLOBAL_LEXICONS_REGEX)
    @RequirePermission("lexicon.global.list")
    public void onListGrobalReply(XiaomingUser user) {
        Map<String, LexiconItem> map = LexiconPlugin.getINSTANCE().getData().getGlobal();
        if (Objects.nonNull(map) && !map.isEmpty()) {
            StringBuilder builder = new StringBuilder("全局有 " + map.size() + " 个词条：");
            for (String key : map.keySet()) {
                builder.append("\n").append(key);
            }
            user.sendMessage(builder.toString());
        } else {
            user.sendMessage("全局没有任何词条");
        }
    }

    /**
     * 查看所有的私人词条
     */
    @Filter(LexiconWords.PERSONAL_LEXICONS_REGEX)
    @RequirePermission("lexicon.personal.list")
    public void onListPersonalReply(XiaomingUser user) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        final Map<String, LexiconItem> userMap = data.getUserMap(user.getQQ());
        if (Objects.nonNull(userMap)) {
            StringBuilder builder = new StringBuilder("你一共有 " + userMap.size() + " 个私人词条：");
            for (String key : userMap.keySet()) {
                builder.append("\n").append(key);
            }
            user.sendMessage(builder.toString());
        } else {
            user.sendMessage("你还没有任何私人词条哦");
        }
    }

    /**
     * 查看所有的群词条
     */
    @GroupInteractor
    @Filter(LexiconWords.GROUP_LEXICONS_REGEX)
    @RequirePermission("lexicon.group.list")
    public void onListGroupReply(XiaomingUser user) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        final Map<String, LexiconItem> groupMap = data.getGroupMap(user.getGroup().getId());
        if (Objects.nonNull(groupMap)) {
            StringBuilder builder = new StringBuilder("本群一共有 " + groupMap.size() + " 个词条：");
            for (String key : groupMap.keySet()) {
                builder.append("\n").append(key);
            }
            user.sendMessage(builder.toString());
        } else {
            user.sendMessage("这里还没有任何群词条哦");
        }
    }

    /**
     * 查看他人的私人词条
     */
    @Filter(LexiconWords.OTHER_REGEX + LexiconWords.LEXICONS_REGEX + " {qq}")
    @RequirePermission("lexicon.others.list")
    public void onListOthersReply(XiaomingUser user,
                                  @FilterParameter("qq") final long qq) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        final Map<String, LexiconItem> userMap = data.getUserMap(qq);
        if (Objects.nonNull(userMap)) {
            StringBuilder builder = new StringBuilder("他一共有 " + userMap.size() + " 个私人词条：");
            for (String key : userMap.keySet()) {
                builder.append("\n").append(key);
            }
            user.sendMessage(builder.toString());
        } else {
            user.sendMessage("他还没有任何私人词条哦");
        }
    }

    /**
     * 查看私人词条中的某个项目
     */
    @Filter(LexiconWords.PERSONAL_LEXICONS_REGEX + " {key}")
    @RequirePermission("lexicon.personal.look")
    public void onLookPersonalReply(XiaomingUser user,
                                    @FilterParameter("key") final String key) {
        LexiconItem userValue = LexiconPlugin.getINSTANCE().getData().getUserValue(user.getQQ(), key);
        if (Objects.isNull(userValue)) {
            user.sendMessage("你没有私人词条：{}", key);
        } else {
            StringBuilder builder = new StringBuilder("你的私人词条 " + key + " 中的随机回答有：");
            int index = 1;
            for (String reply : userValue.getAnswers()) {
                builder.append("\n").append(index++).append("、").append(reply);
            }
            final Set<String> aliases = userValue.getAliases();
            if (!aliases.isEmpty()) {
                builder.append("\n有 ").append(aliases.size()).append(" 个词条被重定向至此：").append(aliases);
            }
            user.sendMessage(builder.toString());
        }
    }

    /**
     * 查看群词条中的某个项目
     */
    @GroupInteractor
    @Filter(LexiconWords.GROUP_LEXICONS_REGEX + " {key}")
    public void onLookGroupReply(XiaomingUser user,
                                 @FilterParameter("key") final String key) {
        if (!user.requirePermission("lexicon." + user.getGroup().getId() + ".look")) {
            return;
        }
        LexiconItem groupValue = LexiconPlugin.getINSTANCE().getData().getGroupValue(user.getGroup().getId(), key);
        if (Objects.isNull(groupValue)) {
            user.sendMessage("本群没有群词条：{}", key);
        } else {
            StringBuilder builder = new StringBuilder("群词条 " + key + " 中的随机回答有：");
            int index = 1;
            for (String reply : groupValue.getAnswers()) {
                builder.append("\n").append(index++).append("、").append(reply);
            }
            final Set<String> aliases = groupValue.getAliases();
            if (!aliases.isEmpty()) {
                builder.append("\n有 ").append(aliases.size()).append(" 个词条被重定向至此：").append(aliases);
            }
            user.sendMessage(builder.toString());
        }
    }

    /**
     * 查看他人的私人词条中的某个项目
     */
    @Filter(LexiconWords.OTHER_REGEX + LexiconWords.LEXICONS_REGEX + " {qq} {key}")
    @RequirePermission("lexicon.others.look")
    public void onLookOthersReply(XiaomingUser user,
                                  @FilterParameter("key") final String key,
                                  @FilterParameter("qq") final long qq) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem userValue = data.getUserValue(qq, key);
        if (Objects.nonNull(userValue)) {
            StringBuilder builder = new StringBuilder("他的私人词条 " + key + " 中的随机回答有：");
            int index = 1;
            for (String reply : userValue.getAnswers()) {
                builder.append("\n").append(index++).append("、").append(reply);
            }
            final Set<String> aliases = userValue.getAliases();
            if (!aliases.isEmpty()) {
                builder.append("\n有 ").append(aliases.size()).append(" 个词条被重定向至此：").append(aliases);
            }
            user.sendMessage(builder.toString());
        } else {
            user.sendMessage("他的私人词条中没有：{}", key);
        }
    }

    /**
     * 私发他人的私人词条中的某个项目
     */
    @Filter(LexiconWords.REVERSE_REGEX + LexiconWords.OTHER_REGEX + LexiconWords.LEXICONS_REGEX + LexiconWords.SEND_METHOD_REGEX + " {qq} {key}")
    @RequirePermission("lexicon.others.method")
    public void onReverseMethodOthersReply(XiaomingUser user,
                                           @FilterParameter("key") final String key,
                                           @FilterParameter("qq") final long qq) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        final LexiconItem userValue = data.getUserValue(qq, key);
        if (Objects.nonNull(userValue)) {
            userValue.setPrivateSend(!userValue.isPrivateSend());
            user.sendMessage("他的私人词条：{}的发送方法被设置为：{}", key, userValue.isPrivateSend() ? "私发" : "自动");
            data.save();
        } else {
            user.sendMessage("他的私人词条中没有：{}", key);
        }
    }

    /**
     * 翻转私人词条发送方法
     */
    @Filter(LexiconWords.REVERSE_REGEX + LexiconWords.PERSONAL_REGEX + LexiconWords.LEXICONS_REGEX + LexiconWords.SEND_METHOD_REGEX + " {key}")
    public void onReverseMethodPersonalReply(XiaomingUser user,
                                             @FilterParameter("key") final String key) {
        if (!user.requirePermission("lexicon.personal.method." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        final LexiconItem userValue = data.getUserValue(user.getQQ(), key);
        if (Objects.isNull(userValue)) {
            user.sendMessage("你没有私人词条：{}", key);
        } else {
            userValue.setPrivateSend(!userValue.isPrivateSend());
            user.sendMessage("你的私人词条：{}的发送方法被设置为：{}", key, userValue.isPrivateSend() ? "私发" : "自动");
            data.save();
        }
    }

    /**
     * 翻转全局词条发送方法
     */
    @Filter(LexiconWords.REVERSE_REGEX + LexiconWords.GLOBAL_REGEX + LexiconWords.LEXICONS_REGEX + LexiconWords.SEND_METHOD_REGEX + " {key}")
    public void onReverseMethodGlobalReply(XiaomingUser user,
                                           @FilterParameter("key") final String key) {
        if (!user.requirePermission("lexicon.global.method." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        final LexiconItem globalValue = data.getGlobalValue(key);
        if (Objects.isNull(globalValue)) {
            user.sendMessage("全局没有词条：{}", key);
        } else {
            globalValue.setPrivateSend(!globalValue.isPrivateSend());
            user.sendMessage("全局词条：{}的发送方法被设置为：{}", key, globalValue.isPrivateSend() ? "私发" : "自动");
            data.save();
        }
    }

    /**
     * 翻转群词条发送方法
     */
    @GroupInteractor
    @Filter(LexiconWords.REVERSE_REGEX + LexiconWords.GROUP_LEXICONS_REGEX + LexiconWords.LEXICONS_REGEX + LexiconWords.SEND_METHOD_REGEX + " {key}")
    public void onReverseMethodGroupReply(XiaomingUser user,
                                           @FilterParameter("key") final String key) {
        if (!user.requirePermission("lexicon." + user.getGroup().getId() + ".method." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        final LexiconItem globalValue = data.getGroupValue(user.getGroup().getId(), key);
        if (Objects.isNull(globalValue)) {
            user.sendMessage("本群没有私人词条：{}", key);
        } else {
            globalValue.setPrivateSend(!globalValue.isPrivateSend());
            user.sendMessage("本群词条：{}的发送方法被设置为：{}", key, globalValue.isPrivateSend() ? "私发" : "自动");
            data.save();
        }
    }

    /**
     * 删除私人词条
     */
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.PERSONAL_LEXICONS_REGEX + " {key}")
    public void onRemovePersonalReply(XiaomingUser user,
                                      @FilterParameter("key") final String key) {
        if (!user.requirePermission("lexicon.personal.remove." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem userValue = data.getUserValue(user.getQQ(), key);
        if (Objects.isNull(userValue)) {
            user.sendMessage("你没有私人词条：{}", key);
        } else {
            final Set<String> aliases = userValue.getAliases();
            if (aliases.contains(key)) {
                user.sendMessage("你的私人词条{}并不是独立的词条，已经解除其和其他词条的定向关系", key);
                aliases.remove(key);
            } else {
                data.removeUserKey(user.getQQ(), key);
                StringBuilder builder = new StringBuilder("已删除你的私人词条「" + key + "」。");
                if (!aliases.isEmpty()) {
                    builder.append("有").append(aliases.size()).append("个词条被重定向至此，也被一并删除：").append(aliases);
                }
                user.sendMessage(builder.toString());
            }
            data.save();
        }
    }

    /**
     * 删除私人词条下的某个回复
     */
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.PERSONAL_LEXICONS_REGEX + " {key} {index}")
    public void onRemovePersonalReply(XiaomingUser user,
                                      @FilterParameter("key") final String key,
                                      @FilterParameter("index") final String indexString) {
        if (!user.requirePermission("lexicon.personal.remove." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem userValue = data.getUserValue(user.getQQ(), key);
        if (Objects.isNull(userValue)) {
            user.sendMessage("你没有私人词条：{}", key);
        } else {
            if (indexString.matches("\\d+")) {
                int index = Integer.parseInt(indexString);
                if (index >= 1 && index <= userValue.getAnswers().size()) {
                    index--;
                    data.removeUserKey(user.getQQ(), key, index);

                    if (userValue.getAnswers().isEmpty()) {
                        user.sendMessage("已删除私人词条{}", key, indexString);
                    } else {
                        user.sendMessage("已删除{}词条下的第{}条回复", key, indexString);
                    }
                    data.save();
                } else {
                    user.sendError("序号{}不对哦，应该在 1 到{}之间", userValue.getAnswers().size());
                }
            } else {
                user.sendError("{}这里应该写删除的是第几个随机回答（使用 我的词条 {} 查看），或者留空以删除整个私人词条哦", indexString, key);
            }
        }
    }

    /**
     * 删除他人的私人词条
     */
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.OTHER_REGEX + " {qq} {key}")
    public void onRemoveOthersReply(XiaomingUser user,
                                    @FilterParameter("qq") final long qq,
                                    @FilterParameter("key") final String key) {
        if (!user.requirePermission("lexicon.others.remove." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem userValue = data.getUserValue(qq, key);
        if (Objects.isNull(userValue)) {
            user.sendMessage("他没有私人词条：{}", key);
        } else {
            final Set<String> aliases = userValue.getAliases();
            if (aliases.contains(key)) {
                user.sendMessage("他的私人词条{}并不是独立的词条，已经解除其和其他词条的定向关系", key);
                aliases.remove(key);
            } else {
                data.removeUserKey(qq, key);
                StringBuilder builder = new StringBuilder("已删除他的私人词条「" + key + "」。");
                if (!aliases.isEmpty()) {
                    builder.append("有 ").append(aliases.size()).append(" 个词条被重定向至此，也被一并删除：").append(aliases);
                }
                user.sendMessage(builder.toString());
            }
            data.save();
        }
    }

    /**
     * 删除他人的私人词条
     */
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.OTHER_REGEX + " {qq} {key} {index}")
    public void onRemoveOthersReply(XiaomingUser user,
                                    @FilterParameter("qq") final long qq,
                                    @FilterParameter("key") final String key,
                                    @FilterParameter("index") final String indexString) {
        if (!user.requirePermission("lexicon.others.remove." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem userValue = data.getUserValue(qq, key);
        if (Objects.isNull(userValue)) {
            user.sendMessage("用户{}没有私人词条：{}", qq, key);
        } else {
            if (indexString.matches("\\d+")) {
                int index = Integer.parseInt(indexString);
                if (index >= 1 && index <= userValue.getAnswers().size()) {
                    index--;
                    data.removeUserKey(qq, key, index);
                    if (userValue.getAnswers().isEmpty()) {
                        user.sendMessage("已删除他的私人词条{}", key, indexString);
                    } else {
                        user.sendMessage("已删除他的私人词条{}下的第{}条回复", key, indexString);
                    }
                    data.save();
                } else {
                    user.sendError("序号{}不对哦，应该在 1 到{}之间", userValue.getAnswers().size());
                }
            } else {
                user.sendError("{}这里应该写删除的是第几个随机回答（使用 查看他人 @他 {} 查看），或者留空以删除整个私人词条哦", indexString, key);
            }
        }
    }

    /**
     * 删除全局词条
     */
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.GLOBAL_LEXICONS_REGEX + " {key}")
    public void onRemoveGlobalReply(XiaomingUser user,
                                    @FilterParameter("key") final String key) {
        if (!user.requirePermission("lexicon.global.remove." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem globalValue = data.getGlobalValue(key);
        if (Objects.isNull(globalValue)) {
            user.sendMessage("全局词库中没有词条：{}", key);
        } else {
            final Set<String> aliases = globalValue.getAliases();
            if (aliases.contains(key)) {
                user.sendMessage("全局词条{}并不是独立的词条，已经解除其和其他词条的定向关系", key);
                aliases.remove(key);
            } else {
                data.removeGlobal(key);
                StringBuilder builder = new StringBuilder("已删除全局词条「" + key + "」。");
                if (!aliases.isEmpty()) {
                    builder.append("有 ").append(aliases.size()).append(" 个词条被重定向至此，也被一并删除：").append(aliases);
                }
                user.sendMessage(builder.toString());
            }
            data.save();
        }
    }

    /**
     * 删群词条
     */
    @GroupInteractor
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.GROUP_LEXICONS_REGEX + " {key}")
    public void onRemoveGroupReply(XiaomingUser user,
                                   @FilterParameter("key") final String key) {
        if (!user.requirePermission("lexicon." + user.getGroup().getId() + ".remove." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem groupValue = data.getGroupValue(user.getGroup().getId(), key);
        if (Objects.isNull(groupValue)) {
            user.sendMessage("本群词库中没有词条：{}", key);
        } else {
            final Set<String> aliases = groupValue.getAliases();
            if (aliases.contains(key)) {
                user.sendMessage("本群词条{}并不是独立的词条，已经解除其和其他词条的定向关系", key);
                aliases.remove(key);
            } else {
                data.removeGroupKey(user.getGroup().getId(), key);
                StringBuilder builder = new StringBuilder("已删除本群词条「" + key + "」。");
                if (!aliases.isEmpty()) {
                    builder.append("有 ").append(aliases.size()).append(" 个词条被重定向至此，也被一并删除：").append(aliases);
                }
                user.sendMessage(builder.toString());
            }
            data.save();
        }
    }

    /**
     * 删除全局词条中的某个项目
     */
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.GLOBAL_LEXICONS_REGEX + " {key} {index}")
    public void onRemoveGlobalReply(XiaomingUser user,
                                    @FilterParameter("key") final String key,
                                    @FilterParameter("index") final String indexString) {
        if (!user.requirePermission("lexicon.global.remove." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem globalValue = data.getGlobalValue(key);
        if (Objects.isNull(globalValue)) {
            user.sendMessage("全局词库中没有词条：{}", key);
        } else {
            if (indexString.matches("\\d+")) {
                int index = Integer.parseInt(indexString);
                if (index >= 1 && index <= globalValue.getAnswers().size()) {
                    index--;
                    data.removeGlobal(key, index);
                    if (globalValue.getAnswers().isEmpty()) {
                        user.sendMessage("已删除全局词条{}", key, indexString);
                    } else {
                        user.sendMessage("已删除{}词条下的第{}条回复", key, indexString);
                    }
                    data.save();
                } else {
                    user.sendError("序号{}不对哦，应该在 1 到{}之间", globalValue.getAnswers().size());
                }
            } else {
                user.sendError("{}这里应该写删除的是第几个随机回答（使用 全局词条{}查看），或者留空以删除整个全局词条哦", key, indexString);
            }
        }
    }

    /**
     * 删除群词条中的某个项目
     */
    @GroupInteractor
    @Filter(LexiconWords.REMOVE_REGEX + LexiconWords.GROUP_LEXICONS_REGEX + " {key} {index}")
    public void onRemoveGroupReply(XiaomingUser user,
                                   @FilterParameter("key") final String key,
                                   @FilterParameter("index") final String indexString) {
        if (!user.requirePermission("lexicon." + user.getGroup().getId() + ".remove." + key)) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem groupValue = data.getGroupValue(user.getGroup().getId(), key);
        if (Objects.isNull(groupValue)) {
            user.sendMessage("群词库中没有词条：{}", key);
        } else {
            if (indexString.matches("\\d+")) {
                int index = Integer.parseInt(indexString);
                if (index >= 1 && index <= groupValue.getAnswers().size()) {
                    index--;
                    data.removeGroupKey(user.getGroup().getId(), key, index);
                    if (groupValue.getAnswers().isEmpty()) {
                        user.sendMessage("已删除本群词条{}", key, indexString);
                    } else {
                        user.sendMessage("已删除本群{}词条下的第{}条回复", key, indexString);
                    }
                    data.save();
                } else {
                    user.sendError("序号{}不对哦，应该在 1 到{}之间", groupValue.getAnswers().size());
                }
            } else {
                user.sendError("{}这里应该写删除的是第几个随机回答（使用 群词条{}查看），或者留空以删除整个群词条哦", key, indexString);
            }
        }
    }

    /**
     * 添加私人词条
     */
    @Filter(LexiconWords.NEW_REGEX + LexiconWords.PERSONAL_LEXICONS_REGEX + " {key} {remain}")
    public void onAddPersonalReply(XiaomingUser user,
                                   @FilterParameter("key") final String key,
                                   @FilterParameter("remain") final String value) {
        if (!user.requirePermission("lexicon.personal.add." + key)) {
            return;
        }

        long qq = user.getQQ();

        if (value.isEmpty()) {
            user.sendMessage("回答不能为空呢 ( *^-^)ρ(*╯^╰)");
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem personalValue = data.getUserValue(qq, key);

        if (!LexiconPlugin.getINSTANCE().getConfig().isLegal(key)) {
            user.sendMessage("这个词条好像不太好，不如换一个吧 (；′⌒`)");
            return;
        }
        if (!LexiconPlugin.getINSTANCE().getConfig().isLegal(value)) {
            user.sendMessage("这个回答看起来不太好，还是换一个吧 （；´д｀）ゞ");
            return;
        }

        // 保存或寻找词条中的 URL 图片
        /*
        if (!getXiaomingBot().getPictureManager().listCatCodes(key).isEmpty()) {
            user.sendError("触发词中不能包含图片");
            return;
        }
        getXiaomingBot().getPictureManager().requireRecordedMessage(value);*/

        if (Objects.isNull(personalValue)) {
            data.addUser(qq, key, value);
            user.sendMessage("已添加新的私人词条 {} => {}", key, value);
        } else {
            personalValue.getAnswers().add(value);
            user.sendMessage("已在现有的私人词条{}下追加了新的回复" +
                    "（现该词条下共有{}条随机回复）：{}", key, personalValue.getAnswers().size(), value);
        }
        data.save();
    }

    /**
     * 添加群词条
     */
    @GroupInteractor
    @Filter(LexiconWords.NEW_REGEX + LexiconWords.GROUP_LEXICONS_REGEX + " {key} {remain}")
    public void onAddGroupReply(XiaomingUser user,
                                @FilterParameter("key") final String key,
                                @FilterParameter("remain") final String value) {
        if (!user.requirePermission("lexicon." + user.getGroup().getId() + ".add." + key)) {
            return;
        }

        long qq = user.getQQ();

        if (value.isEmpty()) {
            user.sendMessage("回答不能为空呢 ( *^-^)ρ(*╯^╰)");
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        final LexiconItem groupValue = data.getGroupValue(user.getGroup().getId(), key);

        if (!LexiconPlugin.getINSTANCE().getConfig().isLegal(key)) {
            user.sendMessage("这个词条好像不太好，不如换一个吧 (；′⌒`)");
            return;
        }
        if (!LexiconPlugin.getINSTANCE().getConfig().isLegal(value)) {
            user.sendMessage("这个回答看起来不太好，还是换一个吧 （；´д｀）ゞ");
            return;
        }

        // 保存或寻找词条中的 URL 图片
        /*
        if (!getXiaomingBot().getPictureManager().listCatCodes(key).isEmpty()) {
            user.sendError("触发词中不能包含图片");
            return;
        }
        getXiaomingBot().getPictureManager().requireRecordedMessage(value);

         */

        if (Objects.isNull(groupValue)) {
            data.addGroup(user.getGroup().getId(), key, value);
            user.sendMessage("已添加新的群词条 {} => {}", key, value);
        } else {
            groupValue.getAnswers().add(value);
            user.sendMessage("已在现有的群词条{}下追加了新的回复" +
                    "（现该词条下共有{}条随机回复）：{}", key, groupValue.getAnswers().size(), value);
        }
        data.save();
    }

    /**
     * 添加全局词条
     */
    @Filter(LexiconWords.NEW_REGEX + LexiconWords.GLOBAL_LEXICONS_REGEX + " {key} {remain}")
    public void onAddGlobalReply(XiaomingUser user,
                                 @FilterParameter("key") final String key,
                                 @FilterParameter("remain") final String value) {
        if (!user.requirePermission("lexicon.global.add." + key)) {
            return;
        }
        if (value.isEmpty()) {
            user.sendMessage("回答不能为空呢 ( *^-^)ρ(*╯^╰)");
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem globalValue = data.getGlobalValue(key);

        if (!LexiconPlugin.getINSTANCE().getConfig().isLegal(key)) {
            user.sendMessage("这个词条好像不太好，不如换一个吧 (；′⌒`)");
            return;
        }
        if (!LexiconPlugin.getINSTANCE().getConfig().isLegal(value)) {
            user.sendMessage("这个回答看起来不太好，还是换一个吧 （；´д｀）ゞ");
            return;
        }

        // 保存或寻找词条中的 URL 图片
        /*
        if (!getXiaomingBot().getPictureManager().listCatCodes(key).isEmpty()) {
            user.sendError("触发词中不能包含图片");
            return;
        }
        getXiaomingBot().getPictureManager().requireRecordedMessage(value);
         */

        if (Objects.isNull(globalValue)) {
            data.addGlobal(key, value);
            user.sendMessage("已添加新的公共词条 {} => {}", key, value);
        } else {
            globalValue.getAnswers().add(value);
            user.sendMessage("已在现有的公共词条{}下追加了新的回复" +
                    "（现该词条下共有{}条随机回复）：{}", key, globalValue.getAnswers().size(), value);
        }
        data.save();
    }

    /**
     * 重定向全局词条
     */
    @Filter(LexiconWords.RESTRICT_REGEX + LexiconWords.GLOBAL_LEXICONS_REGEX + " {what} {alias}")
    @RequirePermission("lexicon.global.restrict")
    public void onRestrictGlobalReply(XiaomingUser user,
                                      @FilterParameter("what") final String what,
                                      @FilterParameter("alias") final String alias) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem source = data.getGlobalValue(what);
        LexiconItem to = data.getGlobalValue(alias);
        if (Objects.isNull(source)) {
            user.sendError("找不到全局词条{}", what);
        } else if (Objects.nonNull(to)) {
            user.sendError("已经存在全局词条{}，请先 合并全局词条 {} {} 或 删除全局词条 {}", alias, what, alias, alias);
        } else if (to == source) {
            user.sendError("这两个词条指向同一词条，不需再次重定向");
        } else {
            source.addAlia(alias);
            data.save();
            user.sendMessage("成功将全局词条{}重定向到现有的全局词条{}", alias, what);
        }
    }

    /**
     * 重定向私人词条
     */
    @Filter(LexiconWords.RESTRICT_REGEX + LexiconWords.PERSONAL_LEXICONS_REGEX + " {what} {alias}")
    @RequirePermission("lexicon.personal.restrict")
    public void onRestrictPersonalReply(XiaomingUser user,
                                        @FilterParameter("what") final String what,
                                        @FilterParameter("alias") final String alias) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem source = data.getUserValue(user.getQQ(), what);
        LexiconItem to = data.getUserValue(user.getQQ(), alias);
        if (Objects.isNull(source)) {
            user.sendError("找不到私人词条{}", what);
        } else if (Objects.nonNull(to)) {
            user.sendError("已经存在私人词条{}，请先 合并私人词条 {} {} 或 删除私人词条 {}", alias, what, alias, alias);
        } else if (to == source) {
            user.sendError("这两个词条指向同一词条，不需再次重定向");
        } else {
            source.addAlia(alias);
            data.save();
            user.sendMessage("成功将私人词条{}重定向到现有的私人词条{}", alias, what);
        }
    }

    /**
     * 重定向群词条
     */
    @GroupInteractor
    @Filter(LexiconWords.RESTRICT_REGEX + LexiconWords.GROUP_LEXICONS_REGEX + " {what} {alias}")
    public void onRestrictGroupReply(XiaomingUser user,
                                     @FilterParameter("what") final String what,
                                     @FilterParameter("alias") final String alias) {
        if (!user.requirePermission("lexicon." + user.getGroup().getId() + ".restrict")) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem source = data.getGroupValue(user.getGroup().getId(), what);
        LexiconItem to = data.getGroupValue(user.getGroup().getId(), alias);
        if (Objects.isNull(source)) {
            user.sendError("找不到群词条{}", what);
        } else if (Objects.nonNull(to)) {
            user.sendError("已经存在群词条{}，请先 合并群词条 {} {} 或 删除群词条 {}", alias, what, alias, alias);
        } else if (to == source) {
            user.sendError("这两个词条指向同一词条，不需再次重定向");
        } else {
            source.addAlia(alias);
            data.save();
            user.sendMessage("成功将群词条{}重定向到现有的群词条{}", alias, what);
        }
    }

    /**
     * 合并私人词条
     */
    @Filter(LexiconWords.MERGE_REGEX + LexiconWords.PERSONAL_LEXICONS_REGEX + " {what} {from}")
    @RequirePermission("lexicon.personal.merge")
    public void onMergePersonalReply(XiaomingUser user,
                                     @FilterParameter("what") final String what,
                                     @FilterParameter("from") final String alias) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem source = data.getUserValue(user.getQQ(), what);
        LexiconItem from = data.getUserValue(user.getQQ(), alias);
        if (Objects.isNull(source)) {
            user.sendError("找不到私人词条{}", what);
        } else if (from == source) {
            user.sendError("这两个词条指向同一词条，不能自合并");
        } else if (Objects.nonNull(from)) {
            source.getAnswers().addAll(from.getAnswers());
            source.getAliases().addAll(from.getAliases());
            if (from.getAliases().isEmpty()) {
                user.sendMessage("成功将私人词条{}并入另一个私人词条{}。并入操作为词条{}增加了{}个随机回答",
                        alias, what, what, from.getAnswers().size());
            } else {
                user.sendMessage("成功将私人词条{}并入另一个私人词条{}。并入操作为词条{}增加了{}个随机回答，{}个词条也随之被重定向至此",
                        alias, what, what, from.getAnswers().size(), from.getAliases().size());
            }
            data.removeUserKey(user.getQQ(), alias);
        } else {
            user.sendError("找不到私人词条{}", alias);
        }
    }

    /**
     * 合并全局词条
     */
    @Filter(LexiconWords.MERGE_REGEX + LexiconWords.GLOBAL_LEXICONS_REGEX + " {what} {from}")
    @RequirePermission("lexicon.global.merge")
    public void onMergeGlobalReply(XiaomingUser user,
                                   @FilterParameter("what") final String what,
                                   @FilterParameter("from") final String alias) {
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem source = data.getGlobalValue(what);
        LexiconItem from = data.getGlobalValue(alias);
        if (Objects.isNull(source)) {
            user.sendError("找不到全局词条{}", what);
        } else if (from == source) {
            user.sendError("这两个词条指向同一词条，不能自合并");
        } else if (Objects.nonNull(from)) {
            source.getAnswers().addAll(from.getAnswers());
            source.getAliases().addAll(from.getAliases());
            if (from.getAliases().isEmpty()) {
                user.sendMessage("成功将全局词条{}并入另一个全局词条{}。并入操作为词条{}增加了{}个随机回答",
                        alias, what, what, from.getAnswers().size());
            } else {
                user.sendMessage("成功将全局词条{}并入另一个全局词条{}。并入操作为词条{}增加了{}个随机回答，{}个词条也随之被重定向至此",
                        alias, what, what, from.getAnswers().size(), from.getAliases().size());
            }
            data.removeGlobal(alias);
        } else {
            user.sendError("找不到全局词条{}", alias);
        }
    }

    /**
     * 合并群词条
     */
    @GroupInteractor
    @Filter(LexiconWords.MERGE_REGEX + LexiconWords.GROUP_LEXICONS_REGEX + " {what} {from}")
    public void onMergeGroupReply(XiaomingUser user,
                                  @FilterParameter("what") final String what,
                                  @FilterParameter("from") final String alias) {
        if (!user.requirePermission("lexicon." + user.getGroup().getId() + ".restrict")) {
            return;
        }
        final LexiconData data = LexiconPlugin.getINSTANCE().getData();
        LexiconItem source = data.getGroupValue(user.getGroup().getId(), what);
        LexiconItem from = data.getGroupValue(user.getGroup().getId(), alias);
        if (Objects.isNull(source)) {
            user.sendError("找不到群词条{}", what);
        } else if (from == source) {
            user.sendError("这两个词条指向同一词条，不能自合并");
        } else if (Objects.nonNull(from)) {
            source.getAnswers().addAll(from.getAnswers());
            source.getAliases().addAll(from.getAliases());
            if (from.getAliases().isEmpty()) {
                user.sendMessage("成功将群词条{}并入另一个群词条{}。并入操作为词条{}增加了{}个随机回答",
                        alias, what, what, from.getAnswers().size());
            } else {
                user.sendMessage("成功将群词条{}并入另一个群词条{}。并入操作为词条{}增加了{}个随机回答，{}个词条也随之被重定向至此",
                        alias, what, what, from.getAnswers().size(), from.getAliases().size());
            }
            data.removeGlobal(alias);
        } else {
            user.sendError("找不到群词条{}", alias);
        }
    }

    /**
     * 查看调用限制
     */
    @Filter(LexiconWords.LEXICONS_REGEX + LexiconWords.CALL_REGEX + LexiconWords.LIMIT_REGEX)
    public void onLookGroupCallLimit(XiaomingUser user) {
        final CallLimitConfig config = LexiconPlugin.getINSTANCE().getConfig().getGroupPersonalCall();
        user.sendMessage("只有在 QQ 群内调用小明的私人词库有调用限制。\n" +
                        "每{}可以调用{}次，而私聊调用任何词库，或在群内调用公共词库无此限制",
                TimeUtil.toTimeString(config.getPeriod()),
                config.getTop());
    }

    /**
     * 设置调用周期
     */
    @Filter(LexiconWords.LEXICONS_REGEX + LexiconWords.CALL_REGEX + " (周期|period) {time}")
    @RequirePermission("lexicon.limit.period")
    public void onSetPeriod(XiaomingUser user,
                            @FilterParameter("time") final String timeString) {
        final long time = TimeUtil.parseTime(timeString);
        final LexiconConfig lexiconConfig = LexiconPlugin.getINSTANCE().getConfig();
        final CallLimitConfig config = lexiconConfig.getGroupPersonalCall();
        if (time == -1) {
            user.sendMessage("{}并不是一个合理的时间哦", timeString);
        } else {
            config.setPeriod(time);
            lexiconConfig.save();
            user.sendMessage("成功设置词库间隔调用时长为{}，在这段时间内最多在群内调用{}次私人词库",
                    TimeUtil.toTimeString(time),
                    config.getTop());
        }
    }

    /**
     * 设置调用上限
     */
    @Filter(LexiconWords.LEXICONS_REGEX + LexiconWords.CALL_REGEX + " (上限|top) {time}")
    @RequirePermission("lexicon.limit.top")
    public void onSetTop(XiaomingUser user,
                         @FilterParameter("time") final String timeString) {
        if (!timeString.matches("\\d+")) {
            user.sendError("{}并不是一个合理的数字哦", timeString);
        }
        final LexiconConfig lexiconConfig = LexiconPlugin.getINSTANCE().getConfig();
        final CallLimitConfig config = lexiconConfig.getGroupPersonalCall();
        int time = Integer.parseInt(timeString);
        config.setTop(time);
        lexiconConfig.save();
        user.sendMessage("成功设置词库最大调用次数为{}次，每{}内可以在群内调用这么多次私人词库",
                time,
                TimeUtil.toTimeString(config.getTop()));
    }
}