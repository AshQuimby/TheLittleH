// package src;
// 
// import java.util.*;
// import java.awt.image.BufferedImage;
// import java.awt.Color;
// import java.awt.image.WritableRaster;
// import java.awt.image.ColorModel;
// import java.io.BufferedInputStream;
// import java.io.DataInputStream;
// import java.io.File;
// import java.io.FileInputStream;
// import java.io.IOException;
// import java.io.InputStream;
// import javax.sound.sampled.*;
// 
// public class SoundEngine implements LineListener {
//    private static HashMap<String, Clip> cache = new HashMap();
//    // public static AudioFormat format;
//    public static Clip currentSong;
//    // private Mixer mixer;
// 
//    public void load() {
//       // format = getAudioStream("menu_song.wav").getFormat();
//       currentSong = null;
//    }
//    
//    public void playMusic(String key) {
//       playMusic(key, true);
//    }
//    
//    public void playMusic(String key, boolean loop) {
//       if (true) return;
//       if (currentSong != null) {
//          currentSong.stop();
//          currentSong.close();
//       }
//       
//       // Only fully stop music if key is null
//       if (key == null) return;
// 
//       Clip clip = getClip(key);
//       if (clip.isOpen()) {
//          clip.stop();
//          clip.setFramePosition(0);
//       }
//       clip.loop(loop ? -1 : 0);
//       currentSong = clip;
//       clip.start();
//    }
//    
//    public void playSound(String key) {
//       if (true) return;
//       Clip clip = getClip(key);
//       if (clip.isOpen()) {
//          clip.stop();
//          clip.setFramePosition(0);
//       }
//       clip.start();
//    }
//    
//    public Clip getClip(String key) {
//       AudioInputStream stream = getAudioStream(key);
//       if (getAudioStream(key) == null) {
//          System.out.println("Failed loading sound with key \"" + key + "\". Sound does not exist.");
//       }
//       if (cache.containsKey(key)) {
//          return cache.get(key);
//       }
//       try {
//          Clip audioClip = AudioSystem.getClip();
//          audioClip.addLineListener(this);
//          audioClip.open(getAudioStream(key));
//          cache.put(key, audioClip);
//          return audioClip;
//       } catch (LineUnavailableException | IOException e) {
//          System.out.println("Failed loading sound with key \"" + key + "\". Could not load Clip.");
//          throw new RuntimeException(e);
//       }
//    }
//    
//    private static AudioInputStream getAudioStream(String key) {
//       key = "assets/sounds/" + key;
//       
//       AudioInputStream sound = null;
//       
//       if (Images.inArchive) {
//          InputStream in = SoundEngine.class.getResourceAsStream("/" + key);
//          if (in == null) return null;
//          try { 
//             sound = AudioSystem.getAudioInputStream(in);
//          } catch (Exception e) {
//             return null;
//          }
//          return sound;
//       }
//       
//       File file = new File(key);
//       if (!file.exists()) return null;
//       try {
//          sound = AudioSystem.getAudioInputStream(file);
//       } catch (Exception e) { 
//          e.printStackTrace();
//          return null; 
//       }
//       return sound;
//    }
//    
//    @Override
//     public void update(LineEvent event) {
//         if (LineEvent.Type.START == event.getType()) {
//         } else if (LineEvent.Type.STOP == event.getType()) {
//         }
//     }
//    
//    public static void updateCache() {
//       cache.clear();
//    }
// }