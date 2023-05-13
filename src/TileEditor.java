package src;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Scanner;
import java.util.Random;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.awt.event.MouseWheelEvent;

import src.map.Map;
import src.map.GameState;
import src.map.PlayState;
import src.map.CampaignState;
import src.map.Tile;
import src.map.menu.Menu;
import src.util.MouseUtils;
import src.util.SoundEngine;
import src.util.TextUtils;
import src.util.Settings;
import src.util.SABDecoder;
import src.util.dialogue.Dialogues;

public class TileEditor extends JPanel implements ActionListener, KeyListener, MouseListener, MouseWheelListener {
   // private static int fps;
   // private static long lastCheckedFps;
   private static long lastFrame;
   // private static int fpsCount;
   public static final String VERSION = "0.9";
   public static final String WINDOW_TITLE = "The Little H";
   public static final Color DARK_GRAY = new Color(0.075f, 0.075f, 0.075f, 1);
   public static final Color LIGHT_GRAY = new Color(0.9f, 0.9f, 0.9f, 1);
   public static final java.util.Map<String, Tile> customTiles = new HashMap<>(); 
   public static int hHue;
   public static boolean hardPause;
   public static JFrame window;
   public static TileEditor program;
   public static GameState game;
   public static Timer timer;
   private static boolean lookingForMap;
   private boolean onTitle;
   public String info;
   private static String levelName;
   private static String levelDisplayName;
   private boolean removeMap;
   public static boolean darkMode = true;
   public static boolean inArchive;
   
   // public static final SoundEngine soundEngine = new SoundEngine();
   public static Cursor hCursorLight;
   public static Cursor hCursorDark;
   public static Cursor blankCursor;
   public static String mapsFolderPath;
   public static String tbhlFolderPath;
   private static String[] foundMaps;
   public int tick;
   public static int fileMenuIndex;
   private static int selectedMapIndex;
   
   public Menu<String> currentMenu;
   public static Menu<String> fileMenu;
   private static long millisecond, lastCheckedFps;
   private static int fps, fpsCount, timerGoal;
   
   private static MenuType mainMenu;
   
   public TileEditor() {
      lookingForMap = true;
      levelName = "";
      levelDisplayName = "";
      onTitle = true;
      tick = 0;
      fileMenuIndex = 0;
      selectedMapIndex = -1;
      mainMenu = new MenuType.Default();
      
      // map = new Map("test.map");
   }
   
   public static void main(String[] args) {
      Images.load();
      Settings.load();
      SoundEngine.load();
      hardPause = false;

      darkMode = Settings.getBooleanSetting("dark_mode");
      hHue = Settings.getIntSetting("h_hue");
      // soundEngine.load();
      // soundEngine.playMusic("menu_song.wav", true);
      inArchive = Images.inArchive;
      mapsFolderPath = inArchive ? "./maps" : "../maps";
      tbhlFolderPath = inArchive ? "./maps" : "../maps";
      File mapsFolder = new File(mapsFolderPath);
      try { if (!mapsFolder.exists()) Files.createDirectory(Paths.get(mapsFolder.getPath())); } catch (Exception e) { e.printStackTrace(); }
      foundMaps = new File(mapsFolderPath).list();
      window = new JFrame(TileEditor.WINDOW_TITLE + "");
      window.setPreferredSize(new Dimension(1040, 615));
      program = new TileEditor();
      window.setIconImage(Images.getImage("icon.png"));
      hCursorLight = Toolkit.getDefaultToolkit().createCustomCursor(Images.getImage("ui/cursor/light.png"), new Point(0, 0), "H!");
      hCursorDark = Toolkit.getDefaultToolkit().createCustomCursor(Images.getImage("ui/cursor/dark.png"), new Point(0, 0), "H_DARK!");
      blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(Images.getImage("ui/cursor/blank.png"), new Point(0, 0), "BLANK!");
      window.add(program);
      window.addKeyListener(program);
      window.addMouseListener(program);
      window.addMouseWheelListener(program);
      window.pack();
      window.setVisible(true);
      window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      window.setExtendedState(JFrame.MAXIMIZED_BOTH);
      reset();
      timerGoal = 0;
      timer = new Timer(timerGoal, program);
      timer.start();
//    
      // This method can impact performance on incredibly slow devices, but also syncs framerate better than java's timer
      // while (true) {
//          long millisecond = System.currentTimeMillis();
//          if (millisecond > lastFrame + 16) {
//             lastFrame = millisecond - (int) (millisecond % 16);
//             // fpsCount++;
//             // System.out.println("1");
//             program.update();
//             
//             // try { Thread.sleep(1); } catch (Exception e) { e.printStackTrace(); }
//             finishedRender = false;
//             program.repaint();
//             // Why does screen tear, they aren't on seperate threads :(
//          }
//          millisecond = System.currentTimeMillis();
//          if (lastFrame + 16 - millisecond <= 0) continue;
//          // The smaller the "- 4" the better the framerate syncing but the worse the performance
//          try { Thread.sleep(Math.max(0, lastFrame + 16 - millisecond - 1)); } catch (Exception e) { e.printStackTrace(); }
//       }
   }
   
   public static void reset() {
      SoundEngine.playMusic("music/menu_song.wav");
      levelName = "";
      levelDisplayName = "";
      lookingForMap = true;
      game = null;
      Tile.clearTagsCache();
      window.setTitle(TileEditor.WINDOW_TITLE + "");
      Images.updateCache();
      foundMaps = new File(mapsFolderPath).list();
      updateFileMenu();
      mainMenu = new MenuType.Default();
      Dialogues.resetDialogues();
      loadCustomTiles();
   }
   
   public static void loadCustomTiles() {
      try {
         File packsFolder = new File((inArchive ? "./" : "../") + "tile_packs");
         try {
            if (!packsFolder.exists()) Files.createDirectory(Paths.get(packsFolder.getPath()));
         } catch (Exception e) {
            e.printStackTrace();
         }
         File[] packs = packsFolder.listFiles();
         Set<Tile> tiles = new HashSet<>();
         for (File pack : packs) {
            if (pack.isDirectory()) {
               File scriptsFolder = new File(pack.getPath().replace("\\", "/") + "/scripts");
               tiles.addAll(searchForScripts(scriptsFolder));
            }
         }
         for (Tile tile : tiles) {
            customTiles.put(tile.image, tile);
            // System.out.println(tile.image);
         }
      } catch (Exception e) {
         System.out.println("One or more tile packs failed to load");
      }
   }

   public static void playBlip() {
      SoundEngine.playSound("effects/blip.wav");
   }
   
   public static Set<Tile> searchForScripts(File startFile) {
      Set<Tile> tiles = new HashSet<>();
      for (File file : startFile.listFiles()) {
         if (file.getName().endsWith(".sab")) {
            java.util.Map<String, String> properties = SABDecoder.decode(file);
            Tile tile = new Tile(startFile.getPath().replace("\\", "/").replace("scripts", "images") + "/" + properties.get("image"), SABDecoder.decodeArray(properties.get("tags")));
            tiles.add(tile);
         } else if (file.isDirectory()) {
            searchForScripts(file);
         }
      }
      return tiles;
   }
   
   public static File askForFile() {
      JFileChooser chooser = new JFileChooser(mapsFolderPath);
      try {
         UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
         SwingUtilities.updateComponentTreeUI(chooser);
      } catch (Exception e) {
         e.printStackTrace();
      }
      FileNameExtensionFilter filter = new FileNameExtensionFilter(".map files", "map");
      chooser.setFileFilter(filter);
      int returnVal = chooser.showOpenDialog(program);
      program.remove(chooser);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         return chooser.getSelectedFile();
      }
      return null;
   }
   
   public static void updateFileMenu() {

//       int n = 0;
//       String[] maps = new String[foundMaps.length];
//       for (int i = fileMenuIndex; i < foundMaps.length; i++) {
//          maps[n] = foundMaps[i];
//          n++;
//       }

      fileMenu = new Menu<>(foundMaps, 192, 64, 4);
      updateFileMenuRect();

//       int lastIndex = fileMenu.getLastIndexInBounds(new Rectangle(8, 0, program.getWidth() - 16, program.getHeight()));
//       n = 0;
//       maps = new String[lastIndex - fileMenuIndex];
//       for (int i = fileMenuIndex; i < fileMenuIndex + lastIndex; i++) {
//          if (i >= foundMaps.length) break;
//          maps[n] = foundMaps[i];
//          n++;
//       }
   }
   
   public static void updateFileMenuRect() {
      fileMenu.setMenuRectangle(64, 128, TileEditor.window.getHeight() - fileMenu.elementHeight * 3, false);
   }
   
