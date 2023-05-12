package src;

import java.util.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class Images {
   public static boolean inArchive;
   public static HashMap<String, BufferedImage> cache = new HashMap<>();

   public static void load() {
      inArchive = !new File("assets/images/loading.png").exists();
   }
   
   public static BufferedImage getImage(String key) {
      BufferedImage image = null;
         
      if (key.startsWith(".")) {
         if (cache.containsKey(key)) {
            return cache.get(key);
         }
      } else {
         key = "assets/images/" + key;
         if (cache.containsKey(key)) {
            return cache.get(key);
         }
         
         if (inArchive) {
            InputStream in = Images.class.getResourceAsStream("/" + key);
            if (in == null) return null;
            try { image = ImageIO.read(in); } catch (IOException e) { return null; }
            cache.put(key, image);
            return image;
         }
      }
      
      File file = new File(key);
      if (!file.exists()) return null;
      try { 
         image = ImageIO.read(file); 
      } catch (IOException e) { 
         return null; 
      }
      cache.put(key, image);
      return image;
   }
   
   // Hue shift is in degrees
   public static BufferedImage hsvEffect(BufferedImage image, float hueShift, float saturationMult, float brightnessMult, String saveKey) {
        if (cache.containsKey(saveKey)) return cache.get(saveKey);
        BufferedImage colorImage = createCopy(image);
        for (int i = 0; i < colorImage.getWidth(); i++) {
            for (int j = 0; j < colorImage.getHeight(); j++) {
                Color newColor = new Color(colorImage.getRGB(i, j), true);
                int alpha = newColor.getAlpha();
                float[] hsv = new float[3];
                Color.RGBtoHSB(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), hsv);
                hsv[0] = (hsv[0] * 360 + hueShift) / 360 % 1;
                hsv[1] *= saturationMult;
                hsv[2] *= brightnessMult;
                newColor = Color.getHSBColor(hsv[0], hsv[1], hsv[2]);
                newColor = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), alpha);
                colorImage.setRGB(i, j, newColor.getRGB());
            }
        }
        cache.put(saveKey, colorImage);
        return colorImage;
    }
    
    private static BufferedImage createCopy(BufferedImage image) {
        ColorModel cm = image.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
   
   public static void updateCache() {
      cache.clear();
   }
   
   public static void drawImage(Graphics2D g, BufferedImage image, Rectangle drawTo, Rectangle drawFrom) {
      g.drawImage(image, drawTo.x, drawTo.y, drawTo.x + drawTo.width, drawTo.y + drawTo.height, drawFrom.x, drawFrom.y, drawFrom.x + drawFrom.width, drawFrom.y + drawFrom.height, TileEditor.window);
   }
}