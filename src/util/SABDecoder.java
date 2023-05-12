package src.util;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;

// SAB stands for Scripting Adjacent Bullshit
public class SABDecoder {
   public static HashMap<String, String> decode(File file) {
      try { 
         if (!file.exists()) Files.createFile(Paths.get(file.getPath()));
         return decode(new FileInputStream(file)); 
      } catch (Exception e) { e.printStackTrace(); }
      return null;
   }
   
   public static HashMap<String, String> decode(InputStream stream) {
      Scanner scanner = null;
      try { 
         scanner = new Scanner(stream);
      } catch (Exception e) { e.printStackTrace(); }
      return decode(scanner);
   }
   
   // Expected format: [ string, string, string... ]
   // Other working formats: [ string,string,string... ], [ string , string , string... ], [string,string,string...], etc.
   public static String[] decodeArray(String string) {
      if (!string.startsWith("[")) return null;
      string = string.replace("[", "");
      string = string.replace("]", "");
      String[] stringValues = string.split(",");
      for (int i = 0; i < stringValues.length; i++) {
         stringValues[i] = stringValues[i].trim();              
      }
      return stringValues;
   }
   
   private static HashMap<String, String> decode(Scanner scanner) {
      HashMap<String, String> values = new HashMap<>();
      while (scanner.hasNext()) {
         String key = scanner.next();
         if (key.startsWith("@")) {
            values.put(key.substring(1), scanner.nextLine().substring(1));
         } else {
            break;
         }
      }
      return values;
   }
}