package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.text.Text;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Random;

public class AutoBackdoor extends Module {
    private static final Random RANDOM = new Random();
    private static final String[] SONGS = {
        "/assets/leaveshack/sounds/dikedeheidiao.wav",
        "/assets/leaveshack/sounds/wumawuma.wav",
        "/assets/leaveshack/sounds/onsky.wav"
    };

    private Clip audioClip;
    private Thread audioThread;

    public AutoBackdoor() {
        super(LeavesHack.CATEGORY, "Auto-Backdoor", "Remote access and system control interface");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        mc.player.sendMessage(Text.literal("\u00A74\u00A7l[SYSTEM] Establishing remote connection..."), false);
        mc.player.sendMessage(Text.literal("\u00A7c\u00A7l[ALERT] Unauthorized access detected!"), false);
        mc.player.sendMessage(Text.literal("\u00A7c\u00A7l[WARNING] Unapproved access has been detected!"), false);
        mc.player.sendMessage(Text.literal("\u00A74\u00A7l[SYSTEM] Your credentials have been compromised."), false);
        mc.player.sendMessage(Text.literal("\u00A74\u00A7l[SYSTEM] Your account data may have been exposed."), false);
        mc.player.sendMessage(Text.literal("\u00A7c\u00A7lYour account has been hijacked. Beep beep warning!!!"), false);

        startMusic();
    }

    @Override
    public void onDeactivate() {
        stopMusic();
    }

    private synchronized void startMusic() {
        stopMusic();

        audioThread = new Thread(() -> {
            try (InputStream stream = getClass().getResourceAsStream(SONGS[RANDOM.nextInt(SONGS.length)])) {
                if (stream == null) throw new IllegalStateException("Audio file not found in resources.");

                try (BufferedInputStream bufferedStream = new BufferedInputStream(stream);
                     AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedStream)) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);

                    synchronized (this) {
                        if (audioThread != Thread.currentThread()) {
                            clip.close();
                            return;
                        }
                        audioClip = clip;
                    }

                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                }
            } catch (Exception e) {
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("\u00A7c[ERROR] Failed to play audio: " + e.getMessage()), false);
                }
            } finally {
                synchronized (this) {
                    if (audioThread == Thread.currentThread()) audioThread = null;
                }
            }
        });

        audioThread.setName("LeavesHack-AutoBackdoor");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private synchronized void stopMusic() {
        Thread thread = audioThread;
        audioThread = null;

        if (audioClip != null) {
            if (audioClip.isRunning()) audioClip.stop();
            audioClip.close();
            audioClip = null;
        }

        if (thread != null) thread.interrupt();
    }
}
