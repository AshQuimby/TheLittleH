package src.util;

import src.Images;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Random;

public class Settings {
   private static Map<String, String> settings = new HashMap<>();
   private static File file;
   
   public static void load() {
      file = new File((Images.inArchive ? "./" : "../") + "settings.sab");
      settings = SABDecoder.decode(file);
      String[] keys = new String[]{ "h_hue", "dark_mode", "default_background_visibility", "old_look_and_feel", "nickname", "music_volume", "sfx_volume", "parallax" };
      boolean foundDiscrepancy = false;
      if (settings.isEmpty()) {
         resetSettings();
         saveSettings();
         return;
      }
      for (String key : keys) {
         if (!settings.containsKey(key)) {
            System.out.println("Found missing setting: " + key + ". Setting to default.");
            settings.put(key, getDefaultValue(key));
            foundDiscrepancy = true;
         }
      }
      if (foundDiscrepancy) saveSettings();
   }
   
   public static void resetSettings() {
      String[] keys = new String[]{ "h_hue", "dark_mode", "default_background_visibility", "old_look_and_feel", "nickname", "music_volume", "sfx_volume", "parallax" };
      for (String key : keys) {
         settings.put(key, getDefaultValue(key));
      }
   }
   
   public static String getDefaultValue(String key) {
      switch (key) {
         case "h_hue" :
            return "0";
         case "dark_mode":
         case "parallax":
            return "true";
         case "old_look_and_feel":
         case "debug_mode":
         case "default_background_visibility":
            return "false";
         case "nickname" :
            return getRandomName();
         // case "master_volume":
         case "music_volume":
         case "sfx_volume":
            // May change to a float system
            return "true";
         default :
            return null;
      }
   }
   
   public static String getRandomName() {
      Random random = new Random(System.currentTimeMillis());
      String[] prefixes = new String[]{ "The Big", "The Little", "The Medium", "The Colossal", "The Microscopic", "The Minuscule" };
      String[] bases = new String[]{ "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
      String[] suffixes = new String[]{ "Fan", "Enjoyer", "Appreciator", "Stan", "Hater", "Critic", "Scorner", "Murderer", "Elater", "Skeptic" };
      return prefixes[random.nextInt(prefixes.length)] + " " + bases[random.nextInt(bases.length)] + " " + suffixes[random.nextInt(suffixes.length)];
   }
   
   public static void setSetting(String key, String value) {
      if (settings.containsKey(key)) {
         settings.replace(key, value);
      } else {
         settings.put(key, value);
      }
      saveSettings();
   }
   
   public static String getStringSetting(String key) {
      if (!settings.containsKey(key)) {
         String defaultValue = getDefaultValue(key);
         if (defaultValue != null) {
            settings.put(key, defaultValue);
            saveSettings();
            return defaultValue;
         } else {
            throw new RuntimeException(key + "! That settings key doesn't exits, you idot!!! >:(");
         }
      }
      return settings.get(key);
   }
   
   public static boolean getBooleanSetting(String key) {
      return Boolean.parseBoolean(getStringSetting(key));
   }
   
   public static float getFloatSetting(String key) {
      return Float.parseFloat(getStringSetting(key));
   }
   
   public static int getIntSetting(String key) {
      return Integer.parseInt(getStringSetting(key));
   }
   
   public static void saveSettings() {
      SoundEngine.updateVolumeSettings();
      if (getBooleanSetting("music_volume")) {
         if (!SoundEngine.isMusicPlaying()) {
            SoundEngine.playMusic("music/menu_song.wav");
         }
      } else {
         SoundEngine.stopMusic();
      }
      FileWriter writer = null;
      try {
         file.delete();
         writer = new FileWriter(file); 
         for (String key : settings.keySet()) {
            writer.write("@" + key + " " + settings.get(key) + "\n");
            // System.out.println(key + " " + settings.get(key));
         }
         writer.close();
      } catch (Exception e) {
         resetSettings();
         e.printStackTrace(); 
      }
   }
}