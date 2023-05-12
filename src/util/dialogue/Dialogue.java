package src.util.dialogue;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import src.TileEditor;
import src.Images;
import src.util.TextUtils;

public class Dialogue {
   private String[] text;
   private int atPosition;
   private Font font;
   private String[] characterNames;
   private String[] fileNames;
   private String lastBlock;
   private int atBlock;
   private int waitFor;
   private boolean finished;
   private boolean fastBlock;
   
   public Dialogue(String[] text, String[] characterNames, String[] fileNames) {
      this.text = text;
      atPosition = 0;
      atBlock = 0;
      this.characterNames = characterNames;
      this.fileNames = fileNames;
      waitFor = 0;
      finished = false;
   }
   
   public String getPortrait() {
      return fileNames[Integer.parseInt(text[atBlock].substring(0, 1)) - 1];
   }
   
   public void toEnd() {
      atPosition = text[atBlock].length() - 2;
   }
   
   public String getName() {
      return characterNames[Integer.parseInt(text[atBlock].substring(0, 1)) - 1];
   }
   
   public void nextBlock() {
      if (atBlock + 1>= text.length) {
         finished = true;
         return;
      }
      fastBlock = false;
      atPosition = 0;
      atBlock++;
   }
   
   public boolean finishedBlock() {
      return atPosition >= text[atBlock].length() - 1;
   }
   
   public boolean finished() {
      return finished;
   }
   
   public String next() {
      if (waitFor > 0 || finishedBlock()) {
         waitFor--;
         return lastBlock;
      }
      atPosition++;
      String next = text[atBlock].substring(Math.min(atPosition, 1), atPosition + 1);
//       Graphics2D g = (Graphics2D) TileEditor.program.getGraphics();
//       FontMetrics metrics = g.getFontMetrics(g.getFont());
// 
//       ArrayList<String> cutText = new ArrayList<String>();
//       String[] splitString = next.split(" ");
//       String oldLine = "";
//       String line = "";
//       for (int i = 0; i < splitString.length; i++) {
//          String string = splitString[i];
//          line += string + " ";
//          
//          int rawLength = line.length();
//          
//          line = line.replace("_", "");
//          
//          waitFor = (rawLength - line.length()) * 2;
//          
//          int width = metrics.stringWidth(line);
//          if (width > TileEditor.program.getWidth() - 128 || i == splitString.length - 1) { 
//             if (i != splitString.length - 1) i--;
//             cutText.add(width <= 56 || i == splitString.length - 1 ? line : oldLine);
//             line = "";
//          }
//          oldLine = line;
//       }
//       String[] toReturn = new String[cutText.size()];
//       for (int i = 0; i < cutText.size(); i++) {
//          toReturn[i] = cutText.get(i);
//       }
      lastBlock = next;
      return next;
   }
   
   public void render(Graphics g) {
      TileEditor.drawMenuBox(g, new Rectangle(0, TileEditor.program.getHeight() - 128, 128, 128));
      TileEditor.drawMenuBox(g, new Rectangle(128, TileEditor.program.getHeight() - 128, TileEditor.program.getWidth() - 128, 128));
      if (getPortrait().contains("player")) {
         g.drawImage(Images.hsvEffect(Images.getImage(getPortrait() + "_color.png"), TileEditor.hHue, 1, 1, getPortrait() + "_" + TileEditor.hHue + "_color.png"), 0, (int) TileEditor.program.getHeight() - 128, 128, 128, TileEditor.program);
         g.drawImage(Images.getImage(getPortrait() + ".png"), 0, (int) TileEditor.program.getHeight() - 128, 128, 128, TileEditor.program);
      } else {
         g.drawImage(Images.getImage(getPortrait()), 0, (int) TileEditor.program.getHeight() - 128, 128, 128, TileEditor.program);
      }
      TileEditor.drawMenuFrame(g, new Rectangle(0, TileEditor.program.getHeight() - 128, 128, 128));
      String dialogue = next();

      TextUtils.drawWrappingText((Graphics2D) g, 140, (int) TileEditor.program.getHeight() - 112, 28, dialogue, new Color(255, 255, 255));
   }
}