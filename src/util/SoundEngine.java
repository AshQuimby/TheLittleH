package src.util;

import static javax.sound.sampled.AudioSystem.*;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;

public class SoundEngine {
    public static final boolean inArchive = SoundEngine.class.getResourceAsStream("SoundEngine.class").toString().startsWith("jar");
    private static HashMap<String, Clip> cache = new HashMap<>();
    public static AudioPiece currentMusic = null;
    public static int musicVolume;
    public static int sfxVolume;
    public static int masterVolume;

    public static void load() {
        updateVolumeSettings();
    }

    public static void updateVolumeSettings() {
        musicVolume = Settings.getBooleanSetting("music_volume") ? 175 : 0;
        sfxVolume = Settings.getBooleanSetting("sfx_volume") ? 50 : 0;
        masterVolume = 50;
    }

    private static LineListener listener = event -> {
        if (event.getType() == LineEvent.Type.STOP) {
            event.getLine().close();
        }
    };

    public static void playSound(String filePath) {
        if (masterVolume * sfxVolume <= 0f) return;
        if (cache.containsKey(filePath)) {
            Clip clip = cache.get(filePath);
            clip.setMicrosecondPosition(0);
            clip.start();
        }
        new AudioPiece(getActualPath(filePath));
    }

    public static void playMusic(String filePath) {
        if (masterVolume * musicVolume <= 0f) return;
        if (cache.containsKey(filePath)) {
            Clip clip = cache.get(filePath);
            clip.setMicrosecondPosition(0);
            clip.start();
        }
        new AudioPiece(getActualPath(filePath), true);
    }

    public static String getActualPath(String filePath) {
        return (inArchive ? "/" : "/") + "assets/sounds/" + filePath;
    }

    public static void stopMusic() {
        if (currentMusic != null) {
            currentMusic.clip.stop();
            currentMusic.clip.close();
            currentMusic = null;
        }
    }

    public static boolean isMusicPlaying() {
        return currentMusic != null;
    }

    public static class AudioPiece {

        public Clip clip;
        public AudioInputStream ais;

        private AudioPiece(String stream, boolean music) {
            try {
                if (music) stopMusic();
                InputStream streamIn = getClass().getResourceAsStream(stream);
                ais = getAudioInputStream(new BufferedInputStream(streamIn));
                clip = getClip();
                clip.open(ais);
                FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                if (music) {
                    volume.setValue(-musicVolume * masterVolume / 1000f);
                    clip.loop(-1);
                    currentMusic = this;
                } else {
                    volume.setValue(-sfxVolume * masterVolume / 1000f);
                }
                clip.addLineListener(listener);
                cache.put(stream, clip);
                clip.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private AudioPiece(String stream) {
            this(stream, false);
        }
    }
}