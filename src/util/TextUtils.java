package src.util;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

import src.TileEditor;

public final class TextUtils {
   
   private static final Font GAME_FONT = new Font(Font.SERIF, Font.PLAIN, 1);
   
   public static Rectangle drawText(Graphics2D g, int x, int y, float size, String text, Color defaultColor, int align) {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(defaultColor);
      if (g.getFont() != GAME_FONT) g.setFont(GAME_FONT);
      g.setFont(g.getFont().deriveFont(size));
      Rectangle rect = getTextRectangle(g, x, y, size, text, align);
      g.drawString(text, rect.x, rect.y + (rect.height / 2 + 4));
      return rect;
   }
   
   public static Rectangle drawWrappingText(Graphics2D g, int x, int y, float size, int maxX, String text, Color defaultColor, int align) {
      String[] splitString = text.split(" ");
      
      g.setFont(new Font(Font.SERIF, Font.PLAIN, 1));
      g.setFont(g.getFont().deriveFont(size));
      
      FontMetrics metrics = g.getFontMetrics(g.getFont());
      
      List<String> wrappedString = new ArrayList<>();
      // int lineWidth = x;
      String line = "";
      boolean firstLine = true;
      
      for (String string : splitString) {
         string += " ";
         int newLineWidth = metrics.stringWidth(line + string);
         // System.out.println(line + string + ", " + newLineWidth);
         
         if (string.contains("\n")) {
            String[] newLineBreak = string.split("\n");
            line += newLineBreak[0];
            wrappedString.add(line);
            for (int i = 1; i < newLineBreak.length; i++) {
               if (i == newLineBreak.length - 1) {
                  line = "";
                  line += newLineBreak[i];
               } else {
                  wrappedString.add(newLineBreak[i]);
               }
            }
            continue;
         }
         
         if (newLineWidth > maxX - x) {
            if (firstLine && line.equals("")) {
               firstLine = false;
               continue;
            }
            if (line.endsWith(" ")) line = line.substring(0, line.length() - 1);
            wrappedString.add(line);
            line = string;
            // lineWidth = x;
            firstLine = false;
            continue;
         }
         line += string;
         // lineWidth = newLineWidth;
      }
      if (line.endsWith(" ")) line = line.substring(0, line.length() - 1);
      wrappedString.add(line);
      
      int height = metrics.getHeight();
      int rectHeight = 0;
      int minX = 100000;
      int maxWidth = 0;
      
      for (int i = 0; i < wrappedString.size(); i++) {
         String drawLine = wrappedString.get(i);
         rectHeight += height + 4;
         Rectangle rect = drawText(g, x, y + i * height + 4, size, drawLine, defaultColor, align);
         if (rect.x < minX) minX = rect.x;
         if (rect.width > maxWidth) maxWidth = rect.width;
      }
      
      return new Rectangle(minX, y, maxWidth, rectHeight);
   }
   
   public static Rectangle drawWrappingText(Graphics2D g, int x, int y, float size, int maxX, String text, Color defaultColor) {
      return drawWrappingText(g, x, y, size, maxX, text, defaultColor, -1);
   }
   
   public static Rectangle drawWrappingText(Graphics2D g, int x, int y, float size, String text, Color defaultColor) {
      return drawWrappingText(g, x, y, size, TileEditor.program.getWidth() - 96, text, defaultColor, -1);
   }
   
   public static Rectangle getTextRectangle(Graphics2D g, int x, int y, float size, String text, int align) {
      g.setFont(g.getFont().deriveFont(size));
      FontMetrics metrics = g.getFontMetrics(g.getFont());
      Rectangle rect = new Rectangle(x, y, metrics.stringWidth(text), (int) metrics.getHeight());
      if (align == 0) {
         rect.x -= rect.width / 2;
      } else if (align > 0) {
         rect.x -= rect.width;
      }
      return rect; 
   }
}