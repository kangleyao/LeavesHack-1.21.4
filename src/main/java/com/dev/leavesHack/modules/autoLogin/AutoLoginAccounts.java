package com.dev.leavesHack.modules.autoLogin;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

public class AutoLoginAccounts extends System<AutoLoginAccounts> {
    private final List<AutoLoginAccount> accounts = new ArrayList<>();

    public AutoLoginAccounts() {
        super("autologin-accounts");
    }

    public static AutoLoginAccounts get() {
        return Systems.get(AutoLoginAccounts.class);
    }

    public List<AutoLoginAccount> getAccounts() {
        return accounts;
    }

    public void add(AutoLoginAccount acc) {
        accounts.add(acc);
        save();
    }

    public void remove(AutoLoginAccount acc) {
        accounts.remove(acc);
        save();
    }

    public void saveNow() {
        save();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        NbtList list = new NbtList();

        for (AutoLoginAccount acc : accounts) {
            NbtCompound accountTag = new NbtCompound();
            accountTag.putString("username", acc.username.get());
            accountTag.putString("ip", acc.serverIp.get());
            accountTag.putString("password", acc.password.get());
            list.add(accountTag);
        }

        tag.put("accounts", list);
        return tag;
    }

    @Override
    public AutoLoginAccounts fromTag(NbtCompound tag) {
        accounts.clear();

        NbtList list = tag.getList("accounts", 10);
        for (NbtElement element : list) {
            NbtCompound accountTag = (NbtCompound) element;

            AutoLoginAccount acc = new AutoLoginAccount();
            acc.username.set(accountTag.getString("username"));
            acc.serverIp.set(accountTag.getString("ip"));
            acc.password.set(accountTag.getString("password"));
            accounts.add(acc);
        }

        return this;
    }
}