//    @Override
//    public void actionPerformed(ActionEvent e) {
//       if (e.getSource() == timer) {
//       }
//    }
   
   @Override
   public void actionPerformed(ActionEvent e) {
      fpsCount++;
      millisecond = System.currentTimeMillis();
      if (millisecond > lastCheckedFps + 1000) {
         lastCheckedFps = millisecond - (int) (millisecond % 1000);
         fps = fpsCount;
         fpsCount = 0;
         if (fps != 0) {
            if (fps < 56) {
               // System.out.println(timer.getDelay() + ", " + fps);
               timerGoal--;
               if (fps < 50) timerGoal--;
               if (fps < 10) timerGoal -= 3;
            } else if (fps > 68) { 
               // System.out.println(timer.getDelay() + ", " + fps);
               timerGoal++;
            }
            timerGoal = Math.min(16, Math.max(1, timerGoal));
            timer.setDelay(timerGoal);
         }
      }
      if (hardPause) return;
      program.info = null;
      MouseUtils.update();
      tick++;
      updateFileMenu();
      if (removeMap) {
         game = null;
         lookingForMap = true;
         removeMap = false;
      }
      // Images.updateCache();
      if (game != null && game.map != null) {
         game.update();
      } else {
         mainMenu.update();
         if (selectedMapIndex != -1) {
            File file = new File(mapsFolderPath + "/" + fileMenu.items[selectedMapIndex]);
            playBlip();
            mainMenu = new MenuType.LevelOptions(file);
            selectedMapIndex = -1;
         }
      }
      repaint();
   }

   @Override
   public void paintComponent(Graphics g) {
      // System.out.println("2");
      super.paintComponent(g);
      
      if (!lookingForMap && game != null) {
         game.render((Graphics2D) g);
      } else {
         if (game == null || game.map == null || !GameState.finishedLoading) {
            drawMenuBox(g, new Rectangle(0, 0, TileEditor.program.getWidth(), TileEditor.program.getHeight()));
         }
         if (game == null) {
            g.drawImage(Images.hsvEffect(Images.getImage("title.png"), hHue, 1, 1, "title" + "_color" + hHue + ".png"), getWidth() / 2 - 284, getHeight() / 2 - 232, 568, 464, this);

            if (onTitle) {
               if (tick % 60 < 30) TextUtils.drawText((Graphics2D) g, getWidth() / 2, getHeight() / 6 * 5, 32, "Press Enter", getSecondaryColor(), 0);
            } else {
               mainMenu.render((Graphics2D) g);
            }
         } else {
            TextUtils.drawText((Graphics2D) g, program.getWidth() / 2, window.getHeight() / 2, 24, "File not found, enter name for new map: " + levelDisplayName, getSecondaryColor(), 0);
         }
      }

      if (info != null) {
         Point mousePos = MouseUtils.pointerScreenLocation();
         Rectangle rect = TextUtils.getTextRectangle((Graphics2D) g, mousePos.x + 24, mousePos.y, 20, info, -1);
         int yOffset = rect.y + 48 + 24 > program.getHeight() ? -48 : 48;
         boolean flipText = rect.x + rect.width > program.getWidth();
         rect = TextUtils.getTextRectangle((Graphics2D) g, mousePos.x + 24, mousePos.y + yOffset, 20, info, flipText ? 1 : -1);
         drawMenuBox(g, new Rectangle(rect.x - 16, rect.y - 8, rect.width + 32, rect.height + 16));
         TextUtils.drawText((Graphics2D) g, mousePos.x + 24, mousePos.y + yOffset, 20, info, getSecondaryColor(), flipText ? 1 : -1);
      }
      Toolkit.getDefaultToolkit().sync();
   }

