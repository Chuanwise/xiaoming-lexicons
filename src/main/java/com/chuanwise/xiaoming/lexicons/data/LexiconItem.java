package com.chuanwise.xiaoming.lexicons.data;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class LexiconItem {
    private Set<String> answers = new HashSet<>();
    private Set<String> aliases = new HashSet<>();
    private boolean privateSend = false;

    public Set<String> getAnswers() {
        return answers;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void addAnswer(@NotNull final String answer) {
        answers.add(answer);
    }

    public void addAlia(@NotNull final String alia) {
        aliases.add(alia);
    }

    public boolean isPrivateSend() {
        return privateSend;
    }

    public void setPrivateSend(boolean privateSend) {
        this.privateSend = privateSend;
    }

    public void setAnswers(Set<String> answers) {
        this.answers = answers;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }
}
