package com.chuanwise.xiaoming.lexicons.data;

import com.chuanwise.xiaoming.core.preserve.JsonFilePreservable;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class LexiconData
        extends JsonFilePreservable {
    Map<Long, Map<String, LexiconItem>> users = new HashMap<>();
    Map<String, LexiconItem> global = new HashMap<>();
    Map<Long, Map<String, LexiconItem>> groups = new HashMap<>();

    public void addGlobal(final String key, final String answer) {
        LexiconItem globalValue = getGlobalValue(key);
        if (Objects.isNull(globalValue)) {
            globalValue = new LexiconItem();
            global.put(key, globalValue);
        }
        globalValue.addAnswer(answer);
    }

    public void addUser(final long qq, final String key, final String answer) {
        LexiconItem lexiconItem = getUserValue(qq, key);
        if (Objects.isNull(lexiconItem)) {
            lexiconItem = new LexiconItem();
            getOrPutUserMap(qq).put(key, lexiconItem);
        }
        lexiconItem.addAnswer(answer);
    }

    public void addGroup(final long group, final String key, final String answer) {
        LexiconItem lexiconItem = getGroupValue(group, key);
        if (Objects.isNull(lexiconItem)) {
            lexiconItem = new LexiconItem();
            getOrPutGroupMap(group).put(key, lexiconItem);
        }
        lexiconItem.addAnswer(answer);
    }

    @Nullable
    public LexiconItem getUserValue(final long qq, final String key) {
        final Map<String, LexiconItem> itemMap = users.get(qq);
        if (Objects.nonNull(itemMap)) {
            // 先根据别名
            for (Map.Entry<String, LexiconItem> entry : itemMap.entrySet()) {
                final LexiconItem entryValue = entry.getValue();
                final String entryKey = entry.getKey();
                if (entryKey.equals(key) || (Objects.nonNull(entryValue.getAliases()) && entryValue.getAliases().contains(key))) {
                    return entryValue;
                }
            }
            // 再根据主键
            return users.get(qq).get(key);
        }
        return null;
    }

    @Nullable
    public LexiconItem getGroupValue(final long group, final String key) {
        final Map<String, LexiconItem> itemMap = groups.get(group);
        if (Objects.nonNull(itemMap)) {
            // 先根据别名
            for (Map.Entry<String, LexiconItem> entry : itemMap.entrySet()) {
                final LexiconItem entryValue = entry.getValue();
                final String entryKey = entry.getKey();
                if (entryKey.equals(key) || (Objects.nonNull(entryValue.getAliases()) && entryValue.getAliases().contains(key))) {
                    return entryValue;
                }
            }
            // 再根据主键
            return groups.get(group).get(key);
        }
        return null;
    }

    @Nullable
    public LexiconItem getGlobalValue(final String key) {
        if (Objects.nonNull(global)) {
            for (String string: global.keySet()) {
                LexiconItem item = global.get(string);
                if (string.equals(key) || (Objects.nonNull(item.getAliases()) && item.getAliases().contains(key))) {
                    return item;
                }
            }
        }
        return null;
    }

    @Nullable
    public Map<String, LexiconItem> getUserMap(final long qq) {
        return users.get(qq);
    }

    @NotNull
    public Map<String, LexiconItem> getOrPutUserMap(final long qq) {
        Map<String, LexiconItem> userMap = getUserMap(qq);
        if (Objects.isNull(userMap)) {
            userMap = new HashMap<>();
            users.put(qq, userMap);
        }
        return userMap;
    }

    @Nullable
    public Map<String, LexiconItem> getGroupMap(final long group) {
        return groups.get(group);
    }


    @NotNull
    public Map<String, LexiconItem> getOrPutGroupMap(final long group) {
        Map<String, LexiconItem> userMap = getGroupMap(group);
        if (Objects.isNull(userMap)) {
            userMap = new HashMap<>();
            groups.put(group, userMap);
        }
        return userMap;
    }

    public void removeGlobal(final String key) {
        if (Objects.nonNull(global)) {
            global.remove(key);
        }
    }

    public void removeGlobal(final String key, final int index) {
        LexiconItem globalValue = getGlobalValue(key);
        if (Objects.nonNull(globalValue)) {
            globalValue.getAnswers().remove(index);
            if (globalValue.getAnswers().isEmpty()) {
                global.remove(key);
            }
        }
    }

    public void removeUserKey(final long qq, final String key) {
        if (Objects.nonNull(users) && users.containsKey(qq)) {
            Map<String, LexiconItem> userMap = users.get(qq);
            userMap.remove(key);
            if (userMap.isEmpty()) {
                users.remove(userMap);
            }
        }
    }

    public void removeUserKey(final long qq, final String key, int index) {
        if (Objects.nonNull(users) && users.containsKey(qq)) {
            Map<String, LexiconItem> userMap = users.get(qq);
            LexiconItem lexiconItem = userMap.get(key);
            if (index > 0 && index < lexiconItem.getAnswers().size()) {
                lexiconItem.getAnswers().remove(index);
            }
            if (userMap.isEmpty()) {
                removeUserKey(qq, key);
            }
        }
    }

    public void removeGroupKey(final long group, final String key) {
        final Map<String, LexiconItem> groupMap = groups.get(group);
        if (Objects.nonNull(groupMap)) {
            groupMap.remove(key);
            if (groupMap.isEmpty()) {
                groups.remove(group);
            }
        }
    }

    public void removeGroupKey(final long group, final String key, final int index) {
        final Map<String, LexiconItem> groupMap = groups.get(group);
        if (Objects.nonNull(groupMap)) {
            final LexiconItem lexiconItem = groupMap.get(key);
            if (Objects.nonNull(lexiconItem) && lexiconItem.getAnswers().size() > index) {
                lexiconItem.getAnswers().remove(index);
            }
            if (lexiconItem.getAnswers().isEmpty()) {
                removeGroupKey(group, key);
            }
        }
    }
}