//    @Override
//    public void actionPerformed(ActionEvent e) {
//    }

   public static void drawMenuButton(Graphics g, Rectangle rect) {
      if (Settings.getBooleanSetting("old_look_and_feel")) {
         g.setColor(getSecondaryColor());
         g.fillRect(rect.x, rect.y, rect.width, rect.height);
         g.setColor(getMainColor());
         g.fillRect(rect.x + 2, rect.y + 2, rect.width - 4, rect.height - 4);
         return;
      }
      rect = new Rectangle(rect);
      rect.width = Math.max(rect.width, 40);
      rect.height = Math.max(rect.height, 40);   
      g.drawImage(Images.getImage("ui/menu/menu_top" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y, rect.width - 16, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_base" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y + 8, rect.width - 16, rect.height - 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_bottom" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y + rect.height - 16, rect.width - 16, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_bottom" + (darkMode ? "_dark.png" : ".png")), rect.x, rect.y + rect.height - 24, 8, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_bottom" + (darkMode ? "_dark.png" : ".png")), rect.x + rect.width - 8, rect.y + rect.height - 24, 8, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y, rect.width - 24, 8, program);
      // g.drawImage(Images.getImage("ui/menu/menu_side.png"), rect.x + rect.width - 8, rect.y + rect.height - 24, 8, 8, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x, rect.y + 8, 8, rect.height - 24, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x + rect.width - 8, rect.y + 8, 8, rect.height - 24, program);
   }
   
   public static void drawFlatMenuBox(Graphics g, Rectangle rect) {
      if (Settings.getBooleanSetting("old_look_and_feel")) {
         g.setColor(getSecondaryColor());
         g.fillRect(rect.x, rect.y, rect.width, rect.height);
         g.setColor(getMainColor());
         g.fillRect(rect.x + 2, rect.y + 2, rect.width - 4, rect.height - 4);
         return;
      }
      rect = new Rectangle(rect);
      rect.width = Math.max(rect.width, 40);
      rect.height = Math.max(rect.height, 40);   
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y, rect.width - 16, 8, program);
      g.drawImage(Images.getImage("ui/menu/menu_base" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y + 8, rect.width - 16, rect.height - 24, program);
      g.drawImage(Images.getImage("ui/menu/menu_bottom" + (darkMode ? "_dark.png" : ".png")), rect.x, rect.y + rect.height - 16, rect.width, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x, rect.y, 8, rect.height - 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x + rect.width - 8, rect.y, 8, rect.height - 16, program);
   }

   public static void drawMenuBox(Graphics g, Rectangle rect) {
      if (Settings.getBooleanSetting("old_look_and_feel")) {
         g.setColor(getSecondaryColor());
         g.fillRect(rect.x, rect.y, rect.width, rect.height);
         g.setColor(getMainColor());
         g.fillRect(rect.x + 2, rect.y + 2, rect.width - 4, rect.height - 4);
         return;
      }
      rect = new Rectangle(rect);
      rect.width = Math.max(rect.width, 40);
      rect.height = Math.max(rect.height, 40);   
      g.drawImage(Images.getImage("ui/menu/menu_top" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y, rect.width - 16, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_base" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y + 16, rect.width - 16, rect.height - 32, program);
      g.drawImage(Images.getImage("ui/menu/menu_bottom" + (darkMode ? "_dark.png" : ".png")), rect.x, rect.y + rect.height - 16, rect.width, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x, rect.y, 8, rect.height - 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x + rect.width - 8, rect.y, 8, rect.height - 16, program);
   }
   
   public static void drawMenuFrame(Graphics g, Rectangle rect) {
      if (Settings.getBooleanSetting("old_look_and_feel")) {
         g.setColor(getSecondaryColor());
         g.fillRect(rect.x, rect.y, rect.width, rect.height);
         g.setColor(getMainColor());
         g.fillRect(rect.x + 2, rect.y + 2, rect.width - 4, rect.height - 4);
         return;
      }
      rect = new Rectangle(rect);
      rect.width = Math.max(rect.width, 40);
      rect.height = Math.max(rect.height, 40);
//       g.setColor(getSecondaryColor());
//       g.fillRect(rect.x - outlineWidth * 2, rect.y - outlineWidth * 2, rect.width + outlineWidth * 4, rect.height + outlineWidth * 4);
//       g.setColor(getMainColor());
//       g.fillRect(rect.x - outlineWidth, rect.y - outlineWidth, rect.width + outlineWidth * 2, rect.height + outlineWidth * 2);
      
      g.drawImage(Images.getImage("ui/menu/menu_top" + (darkMode ? "_dark.png" : ".png")), rect.x + 8, rect.y, rect.width - 16, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_bottom" + (darkMode ? "_dark.png" : ".png")), rect.x, rect.y + rect.height - 16, rect.width, 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x, rect.y, 8, rect.height - 16, program);
      g.drawImage(Images.getImage("ui/menu/menu_side" + (darkMode ? "_dark.png" : ".png")), rect.x + rect.width - 8, rect.y, 8, rect.height - 16, program);
   }
   
   public File fileFromName(String name) {
      return new File(mapsFolderPath + "/" + name);
   }

   public void returnFromEditor(Map map) {
      playBlip();
      mainMenu = new MenuType.SaveConfirmation(map);
   }

   public void setHoverInfo(String hoverInfo) {
      info = hoverInfo;
   }
   
   public String getLevelName(File file) {
      try {
         Scanner scanner = new Scanner(file);
         while (!scanner.hasNext("@name") || !scanner.hasNext()) {
            scanner.nextLine();
         }
         scanner.next();
         String name = scanner.nextLine();
         name = name.substring(1, name.length());
         scanner.close();
         return name;
      } catch (Exception e) {
      }
      return "|ERROR!|";
   }
   
   public String getLevelAuthor(File file) {
      try {
         Scanner scanner = new Scanner(file);
         while (!scanner.hasNext("@author") || !scanner.hasNext()) {
            scanner.nextLine();
         }
         scanner.next();
         String name = scanner.nextLine();
         name = name.substring(1, name.length());
         scanner.close();
         return name;
      } catch (Exception e) {
      }
      return "|ERROR!|";
   }
   
   public void setLevelName(String name, File file) {
      Map tempMap = new Map(file);
      tempMap.name = name;
   }
   
   public static void playLevel(String path, boolean jarResource) {
      if (jarResource) {
         game = new CampaignState(path, 0);
         lookingForMap = false;
      } else {
         game = new CampaignState(new File(path));
         lookingForMap = false;
      }
   }
   
   public static Color getBackgroundColor() {
      return darkMode ? Color.BLACK : Color.WHITE;
   }
   
   public static Color getMainColor() {
      return darkMode ? DARK_GRAY : LIGHT_GRAY;
   }
   
   public static Color getSecondaryColor() {
      return darkMode ? Color.WHITE : DARK_GRAY;
   }
   
   public static Color getMidtoneColor() {
      return darkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY; // darkMode ? Color.LIGHT_GRAY : Color.LIGHT_GRAY;
   }
   
   public static Color applyAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }
   
   public static void error(String message) {
      game = null;
      mainMenu = new MenuType.Error(message);
   }
   
   @Override
   public void keyTyped(KeyEvent e) {
      if (game != null) {
         game.keyPressed(e.getKeyCode(), e.getKeyChar(), true);
      }
   }
   
   @Override
   public void keyPressed(KeyEvent e) {
      // if (e.getKeyCode() == KeyEvent.VK_F1) hardPause = !hardPause;
      if (game == null) mainMenu.keyboardAction(e.getKeyCode(), e.getKeyChar(), false);

      if (onTitle) {
         if (e.getKeyCode() == KeyEvent.VK_ENTER) onTitle = false;
         return;
      } else if (lookingForMap) {
      } else if (game != null) {
         game.keyPressed(e.getKeyCode(), e.getKeyChar(), false);
      }
   }
   
   @Override
   public void keyReleased(KeyEvent e) {
      if (game == null) mainMenu.keyboardAction(e.getKeyCode(), e.getKeyChar(), true);
      else game.keyReleased(e.getKeyCode());
   }
   
   public void toMainMenu() {
      removeMap = true;
   }
   
   @Override
   public void mouseClicked(MouseEvent e) {
   }
   
   @Override
   public void mouseEntered(MouseEvent e) {
   }
   
   @Override
   public void mouseExited(MouseEvent e) {
   }
   
   @Override
   public void mousePressed(MouseEvent e) {
      if (game == null) mainMenu.mouseAction(e.getButton(), false);
      else game.mousePressed(e.getButton());
   }
   
   @Override
   public void mouseReleased(MouseEvent e) {
      if (game == null) mainMenu.mouseAction(e.getButton(), true);
      else game.mouseReleased(e.getButton());
   }
   
   @Override
   public void mouseWheelMoved(MouseWheelEvent e) {
      if (game != null && game.player == null) {
         game.camera.addZoom(e.getWheelRotation() / -16f);   
      }
   }
   
   private static class MenuType {
      
      public void update() {
      }
      
      public void mouseAction(int button, boolean released) {   
      }
      
      public void keyboardAction(int keyCode, char keyChar, boolean released) {   
      }
      
      public void render(Graphics2D g) {
      }
      
      public static final class Default extends MenuType {
         
         private Menu<String> menuButtons;
         private Menu<String> navigationButtons;
                  
         private static boolean secret = true; // new java.util.Random(System.currentTimeMillis()).nextFloat() > 0.5f;
         
         public Default() {
            // browseButton = new Rectangle(16, 16, 64, 64);
            // promptButton = new Rectangle(96, 16, 64, 64);
            menuButtons = new Menu<>(new String[]{ "ui/folder.png", "ui/prompt.png", "ui/settings_gear.png"/* darkMode ? "ui/light_off.png" : "ui/light_on.png" */, "ui/create.png", "ui/help.png" }, 64, 64, 8);
            navigationButtons = new Menu<>(new String[]{ "back_arrow.png", "forward_arrow.png" }, 64, 64, 8);
         }
         
         @Override
         public void update() {
            menuButtons.setMenuRectangle(16, 16, 1, false);
            navigationButtons.setMenuRectangle(program.getWidth() - 16, program.getHeight() - 104, 1, true);
            TileEditor.window.getContentPane().setCursor(darkMode ? hCursorDark : hCursorLight);
            if (foundMaps.length == 0) {
               mainMenu = new NoLevels();
            }
            updateFileMenuRect();
            int buttonIndex = menuButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
            switch (buttonIndex) {
               case 0 :
                  program.info = "Select a level using a file explorer";
                  break;
               case 1 :
                  program.info = "Use the legacy level selector";
                  break;
               case 2 :
                  program.info = "Settings";
                  break;
               case 3 :
                  program.info = "Create new level";
                  break;
               case 4 :
                  program.info = "Help";
                  break;
            }
            
            buttonIndex = navigationButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
            switch (buttonIndex) {
               case 0 :
                  program.info = "Previous level page";
                  break;
               case 1 :
                  program.info = "Next level page";
                  break;
            }
         }
         
         @Override
         public void mouseAction(int button, boolean released) {
            if (button == 1 && released) {
               selectedMapIndex = fileMenu.getOverlappedElement(MouseUtils.pointerScreenLocation());
               int buttonIndex = menuButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
               if (buttonIndex == 0) {
                  File file = askForFile();
                  playBlip();
                  if (file != null) mainMenu = new LevelOptions(file);
               } else if (buttonIndex == 1) {
                  playBlip();
                  mainMenu = new Legacy();
               } else if (buttonIndex == 2) {
                  playBlip();
                  mainMenu = new SettingsMenu();
                  // darkMode = !darkMode;
                  //   Settings.setSetting("dark_mode", "" + darkMode);
                  //   window.getContentPane().setCursor(darkMode ? hCursorDark : hCursorLight);
                  // menuButtons.items[buttonIndex] = darkMode ? "ui/light_off.png" : "ui/light_on.png";
               } else if (buttonIndex == 3) {
                  playBlip();
                  mainMenu = new Creation();
               } else if (buttonIndex == 4){
                  playBlip();
                  mainMenu = new Help();
               }
               
               buttonIndex = navigationButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
               if (buttonIndex == 0) {
                  fileMenuIndex--;
                  if (fileMenuIndex < 0) {
                     fileMenuIndex = fileMenu.items.length - 1;
                  }
               } else if (buttonIndex == 1) {
                  fileMenuIndex++;
                  if (fileMenuIndex >= fileMenu.items.length) {
                     fileMenuIndex = 0;
                  }
               }
            }
            
            if (button == 1 && released) {
               Rectangle cobweb = new Rectangle(program.getWidth() - 128, 0, 128, 128);
               if (cobweb.contains(MouseUtils.pointerScreenLocation())) {
                  playBlip();
                  mainMenu = new ButtonMenu();
               }
            }
         }
      
         @Override
         public void render(Graphics2D g) {
            fileMenu.setPosition(64, 128);
            Rectangle[] buttons = fileMenu.getItemButtons();
            for (int i = 0; i < fileMenu.items.length; i++) {
               drawMenuButton(g, buttons[i]);
               if (buttons[i].contains(MouseUtils.pointerScreenLocation())) {
                  g.setColor(new Color(255, 255, 255, 127));
                  g.fillRect(buttons[i].x, buttons[i].y, buttons[i].width, buttons[i].height);
               }
               g.setColor(getSecondaryColor());
               String levelName = program.getLevelName(program.fileFromName(fileMenu.items[i]));
               if (levelName.length() > 16) levelName = levelName.substring(0, 16) + "...";
               TextUtils.drawText((Graphics2D) g, (int) buttons[i].getCenterX(), (int) buttons[i].getCenterY() - 12, 20, levelName, getSecondaryColor(), 0);
            }
            
            buttons = menuButtons.getItemButtons();
            for (int i = 0; i < buttons.length; i++) {
               Rectangle button = buttons[i];
               button.height += 8;
               drawFlatMenuBox(g, button);
               button.height -= 8;
               g.drawImage(Images.getImage(menuButtons.items[i]), button.x, button.y, button.width, button.height, program);
               if (button.contains(MouseUtils.pointerScreenLocation())) {
                  g.setColor(new Color(255, 255, 255, 127));
                  g.fillRect(button.x, button.y, button.width, button.height);
               }
            }
            
            buttons = navigationButtons.getItemButtons();
            for (int i = 0; i < buttons.length; i++) {
               Rectangle button = buttons[i];
               button.height += 8;
               drawFlatMenuBox(g, button);
               button.height -= 8;
               g.drawImage(Images.getImage(navigationButtons.items[i]), button.x, button.y, button.width, button.height, program);
               if (button.contains(MouseUtils.pointerScreenLocation())) {
                  g.setColor(new Color(255, 255, 255, 127));
                  g.fillRect(button.x, button.y, button.width, button.height);
               }
            }
            
            if (secret) {
               g.drawImage(Images.getImage("ui/cobweb.png"), program.getWidth() - 128, 0, 128, 128, program);
            }
         }
      }
      
      public static final class ButtonMenu extends MenuType {
         
         // private Rectangle ballButton;
         private int buttonFrame;
         private boolean mouseDown;
         private boolean leftMouseDown;
         private boolean rightMouseDown;
         private boolean touchingButton;
         private int buttonHeldFor;
         
         public ButtonMenu() {
            buttonFrame = 0;
         }
         
         @Override
         public void update() {
            buttonFrame = 0;
            
            if (new Rectangle(program.getWidth() / 2 - 64, program.getHeight() / 2 - 64, 128, 136).contains(MouseUtils.pointerScreenLocation())) {
               if (mouseDown) {
                  buttonFrame = 2;
                  if (buttonHeldFor % 15 == 0) {
                     hHue += leftMouseDown ? 10 : -10;
                     Settings.setSetting("h_hue", "" + hHue);
                  }
                  buttonHeldFor++;
               } else { 
                  buttonFrame = 1;
                  buttonHeldFor = 0;
               }
               touchingButton = true;
            } else {
               buttonHeldFor = 0;
               touchingButton = false;
            }
         }
         
         @Override
         public void mouseAction(int button, boolean released) {
            if (released) {
               mouseDown = false;
               if (button == 1) leftMouseDown = false;
               if (button == 3) rightMouseDown = false;
            } else {
               mouseDown = true;
               if (button == 1) leftMouseDown = true;
               if (button == 3) rightMouseDown = true;
            }
         }
         
         @Override
         public void keyboardAction(int keyCode, char keyChar, boolean released) {
            if (released) {
               playBlip();
               mainMenu = new Default();
            }
         }
         
         @Override
         public void render(Graphics2D g) {
            g.setColor(getBackgroundColor());
            g.fillRect(0, 0, program.getWidth(), program.getHeight());
            Images.drawImage(g, Images.hsvEffect(Images.getImage("ui/big_button.png"), hHue, 1, 1, "ui/big_button" + "_color" + hHue + ".png"), new Rectangle(program.getWidth() / 2 - 64, program.getHeight() / 2 - 64, 128, 136), new Rectangle(0, 0 + 17 * buttonFrame, 16, 17));
         }
      }
      
      public static final class Error extends MenuType {
         
         private String message;
         
         public Error(String message) {
            this.message = message;
         }
               
         @Override
         public void render(Graphics2D g) {
            drawMenuBox(g, new Rectangle(program.getWidth() / 4, window.getHeight() / 4, program.getWidth() / 2, window.getHeight() / 2));
            TextUtils.drawText((Graphics2D) g, program.getWidth() / 2, window.getHeight() / 2, 24, message, getSecondaryColor(), 0);
         }
               
         @Override
         public void keyboardAction(int keyCode, char keyChar, boolean released) {
            if (released) {
               playBlip();
               mainMenu = new Default();
            }
         }
         
         @Override
         public void mouseAction(int button, boolean released) {
            if (released) {
               playBlip();
               mainMenu = new Default();
            }
         }
      }
      
      public static final class SaveConfirmation extends MenuType {

         private Menu<String> confirmButtons;
         private Map editedMap;

         public SaveConfirmation(Map map) {
            confirmButtons = new Menu<>(new String[]{ "Yes", "Return", "No" }, 256, 96, 48);
            this.editedMap = map;
         }
         
         @Override
         public void keyboardAction(int keyCode, char keyCar, boolean released) {
            switch (keyCode) {
               case KeyEvent.VK_ENTER :
                  editedMap.saveToFile();
                  reset();
            }
         }

         @Override
         public void mouseAction(int button, boolean released) {
            if (button == 1 && released) {
               int buttonIndex = confirmButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
               if (buttonIndex == 0) {
                  editedMap.saveToFile();
                  reset();
               } else if (buttonIndex == 1) {
                  lookingForMap = false;
                  game = editedMap;
               } else if (buttonIndex == 2) {
                  reset();
               }
            }
         }

         @Override
         public void render(Graphics2D g) {
            confirmButtons.setMenuRectangle(program.getWidth() / 2 - (304) * 3 / 2 - 24, program.getHeight() / 2, 1, false);
            Rectangle[] buttons = confirmButtons.getItemButtons();
            for (int i = 0; i < buttons.length; i++) {
               drawMenuButton(g, buttons[i]);
               if (buttons[i].contains(MouseUtils.pointerScreenLocation())) {
                  g.setColor(new Color(255, 255, 255, 127));
                  g.fillRect(buttons[i].x - 2, buttons[i].y - 2, buttons[i].width + 4, buttons[i].height + 4);
               }
               g.setColor(getSecondaryColor());
               TextUtils.drawText((Graphics2D) g, (int) buttons[i].getCenterX(), (int) buttons[i].getCenterY() - 12, 24, confirmButtons.items[i], getSecondaryColor(), 0);
            }

            String bigText = "Do you want to save before exiting?";
            
            drawMenuBox(g, new Rectangle (program.getWidth() / 4, program.getHeight() / 4, program.getWidth() / 2, program.getHeight() / 4));
            
            TextUtils.drawText((Graphics2D) g, program.getWidth() / 2, program.getHeight() / 4 + program.getHeight() / 8, 28, bigText, getSecondaryColor(), 0);
         }
      }
      
      public static final class SettingsMenu extends MenuType {

         private Menu<String> settingsButtons;
         private String currentString;
         private int currentStringIndex;
                  
         public SettingsMenu() {
            settingsButtons = new Menu<>(new String[]{ "nickname", "dark_mode", "default_background_visibility", "old_look_and_feel", "parallax", "music_volume", "sfx_volume", "debug_mode", "Back" }, 128, 128, 16);
            currentString = null;
            currentStringIndex = 0;
         }
         
         @Override
         public void render(Graphics2D g) {
            int rows = settingsButtons.getRowCount();
            settingsButtons.setMenuRectangle(program.getWidth() / 2 - 56 - rows * 56, 64, program.getHeight() - 64, false);
            // fileMenu.setPosition(64, 96);
            drawFlatMenuBox(g, settingsButtons.getMenuRectangle());
            Rectangle[] buttons = settingsButtons.getItemButtons();
            for (int i = 0; i < buttons.length; i++) {
               drawMenuButton(g, buttons[i]);
               if (i > 0 && i < settingsButtons.items.length - 1) {
                  if (Settings.getBooleanSetting(settingsButtons.items[i])) g.setColor(new Color(0, 255, 0, 63));
                  else g.setColor(new Color(255, 0, 0, 63));
                  g.fillRect(buttons[i].x + 8, buttons[i].y + 8, buttons[i].width - 16, buttons[i].height - 24);
               }
               if (buttons[i].contains(MouseUtils.pointerScreenLocation())) {
                  switch (i) {
                     case 0 :
                        program.setHoverInfo("Change your author name");
                        break;
                     case 1 :
                        program.setHoverInfo("Toggle dark mode");
                        break;
                     case 2 :
                        program.setHoverInfo("Toggle editor background visibility");
                        break;
                     case 3 :
                        program.setHoverInfo("Toggle the old look and feel of the Little H's main menu");
                        break;
                     case 4 :
                        program.setHoverInfo("Toggle parallax");
                        break;
                     case 5 :
                        program.setHoverInfo("Toggle music");
                        break;
                     case 6 :
                        program.setHoverInfo("Toggle sound effects");
                        break;
                     case 7 :
                        program.setHoverInfo("Toggle developer mode");
                        break;
                     default :
                        program.setHoverInfo("Return to main menu");
                  }
                  g.setColor(new Color(255, 255, 255, 128));
                  g.fillRect(buttons[i].x, buttons[i].y, buttons[i].width, buttons[i].height);
               }
               // String text = "";
               switch (i) {
                  case 0 :
                     // text = "Change nickname";
                     Images.drawImage(g, Images.getImage("ui/name_tag.png"), buttons[i], new Rectangle(0, 0, 16, 16));
                     break;
                  case 1 :
                     // text = "Dark mode";
                     Images.drawImage(g, Images.getImage("ui/light_" + (darkMode ? "off.png" : "on.png")), buttons[i], new Rectangle(0, 0, 16, 16));
                     break;
                  case 2 :
                     // text = "Background visibility";
                     Images.drawImage(g, Images.getImage("ui/big_background_button" + (Settings.getBooleanSetting("default_background_visibility") ? ".png" : "_off.png")), buttons[i], new Rectangle(0, 0, 16, 16));
                     break;
                  case 3 :
                     // text = "Classic mode";
                     Images.drawImage(g, Images.getImage("ui/classic_mode.png"), buttons[i], new Rectangle(0, 0, 16, 16));
                     break;
                  case 4 :
                     // text = "Parallax";
                     Images.drawImage(g, Images.getImage("ui/parallax" + (Settings.getBooleanSetting("parallax") ? ".png" : "_off.png")), buttons[i], new Rectangle(0, 0, 16, 16));
                     break;
                  case 5 :
                     // text = "Music";
                     Images.drawImage(g, Images.getImage("ui/music_button" + (Settings.getBooleanSetting("music_volume") ? ".png" : "_off.png")), buttons[i], new Rectangle(0, 0, 16, 16));
                     break;
                  case 6 :
                     // text = "SFX";
                     Images.drawImage(g, Images.getImage("ui/sfx_button" + (Settings.getBooleanSetting("sfx_volume") ? ".png" : "_off.png")), buttons[i], new Rectangle(0, 0, 16, 16));
                     break;
                  case 7 :
                     // text = "Developer mode";
                     Images.drawImage(g, Images.getImage("ui/debug_wrench.png"), buttons[i], new Rectangle(0, 0, 16, 16));
                     break;
                  default :
                     // text = "Back";
                     Images.drawImage(g, Images.getImage("ui/back_arrow.png"), buttons[i], new Rectangle(0, 0, 16, 16));
               }
//                TextUtils.drawWrappingText((Graphics2D) g, (int) buttons[i].getCenterX(), (int) buttons[i].y + 32, 20, (int) buttons[i].x + buttons[i].width, text, getSecondaryColor(), 0);
//                
               if (currentString != null) { 
                  String bigText = "Author name: " + currentString;
                  
                  drawMenuBox(g, new Rectangle(program.getWidth() / 4, program.getHeight() / 2 - program.getHeight() / 8, program.getWidth() / 2, program.getHeight() / 4));
                  g.setColor(getSecondaryColor());
                  
                  TextUtils.drawWrappingText((Graphics2D) g, program.getWidth() / 2, program.getHeight() / 2 - 12, 24, program.getWidth(), bigText, getSecondaryColor(), 0);
               }
            }
         }
         
         @Override
         public void keyboardAction(int keyCode, char keyChar, boolean released) {
            if (currentString != null && !released) {
               switch (keyCode) {
                  case KeyEvent.VK_ESCAPE :
                     playBlip();
                     mainMenu = new Default();
                     break;
                  case KeyEvent.VK_BACK_SPACE :
                     if (currentString.length() > 0) currentString = currentString.substring(0, currentString.length() - 1);
                     break;
                  case KeyEvent.VK_ENTER :
                     if (currentStringIndex == 0) Settings.setSetting("nickname", currentString);
                     currentString = null;
                     break;
                  default :
                     if (keyChar != KeyEvent.CHAR_UNDEFINED && keyCode != KeyEvent.VK_ESCAPE) {
                        currentString += keyChar;
                     }
                     break;
               }
            }
         }
         
         @Override
         public void mouseAction(int button, boolean released) {
            if (currentString != null) return;
            if (button == 1 && released) {
               int buttonIndex = settingsButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
               if (buttonIndex > -1) {
                  switch (buttonIndex) {
                     case 0 :
                        playBlip();
                        currentStringIndex = 0;
                        currentString = Settings.getStringSetting("nickname");
                        break;
                     case 1 :
                        playBlip();
                        darkMode = !darkMode;
                        Settings.setSetting("dark_mode", "" + darkMode);
                        break;
                     case 2 :
                        playBlip();
                        Settings.setSetting("default_background_visibility", "" + !Settings.getBooleanSetting("default_background_visibility"));
                        break;
                     case 3 :
                        playBlip();
                        Settings.setSetting("old_look_and_feel", "" + !Settings.getBooleanSetting("old_look_and_feel"));
                        break;
                     case 4 :
                        playBlip();
                        Settings.setSetting("parallax", "" + !Settings.getBooleanSetting("parallax"));
                        break;
                     case 5 :
                        playBlip();
                        Settings.setSetting("music_volume", "" + !Settings.getBooleanSetting("music_volume"));
                        break;
                     case 6 :
                        playBlip();
                        Settings.setSetting("sfx_volume", "" + !Settings.getBooleanSetting("sfx_volume"));
                        break;
                     case 7 :
                        playBlip();
                        Settings.setSetting("debug_mode", "" + !Settings.getBooleanSetting("debug_mode"));
                        break;
                     default :
                        playBlip();
                        mainMenu = new Default();
                  }
               }
            }
         }
      }

      public static final class LevelOptions extends MenuType {
         
         private Menu<String> menuButtons;
         private Menu<String> confirmButtons;
         private boolean confirmDelete;
         private boolean wantsName;
         private boolean shiftPressed;
         private String displayName;
         private File referenceFile;
         
         public LevelOptions(File file) {
            menuButtons = new Menu<>(new String[]{ "Play", "Rename", "Edit", "Delete", "Back" }, 128, 64, 32);
            confirmButtons = new Menu<>(new String[]{ new Random(System.currentTimeMillis()).nextInt(5) == 0 ? "Absolutely" : "Yes", "No" }, 128, 64, 32);
            menuButtons.setMenuRectangle(program.getWidth() / 5 - 16, window.getHeight() / 2, 1, false);
            confirmButtons.setMenuRectangle(program.getWidth() / 3 + 64, window.getHeight() / 2, 1, false);

            confirmDelete = false;
            referenceFile = file;
         }
         
         @Override
         public void mouseAction(int button, boolean released) {
            if (button == 1 && released) {
               if (confirmDelete || wantsName) {
                  int buttonIndex = confirmButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
                  if (buttonIndex == 0) {
                     if (confirmDelete) {
                        referenceFile.delete();
                        reset();
                     } else {
                        program.setLevelName(displayName, referenceFile);
                        playBlip();
                        mainMenu = new LevelOptions(referenceFile);
                     }
                  } else if (buttonIndex == 1) {
                     wantsName = false;
                     confirmDelete = false;
                  }
               } else {
                  int buttonIndex = menuButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
                  if (buttonIndex == 0) {
                     if (!shiftPressed) game = new PlayState(referenceFile);
                     else game = new CampaignState(referenceFile);
                     lookingForMap = false;
                  } else if (buttonIndex == 1) {
                     wantsName = true;
                  } else if (buttonIndex == 2) {
                     game = new Map(referenceFile);
                     lookingForMap = false;   
                  } else if (buttonIndex == 3) {
                     confirmDelete = true;
                  } else if (buttonIndex == 4) {
                     playBlip();
                     mainMenu = new Default();
                  }
               }
            }
         } 
      
         @Override
         public void keyboardAction(int keyCode, char keyChar, boolean released) {
            if (keyCode == KeyEvent.VK_SHIFT) shiftPressed = !released;
            if (!released) {
               switch (keyCode) {
                  case KeyEvent.VK_ESCAPE:
                     playBlip();
                     mainMenu = new Default();
                     break;
                  case KeyEvent.VK_BACK_SPACE:
                     if (displayName.length() > 0) displayName = displayName.substring(0, displayName.length() - 1);
                     break;
                  default:
                     if (keyChar != KeyEvent.CHAR_UNDEFINED && keyCode != KeyEvent.VK_ESCAPE) {
                        displayName += keyChar;
                     }
                     break;
               }
            }
         }
      
         @Override
         public void render(Graphics2D g) {
            menuButtons.setPosition(program.getWidth() / 2 - (int) (2.5f * 160) - 16, program.getHeight() / 2);
            confirmButtons.setPosition(program.getWidth() / 2 - 160, program.getHeight() / 2);
            // fileMenu.setPosition(64, 96);
            if (!confirmDelete && !wantsName) {
               Rectangle[] buttons = menuButtons.getItemButtons();
               for (int i = 0; i < menuButtons.items.length; i++) {
                  drawMenuButton(g, buttons[i]);
                  if (buttons[i].contains(MouseUtils.pointerScreenLocation())) {
                     g.setColor(new Color(255, 255, 255, 127));
                     g.fillRect(buttons[i].x, buttons[i].y, buttons[i].width, buttons[i].height);
                  }
                  g.setColor(getSecondaryColor());
                  TextUtils.drawText((Graphics2D) g, (int) buttons[i].getCenterX(), (int) buttons[i].getCenterY() - 12, 20, menuButtons.items[i], getSecondaryColor(), 0);
               }
            } else {
               Rectangle[] buttons = confirmButtons.getItemButtons();
               for (int i = 0; i < confirmButtons.items.length; i++) {
                  drawMenuButton(g, buttons[i]);
                  if (buttons[i].contains(MouseUtils.pointerScreenLocation())) {
                     g.setColor(new Color(255, 255, 255, 127));
                     g.fillRect(buttons[i].x, buttons[i].y, buttons[i].width, buttons[i].height);
                  }
                  g.setColor(getSecondaryColor());
                  TextUtils.drawText((Graphics2D) g, (int) buttons[i].getCenterX(), (int) buttons[i].getCenterY() - 12, 20, confirmButtons.items[i], getSecondaryColor(), 0);
               }
            }
            
            String bigText = "";
            if (!wantsName) displayName = program.getLevelName(referenceFile);
            if (confirmDelete) {
               bigText = "Are you sure you want to delete " + displayName + " (This cannot be undone)?";
            } else if (wantsName) {
               bigText = "The level will be renamed " + displayName + ".";
            } else {
               bigText = "Selected level: " + displayName;
            }
            
            String displayAuthor = program.getLevelAuthor(referenceFile);
            
            drawMenuBox(g, new Rectangle(program.getWidth() / 4, program.getHeight() / 4, program.getWidth() / 2, program.getHeight() / 4));
            
            TextUtils.drawWrappingText((Graphics2D) g, program.getWidth() / 2, program.getHeight() / 4 + program.getHeight() / 12, 24, program.getWidth(), bigText, getSecondaryColor(), 0);
            TextUtils.drawWrappingText((Graphics2D) g, program.getWidth() / 2, program.getHeight() / 4 + program.getHeight() / 12 + 64, 20, program.getWidth(), "Author: " + displayAuthor, getSecondaryColor(), 0);
         }
      }
      
      // No levels :megamind:
      public static final class NoLevels extends MenuType {
         
         private Menu<String> menuButtons;
         
         public NoLevels() {
            menuButtons = new Menu<>(new String[]{ "Get help", "Play tutorial", "Create a level" }, 256, 96, 32);
         }
         
         @Override
         public void mouseAction(int button, boolean released) {
            if (button == 1 && released) {
               int buttonIndex = menuButtons.getOverlappedElement(MouseUtils.pointerScreenLocation());
               switch (buttonIndex) {
                  case 0 :
                     playBlip();
                     mainMenu = new Help();
                     break;
                  case 1 :
                     playLevel((inArchive ? "/" : "") + "assets/maps/tutorial.map", inArchive);
                     break;
                  case 2 :
                     playBlip();
                     mainMenu = new Creation();
                     break;
               }
            }
         } 
      
         @Override
         public void keyboardAction(int keyCode, char keyChar, boolean released) {
         }
      
         @Override
         public void render(Graphics2D g) {
            menuButtons.setMenuRectangle(program.getWidth() / 2 - 128 - 32, program.getHeight() / 3, program.getHeight(), false);
            // fileMenu.setPosition(64, 96);
            Rectangle[] buttons = menuButtons.getItemButtons();
            for (int i = 0; i < menuButtons.items.length; i++) {
               drawFlatMenuBox(g, buttons[i]);
               if (buttons[i].contains(MouseUtils.pointerScreenLocation())) {
                  g.setColor(new Color(255, 255, 255, 127));
                  g.fillRect(buttons[i].x, buttons[i].y, buttons[i].width, buttons[i].height);
               }
               g.setColor(getSecondaryColor());
               TextUtils.drawText((Graphics2D) g, (int) buttons[i].getCenterX(), (int) buttons[i].getCenterY() - 12, 20, menuButtons.items[i], getSecondaryColor(), 0);
            }
            
            String bigText = "You dont appear to have any levels. Start off with one of these";
            
            drawMenuBox(g, new Rectangle(program.getWidth() / 4, program.getHeight() / 6, program.getWidth() / 2, program.getHeight() / 6));
            
            TextUtils.drawText((Graphics2D) g, program.getWidth() / 2, program.getHeight() / 12 * 3, 24, bigText, getSecondaryColor(), 0);
         }
      }
      
      public static final class Creation extends MenuType {
         
         private String fileName;
         private boolean hasFile;
         private boolean fileDuplicate;
         private String displayName;
      
         public Creation() {
            fileName = "";
            displayName = "";
            hasFile = false;
            fileDuplicate = false;
         }
      
         @Override
         public void keyboardAction(int keyCode, char keyChar, boolean released) {
            if (released) return;
            
            if (fileDuplicate) {
               fileDuplicate = false;
               return;
            }
            switch (keyCode) {
               case KeyEvent.VK_ENTER :
                  if (!hasFile) {
                     if (new File(mapsFolderPath + "/" + fileName + ".map").exists()) {
                        fileDuplicate = true;
                        break;
                     }
                     hasFile = true;
                  } else {
                     Map map = new Map(new File(mapsFolderPath + "/" + fileName + ".map"));
                     map.name = displayName;
                     game = map;
                     lookingForMap = false;
                  }
                  break;
               case KeyEvent.VK_ESCAPE :
                  playBlip();
                  mainMenu = new Default();
                  break;
               case KeyEvent.VK_BACK_SPACE :
                  if (!hasFile) {
                     if (fileName.length() > 0) fileName = fileName.substring(0, fileName.length() - 1);
                  } else {
                     if (displayName.length() > 0) displayName = displayName.substring(0, displayName.length() - 1);
                  }
                  break;
               default :
                  if (keyChar != KeyEvent.CHAR_UNDEFINED && keyCode != KeyEvent.VK_ESCAPE) {
                     if (!hasFile) {
                        fileName += keyChar;
                     } else {
                        displayName += keyChar;
                     }
                  }
                  break;
            }
            
         }
         
         @Override
         public void render(Graphics2D g) {
            drawMenuBox(g, new Rectangle(program.getWidth() / 4, window.getHeight() / 4, program.getWidth() / 2, window.getHeight() / 2));
            if (fileDuplicate) {
               TextUtils.drawText((Graphics2D) g, program.getWidth() / 2, window.getHeight() / 2, 24, "Level with file name already exists.", getSecondaryColor(), 0);
            } else if (!hasFile) {
               TextUtils.drawText((Graphics2D) g, program.getWidth() / 2, window.getHeight() / 2, 24, "Enter a file name: " + fileName + ".map", getSecondaryColor(), 0);
            } else {
               TextUtils.drawText((Graphics2D) g, program.getWidth() / 2, window.getHeight() / 2, 24, "Enter level display name: " + displayName + ".", getSecondaryColor(), 0);
            }
         }
      }
      
      public static final class Legacy extends MenuType {
         @Override
         public void keyboardAction(int keyCode, char keyChar, boolean released) {
            if (released) {
            } else {
               if (keyCode == KeyEvent.VK_BACK_SPACE) {
                  if (game == null) {
                     if (levelName.length() > 0) levelName = levelName.substring(0, levelName.length() - 1);
                  } else {
                     if (levelDisplayName.length() > 0) levelDisplayName = levelDisplayName.substring(0, levelDisplayName.length() - 1);
                  }
               } else if (keyCode == KeyEvent.VK_SHIFT) {
               } else if (keyCode == KeyEvent.VK_ESCAPE) {
                  if (lookingForMap) {
                     playBlip();
                     mainMenu = new Default();
                  }
                  reset();
               } else if (keyCode == KeyEvent.VK_ENTER) {
                  if (game == null) {
                     Map map = new Map(mapsFolderPath + "/" + levelName + ".map");
                     map.foundName = true;
                     if (map.named) {
                        game = map;
                        lookingForMap = false;
                     }
                  } else if (game != null) {
                     Map map = new Map(mapsFolderPath + "/" + levelName + ".map");
                     if (map.named) return;
                     game = map;
                     lookingForMap = false;
                     map.name = levelDisplayName;
                     window.setTitle(TileEditor.WINDOW_TITLE + ": Editing Map \"" + levelDisplayName + "\"");
                     levelName = "";
                     levelDisplayName = "";
                  }
               } else if (keyChar != KeyEvent.CHAR_UNDEFINED && keyCode != KeyEvent.VK_ESCAPE) {
                  if (game == null) {
                     levelName += keyChar;
                  } else {
                     levelDisplayName += keyChar;
                  }
               }
            }
         }
      
         @Override
         public void render(Graphics2D g) {
            int maxHeight = 24;
            int maxWidth = 0;
            for (int i = 0; i < foundMaps.length; i++) {
               Rectangle rect = TextUtils.drawText((Graphics2D) g, program.getWidth() / 12, window.getHeight() / 12 + 30 * i + 15, 18, foundMaps[i], getSecondaryColor(), -1);
               maxHeight += i * 24;
               if (rect.width > maxWidth) maxWidth = rect.width;
            }
            g.setColor(getSecondaryColor());
            g.fillRect(program.getWidth() / 12 - 8, window.getHeight() / 12 - 8, maxWidth + 16, maxHeight + 16);
            g.setColor(getMainColor());
            g.fillRect(program.getWidth() / 12 - 4, window.getHeight() / 12 - 4, maxWidth + 8, maxHeight + 8);
            for (int i = 0; i < foundMaps.length; i++) {
               TextUtils.drawText((Graphics2D) g, program.getWidth() / 12, window.getHeight() / 12 + 30 * i + 15, 18, foundMaps[i], getSecondaryColor(), -1);
            }
            g.setColor(getSecondaryColor());
            g.fillRect(program.getWidth() / 4, window.getHeight() / 4, program.getWidth() / 2, window.getHeight() / 2);
            g.setColor(getMainColor());
            g.fillRect(program.getWidth() / 4 + 4, window.getHeight() / 4 + 4, program.getWidth() / 2 - 8, window.getHeight() / 2 - 8);
            TextUtils.drawText((Graphics2D) g, program.getWidth() / 2, window.getHeight() / 2, 24, "Enter file name: " + levelName + ".map", getSecondaryColor(), 0);
         }
      }
      
      public static final class Help extends MenuType {
         
         private String[] subtitles;
         private String[] text;
         private Menu<String> menu;
         private int index;
         
         public Help() {
            subtitles = new String[]{
               "The Little H",
               "Editing",
               "Level Options",
               "Playtesting",
               "Tiles",
               "Enemies",
               "Power Fruits",
               "Death",
               "Checkpoints and Goal Crystals",
               "Timer",
               "Decoration",
               "Tutorial",
               "Back"
            };
            
            text = new String[]{
               "   You are the little H, a special operative sent on missions to retrieve the mysterious and powerful \"Goal Crystals\" after your larger predecessor failed to return in one piece. You task, then, is simple: get there by any means necessary.\n\n   Your abilities include the power to double jump, slide, and wall jump. Use these powers to maneuver your way past obstacles and reach your destination.",               
               "   Sometimes you aren't the Little H, and instead are an omnipotent creator. In the level editor you will have access to many tiles and tools used to place them. You can cycle through tiles with the Q and E keys, but much faster is to use your mouse and click on the tile in the top left. From this menu you can select which block you wish to place. Some tiles will also have a little gear on the lower right side of their icon, this means that the tile has multiple properties you can choose from, be it color, size, or rotation.\n   On the top right of the screen you will see an assortment of tools that can be selected, these are:\n   -Pencil: Attempts to place a tile at your cursor every frame\n   -Eraser: Attempts to erase the tile at your cursor every frame\n   -Pen: Like the pencil but fills in the line between the placed tiles (this allows you to draw smoother)\n   -Paint Bucket: Fills in all adjacent tiles (right click removes all adjacent tiles)\n   -Eye Dropper: Copies the hovered-over tile and its properties to the current selected tile",
               "   In the level editor, you may notice a gear at the bottom right corner. This gear opens the level options. From left to right, the buttons allow you to:\n -Change the level's background,\n -Toggle background visibility in the editor,\n -Change the level's time limit,\n -Toggle the player's ability to double jump,\n -Toggle the player's ability to wall slide/jump,\n -Toggle the player's ability to crouch and slide,\n -Close the menu.",
               "   While editing you can press the Enter key to playtest the level, starting you at a start position and allowing you to move and jump as though you were playing the level.\n   Pressing the Escape key will place you back in the editor as will finishing the level. Pressing the escape key while in the editor will return you to the main menu.",
               "   There are many tiles to choose from when constructing a level, from slippery ice, to bouncy platforms, and malevolent keys. There are three main categories in this selection:\n   -Solid: tiles like grass and ice that the Little H can't pass through\n   -Danger: tiles like malice and spikes that kill the Little H\n   -Special: tiles like bounce platforms and keys that have a unique interaction with the Little H\n\n   Some tiles also have additional selectable properties that may do nothing more than change the appearance or hitbox of the tile, to completely changing the function of the tile.",
               "   Some tiles look like other letters. These poor members of the alphabet have gone feral and want nothing more than to destroy you! Usually, a single touch from another letter will kill the little H, so be smart about avoiding them. \n   The adversaries you can encounter are:\n -e: Walks back and forth, and not much else.\n -a: Chases the little H down. It's not very smart, though.\n -f: Acts much like the e but airborne.",
               "   There are some tiles that appear to be fruits. These are the enigmatic \"Power Fruits\" found throughout the world. Upon eating a power fruit, the little H will take on a new form that functions differently.\n   These plants include:\n -H-Plant: This \"h\" shaped plant turns you back to your normal form if you’ve eaten something else.\n -Big Orange: This big, round orange activates BALL MODE. Ball mode makes the little H extra bouncy and crouching is replaced with the SUPER SLAM in mid-air!\n -Cloudpepper: This light-as-a-feather fruit makes you grow wings! Your jump is improved and (as long as they are enabled) you can double jump as much as you want. Even if air jumps aren't enabled, holding down the jump button lets you glide through the air.\n -Axeberry: The weapon-shaped axeberry remove's the h's legs and arms. In this form, the h cannot change direction in mid-air. On the bright side, though, you become half usual size and can jump on top of enemies to kill them.",
               "   Dying is not permanent, or at least it shouldn't be. We still don't know how the Big H got completely destroyed... but surely the same thing won't happen to you! Dangerous tiles like malice, spikes, or invisible skulls can end your run, as will falling too far into the void.\n   Additionally, pressing the K key will immediately trigger your kill switch, allowing you to easily reset to your last checkpoint, or letting you free yourself from being trapped in the level.",
               "   Believe it or not, mission control wants you to succeed. To aid you they have dropped H reconstruction stations across levels. Unfortunately, the preboarded got samples get destroyed while being deployed. In order to reactivate one, simply walk past it and you will now respawn there. Some checkpoints have a different appearance and red color. These checkpoints can reprocess genetic material, allowing you to trigger them multiple times.\n   These checkpoints will hopefully allow you to reach the Goal Crystal faster. Once you find one, pick it up and we'll warp you out. Keep in mind that there is no guarantee that only one Goal Crystal exists in each level; if there is a fork in the road, both paths may be equally correct.",
               "   Some levels can only sustain you for a limited amount of time as indicated by the countdown in the top right corner. Remember, respawning at a checkpoint doesn't reset the timer and running out of time resets you to the beginning of the level; if you see the timer, you should probably act fast.\n\n   Some levels may have magical stopwatches and clocks that somehow extend the amount of time you can spend in the level.",
               "   Some tiles, namely the so-called \"color cubes\" (despite clearly being squares), exist solely for the purpose of decoration. Use them to give your levels more flair and charm. Or, you can be like some and just put a bunch of amogi everywhere...\n   Decoration can also be used to communicate something to the player without using text. Use decor to make puzzles, markers, or even \"lore.\"",
               "   Seeing this is unintended...",
               "   Seeing this is unintended..."
            };
            
            menu = new Menu<>(subtitles, 128, 128, 4);
            
            index = 0;
         }
         
         @Override
         public void mouseAction(int button, boolean released) {
            if (button == 1 && released) {
               int buttonIndex = menu.getOverlappedElement(MouseUtils.pointerScreenLocation());
               if (buttonIndex != -1) index = buttonIndex;
               if (index == subtitles.length - 1) {
                  playBlip();
                  mainMenu = new Default();
               }
               else if (index == subtitles.length - 2) playLevel((inArchive ? "/" : "") + "assets/maps/tutorial.map", inArchive);
            }
         }
         
         @Override
         public void keyboardAction(int keyCode, char keyChar, boolean released) {
            switch (keyCode) {
               case KeyEvent.VK_ESCAPE :
                  playBlip();
                  mainMenu = new Default();
                  break;
            }
         }
         
         @Override
         public void render(Graphics2D g) {
            drawMenuBox(g, new Rectangle(0, 0, TileEditor.program.getWidth(), TileEditor.program.getHeight()));
            menu.setElementDimensions((int) ((program.getWidth() - 64) / (float) menu.items.length), 128, 4);
            menu.setMenuRectangle(4, program.getHeight() - 128, 1, false);
            
            switch (index) {
               case 0 :
                  g.drawImage(Images.getImage("ui/help/little_h_slide.png"), program.getWidth() / 2 - 256, program.getHeight() / 5 * 3, program);
                  break;
               case 1 :
                  g.drawImage(Images.getImage("ui/help/tools.png"), program.getWidth() - 96, program.getHeight() / 2 - 352 / 2, program);
                  break;
               case 2 :
                  g.drawImage(Images.getImage("ui/help/settings.png"), program.getWidth() / 2 - 240, program.getHeight() / 2 + 64, program);
                  break;
               case 3 :
                  g.drawImage(Images.getImage("ui/help/playtest.png"), program.getWidth() / 2 - 256, program.getHeight() / 5 * 3, program);
                  break;
               case 4 :
                  g.drawImage(Images.getImage("ui/help/tiles.png"), program.getWidth() - 96, program.getHeight() / 2 - 352 / 2 + 96, program);
                  break;
               case 5 :
                  break;
               case 6 :
                  break;
               case 7 :
                  g.drawImage(Images.getImage("ui/help/death.png"), program.getWidth() / 2 - 128, program.getHeight() / 2, program);
                  break;
               case 8 :
                  g.drawImage(Images.getImage("ui/help/checkpoints.png"), program.getWidth() / 2 - 512, program.getHeight() / 2, program);
                  break;
               case 9 :
                  g.drawImage(Images.getImage("ui/help/timer.png"), program.getWidth() / 2 - 240, program.getHeight() / 2 + 256, program);
                  break;
               case 10 :
                  g.drawImage(Images.getImage("ui/help/decoration.png"), program.getWidth() / 2 - 256, program.getHeight() / 4 * 1, program);
                  break;
            }
            
            TextUtils.drawText(g, program.getWidth() / 2, program.getHeight() / 14, program.getWidth() / 32, subtitles[index], getSecondaryColor(), 0);

            TextUtils.drawWrappingText(g, 64, program.getHeight() / 7, program.getWidth() / 50, text[index], getSecondaryColor());
            
            Rectangle[] buttons = menu.getItemButtons();
            
            for (int i = 0; i < buttons.length; i++) {
               Rectangle button = buttons[i];
               boolean hoveredButton = button.contains(MouseUtils.pointerScreenLocation());
               boolean selectedButton = i == index;
               int yOffset = 16;
               
               Rectangle rect = new Rectangle(button.x - 6, -6 + yOffset + button.y + (selectedButton ? -48 : 0) + (hoveredButton ? 32 : 48), button.width + 12, button.height + 96 + 12);
               drawMenuBox(g, rect);

               String buttonName = menu.items[i];
               int limitLength = program.getWidth() / 130;
               if (buttonName.length() > limitLength) buttonName = buttonName.substring(0, limitLength) + "...";

               TextUtils.drawText(g, (int) button.getCenterX(), yOffset - 36 + (int) button.getCenterY() + (selectedButton ? -48 : 0) + (hoveredButton ? 16 : 32), 20, buttonName, getSecondaryColor(), 0);
            }
         }
      }
   }
}