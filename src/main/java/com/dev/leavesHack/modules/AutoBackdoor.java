package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.text.Text;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.util.Random;

public class AutoBackdoor extends Module {
    private Clip audioClip;
    private Thread audioThread;

    public AutoBackdoor() {
        super(LeavesHack.CATEGORY, "Auto-Backdoor", "Remote access and system control interface");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        mc.player.sendMessage(Text.literal("§4§l[SYSTEM] Establishing remote connection..."), false);
        mc.player.sendMessage(Text.literal("§c§l[ALERT] Unauthorized access detected!"), false);
        mc.player.sendMessage(Text.literal("§c§l[警告] 检测到未授权访问！"), false);
        mc.player.sendMessage(Text.literal("§4§l[SYSTEM] Your credentials have been compromised."), false);
        mc.player.sendMessage(Text.literal("§4§l[系统] 你的凭证已被窃取。"), false);
        mc.player.sendMessage(Text.literal("§c§l你的账号已被盗取 咕咕嘎嘎 咕咕嘎嘎！！！"), false);

        startMusic();
    }

    @Override
    public void onDeactivate() {
        stopMusic();
    }

    private void startMusic() {
        audioThread = new Thread(() -> {
            try {
                String[] songs = {
                    "/assets/leaveshack/sounds/dikedeheidiao.wav",
                    "/assets/leaveshack/sounds/wumawuma.wav",
                    "/assets/leaveshack/sounds/onsky.wav"
                };
                String selectedSong = songs[new Random().nextInt(songs.length)];

                var stream = getClass().getResourceAsStream(selectedSong);
                if (stream == null) throw new Exception("Audio file not found in resources");
                BufferedInputStream bufferedStream = new BufferedInputStream(stream);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedStream);
                audioClip = AudioSystem.getClip();
                audioClip.open(audioStream);
                audioClip.loop(Clip.LOOP_CONTINUOUSLY);
                audioClip.start();
            } catch (Exception e) {
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("§c[ERROR] Failed to play audio: " + e.getMessage()), false);
                }
            }
        });
        audioThread.start();
    }

    private void stopMusic() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            audioClip.close();
        }
        if (audioThread != null) {
            audioThread.interrupt();
        }
    }
}
