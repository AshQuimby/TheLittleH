package src.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

import src.TileEditor;
import src.Particle;
import src.Images;
import src.map.menu.Menu;
import src.map.enemy.Enemy;
import src.util.MouseUtils;
import src.util.SoundEngine;
import src.util.TextUtils;
import src.util.Settings;

public class Map extends GameState {
   public MapEditor editor;
   public boolean foundName;
   private int selectionIndex;
   private int menuSelectionIndex;
   public boolean named;
   private Point prevMousePos;
   public static Rectangle selectorButton = new Rectangle(0, 0, 68, 68);
   public static Rectangle settingsButton = new Rectangle(TileEditor.program.getWidth() - 68, TileEditor.program.getHeight() - 68, 68, 68);
   public Tile modifyExtra;
   private int typeInsertPos;
   private String timerSet;
   
   public Point lastResizeOffset;
   
   public Menu<String> currentMenu;
   public static Menu<String> toolMenu = new Menu<String>(new String[]{ "ui/pencil.png", "ui/eraser.png", "ui/pen.png", "ui/paint_can.png", "ui/color_picker.png", "ui/h.png" }, 64, 64, true);
   private int toolIndex;
   public static Menu<String> baseTileMenu = new Menu<String>(new String[]{ "settings_dots", "tiles/sets/grass", "tiles/sets/stone", "tiles/sets/snowy_turf", "tiles/sets/rock", "tiles/sets/location_bricks", "tiles/sets/ice", "tiles/sets/slick_block", "tiles/invisiblock", "tiles/sets/malice", "tiles/half_spike", "tiles/invisible_death", "tiles/coin", "tiles/coin_box", "tiles/power_fruit", "tiles/enemy", "tiles/timer", "tiles/key", "tiles/key_box", "tiles/evil_key", "tiles/evil_key_box", "tiles/sets/bounce", "tiles/one_way", "tiles/checkpoint", "tiles/strong_checkpoint", "tiles/spawn", "tiles/end", "tiles/color_cube", "tiles/text", "forward_arrow" }, 64, 64, true);
   public static Menu<String> debugTileMenu = new Menu<String>(new String[]{ "settings_dots", "tiles/sets/grass", "tiles/sets/stone", "tiles/sets/snowy_turf", "tiles/sets/rock", "tiles/sets/location_bricks", "tiles/sets/ice", "tiles/sets/slick_block", "tiles/invisiblock", "tiles/sets/malice", "tiles/half_spike", "tiles/invisible_death", "tiles/coin", "tiles/coin_box", "tiles/power_fruit", "tiles/enemy", "tiles/timer", "tiles/key", "tiles/key_box", "tiles/evil_key", "tiles/evil_key_box", "tiles/sets/bounce", "tiles/one_way", "tiles/checkpoint", "tiles/strong_checkpoint", "tiles/spawn", "tiles/end", "tiles/h_fragment", "tiles/color_cube", "tiles/text", "tiles/dialogue_trigger", "forward_arrow" }, 64, 64, true);
   public static List<Menu<String>> customTileMenus = new ArrayList<>();
   
   public static Menu<String> tileMenu = Settings.getBooleanSetting("debug_mode") ? debugTileMenu : baseTileMenu;
   private Tile[] tileSelection;
   public static Menu<String> settingsMenu = new Menu<String>(new String[]{ "ui/background.png", "ui/background_visible.png", "ui/clock.png", "ui/double_jump.png", "ui/wall_jump.png", "ui/slide.png", "ui/gear.png" }, 64, 64, 4);
   private static Menu<String> backgroundMenu = new Menu<String>(new String[]{ "mountains", "cold_mountains", "cave", "hyperspace", "tundra" }, 256, 144, 8);
   
   public Map(String name) {
      this(new File(name));
   }
   
   public Map(File file, String displayName) {
      this(file);
      name = displayName;
   }
   
   public Map(File file) {
      super();
      lastResizeOffset = new Point(0, 0);
      tileMenu.subMenu = null;
      currentMenu = null;
      modifyExtra = null;
      typeInsertPos = 0;
      named = false;
      toolIndex = 0;
      setUnsaved();
      tileSelection = new Tile[tileMenu.items.length - 2];
      timeLimit = -1;
      background = "mountains";
      resetTileMenu();
      prevMousePos = new Point(0, 0);
      shiftPressed = false;
   
      TileEditor.window.getContentPane().setCursor(Toolkit.getDefaultToolkit().createCustomCursor(Images.getImage(getToolCursor(toolMenu.items[0])), new Point(0, 0), "H!"));
      
      editor = new MapEditor(this);
   
      referenceFile = file;
   
      try {
         if (!referenceFile.exists()) {
            File mapsFolder = new File(TileEditor.mapsFolderPath);
            if (!mapsFolder.exists()) {
               Files.createDirectory(Paths.get(mapsFolder.getPath()));
            }
            Files.createFile(Paths.get(referenceFile.getPath()));
         }
         loadMap();
      } catch (Exception e) {
         System.out.println("Error reading map file");
         throw new RuntimeException(e);
      } 
   }
   
   public void resetTileMenu() {
      int i = 1;
      java.util.Map<String, List<String>> tileMenus = new HashMap<>();
      for (Tile tile : TileEditor.customTiles.values()) {
         String key = tile.image.replace("./tile_packs/", "").split("/")[0];
         if (!tileMenus.containsKey(key)) {
            tileMenus.put(key, new ArrayList<>());
            tileMenus.get(key).add("settings_dots");
         }
         tileMenus.get(key).add(tile.image);
         i++;
      }
      customTileMenus.clear();
      customTileMenus.add(Settings.getBooleanSetting("debug_mode") ? debugTileMenu : baseTileMenu);
      for (List list : tileMenus.values()) {
         list.add("forward_arrow");
         String[] array = new String[list.size()];
         list.toArray(array);
         customTileMenus.add(new Menu(array, 64, 64, true));
      }
      for (i = 0; i < tileMenu.items.length - 2; i++) {
         // System.out.println(TileEditor.customTiles.get(tileMenu.items[i + 1]).tags);
         tileSelection[i] = new Tile(tileMenu.items[i + 1], getTags(tileMenu.items[i + 1]));
      }
   }
   
   public String getToolCursor(String original) {
      String[] splitString = original.split("/");
      splitString[1] = splitString[1].replace(".png", "");
      String string = splitString[0] + "/cursor/" + splitString[1] + (TileEditor.darkMode ? "_dark" : "_light") + ".png";
      return string;
   }
   
   @Override
   public void readFile() {
      int width = 16;
      int height = 16;
      Scanner scanner = null;
      try { scanner = new Scanner(referenceFile); } 
      catch (Exception e) { }
      while (scanner.hasNext()) {
         totalLines++;
         scanner.nextLine();
      }
      scanner.close();
      try { scanner = new Scanner(referenceFile); } 
      catch (Exception e) { }
      if (!scanner.hasNext()) {
         saveToFile();
      }
      scanner.close();
      try { scanner = new Scanner(referenceFile); } 
      catch (Exception e) { }
      
      String line = scanner.nextLine();
      while (line.startsWith("@") && scanner.hasNext()) {
         line = scanner.nextLine();
      }
      
      TileEditor.window.setTitle(TileEditor.WINDOW_TITLE + ": Editing Map \"" + name + "\"");
      while (line != null) {
         if (line.startsWith("@")) {
            if (!scanner.hasNext()) break;
            else line = scanner.nextLine();
         }
         String[] data = line.split(" ");
         String extra = "";
         for (int i = 4; i < data.length; i++) {
            extra += data[i] + " ";
         }
         int type = Integer.parseInt(data[3]);
         if (updateReadTiles) {
            data[2] = getCurrentVersionEquivalent(data[2]);
            if (data[2].contains("half_spike")) type = 2;
         }
         
         if (data.length < 4) allTiles.add(new Tile(Integer.parseInt(data[0]), Integer.parseInt(data[1]), data[2], type));
         else allTiles.add(new Tile(Integer.parseInt(data[0]), Integer.parseInt(data[1]), data[2], type, getTags(data[2]), extra));

         if (width < Math.max(16, Integer.parseInt(data[0])) + 1) {
            width = Math.max(16, Integer.parseInt(data[0]) + 1);
         }
         if (height < Math.max(16, Integer.parseInt(data[1])) + 1) {
            height = Math.max(16, Integer.parseInt(data[1])) + 1;
         }
                  
         if (!scanner.hasNext()) line = null;
         else line = scanner.nextLine();
         tilesLoaded++;
      }
      scanner.close();
      finalizing = true;
      map = new Tile[width][height];
      ArrayList<Tile> duplicates = new ArrayList<>();
      for (Tile tile : allTiles) {
         if (map[tile.x][tile.y] != null) {
            System.out.println("pooplicate");
            duplicates.add(tile);
         } else {
            if (tile.hasTag("spawn")) {
               camera.setCenter(tile.x * 64, tile.y * 64);
            }
            // tile.setTags(getTags(tile.image));
            map[tile.x][tile.y] = tile;
         }
      }
      allTiles.removeAll(duplicates);
      for (Tile tile : allTiles) {
         editor.checkTiling(tile, tile.x, tile.y);
      }
      saveMap = true;
      startEditing();
      finishedReading();
   }
   
   @Override
   public void finishedReading() {
      SoundEngine.playMusic("music/building_song.wav");
      saveToFile();
      finishedLoading = true;
   }
   
   public void startEditing() {
      if (player != null) playerTrace = player.previousPositions;
      enemies.clear();
      syncTilemap();
      SoundEngine.playMusic("music/building_song.wav");
      timeUp = 0;
      player = null;
   }
   
   public void startTesting() {
      desyncTilemap();
      SoundEngine.playMusic("music/" + background + "_song.wav");
      speedrunTimer = 0;
      checkedTime = 0;
      timeLeft = timeLimit;
   }
   
   public boolean getMousePlaceable() {
      Point mousePoint = MouseUtils.pointerScreenLocation();
      if (!new Rectangle(0, 0, TileEditor.program.getWidth(), TileEditor.program.getHeight()).contains(mousePoint)) 
         return false;
      return !(mapSettings[SELECT_BACKGROUND] && backgroundMenu.getMenuRectangle().contains(mousePoint)) && !selectorButton.contains(mousePoint) && !settingsButton.contains(mousePoint) && !menuRectangle().contains(mousePoint) && !subMenuRectangle().contains(mousePoint) && !toolMenuRectangle().contains(mousePoint);
   }
   
   @Override
   public void endGame() {
      startEditing();
   }
   
   @Override
   public void mousePressed(int button) {
      if (button == 1) {
         mouseClicked = true;
         leftMouse = true;
      } else if (button == 3) {
         rightMouse = true;
      }
   }
   
   @Override
   public void mouseReleased(int button) {
      if (button == 1) {
         leftMouse = false;
      } else if (button == 3) {
         rightMouse = false;
      }
   }
   
   @Override
   public void keyPressed(int keyCode, char character, boolean typed) {
      if (typed) 
         return;
      if (mapSettings[CHANGE_TIME_LIMIT]) {
         switch (keyCode) {
            case KeyEvent.VK_BACK_SPACE :
               if (timerSet.length() > 0) timerSet = timerSet.substring(0, timerSet.length() - 1);
               break;
            case KeyEvent.VK_ENTER :
               if (timerSet.equals("")) {
                  timeLimit = -1;
               } else {
                  timeLimit = Integer.parseInt(timerSet);
               }
               mapSettings[CHANGE_TIME_LIMIT] = false;
               timerSet = null;
               break;
            case KeyEvent.VK_ESCAPE :
               mapSettings[CHANGE_TIME_LIMIT] = false;
               timerSet = null;
               break;
            default :
               if (keyCode < 58 && keyCode > 47) {
                  timerSet += keyCode - 48;
                  if (timerSet != null) 
                     if (Integer.parseInt(timerSet) > 3600) timerSet = "3600";
               }
         }
         return;
         
      } else if (modifyExtra != null) {
         String text;
         switch (keyCode) {
            case KeyEvent.VK_BACK_SPACE :
               if (typeInsertPos == 0 || modifyExtra.extra.substring(typeInsertPos - 1).isEmpty()) 
                  break;
               text = modifyExtra.extra;
               modifyExtra.extra = new StringBuilder(text).deleteCharAt(typeInsertPos - 1).toString();
               typeInsertPos--;
               break;
            case KeyEvent.VK_ENTER :
               modifyExtra = null;
               break;
            case KeyEvent.VK_ESCAPE :
               modifyExtra = null;
               break;
            case KeyEvent.VK_LEFT :
               typeInsertPos = Math.max(0, typeInsertPos - 1);
               break;
            case KeyEvent.VK_RIGHT :
               typeInsertPos = Math.min(modifyExtra.extra.length(), typeInsertPos + 1);
               break;
            default :
               if (character == KeyEvent.CHAR_UNDEFINED || keyCode == KeyEvent.VK_ESCAPE) 
                  return;
               text = modifyExtra.extra;
               modifyExtra.extra = new StringBuilder(text).insert(typeInsertPos, character).toString();
               typeInsertPos++;
               return;
         }
         return;
      }
     
      super.keyPressed(keyCode, character, typed);
     
      if (keyCode == KeyEvent.VK_S) {
         if (controlPressed && shiftPressed) {
            saveTBHL = true;
            return;
         }
         if (controlPressed) {
            saveMap = true;
            return;
         }
      }
      if (player == null) {
         switch (keyCode) {
            case KeyEvent.VK_W :
               camera.translate(0, (int) (-64 / camera.getZoom()));
               break;
            case KeyEvent.VK_A :
               camera.translate((int) (-64 / camera.getZoom()), 0);
               break;
            case KeyEvent.VK_S :
               if (!controlPressed) camera.translate(0, (int) (64 / camera.getZoom()));
               break;
            case KeyEvent.VK_D :
               camera.translate((int) (64 / camera.getZoom()), 0);
               break;
            case KeyEvent.VK_MINUS :
               if (shiftPressed) camera.addZoom(-0.03125f);
               else camera.addZoom(-0.125f);
               break;
            case KeyEvent.VK_EQUALS :
               if (shiftPressed) camera.addZoom(0.03125f);
               else camera.addZoom(0.125f);
               break;
            case KeyEvent.VK_Q :
               cycleSelector(false);
               break;
            case KeyEvent.VK_E :
               cycleSelector(true);
               break;
            case KeyEvent.VK_C :
               cycleTileProperties(true);
               break;
            case KeyEvent.VK_Z :
               if (controlPressed) {
                  editor.undo();
               } else {
                  cycleTileProperties(false);
               }
               break;
            case KeyEvent.VK_Y :
               if (controlPressed) {
                  editor.redo();
               }
               break;
            case KeyEvent.VK_ESCAPE :
               TileEditor.game = null;
               TileEditor.reset();
               if (!saved) TileEditor.program.returnFromEditor(this);
               break;
            case KeyEvent.VK_TAB :
               if (currentMenu == tileMenu) {
                  currentMenu = null;
               } else { 
                  currentMenu = tileMenu;
               }
               break;
            case KeyEvent.VK_ENTER :   
               boolean foundStartPos = false;
               for (Tile tile : allTiles) {
                  if (tile.hasTag("spawn")) {
                     startPosition = new Point(tile.x * 64, tile.y * 64);
                     foundStartPos = true;
                  }
               }
               if (!foundStartPos) {
                  currentPopup = new Popup("Please add a start position (the red triangle)");
               } else {
                  startTesting();
                  player = new Player(startPosition, this);
                  camera.setCenter(player.x + player.width / 2, player.y + player.height / 2);
               }
               break;
            default :
               break;
         }
      }
   }
   
   public void cycleTileProperties(boolean forward) {
      if (tileSelection[selectionIndex].hasTag("has_properties")) tileSelection[selectionIndex].cycleProperties(forward);
   }
      
   @Override
   public void keyReleased(int keyCode) {
      if (keyCode == KeyEvent.VK_SHIFT) {
         shiftPressed = false;
      } else if (keyCode == KeyEvent.VK_CONTROL) {
         controlPressed = false;
      }
      if (player == null) {
         
      } else {
         playerKeyReleased(keyCode);
      }
   }
   
   public void resize(int widthToAdd, int heightToAdd, boolean negative) {
      Tile[][] newMap = new Tile[map.length + widthToAdd][map[0].length + heightToAdd];
      for (Tile tile : allTiles) {
         if (negative) {
            tile.x += widthToAdd;
            tile.y += heightToAdd;
            newMap[tile.x][tile.y] = tile;
         } else {
            newMap[tile.x][tile.y] = tile;
         }
      }
      // for (int i = 0; i < map.length; i++) {
   //          for (int j = 0; j < map[i].length; j++) {
   //             if (map[i][j] != null) {
   //                map[i][j].x = i + (negative ? widthToAdd : 0);
   //                map[i][j].y = j + (negative ? heightToAdd : 0);
   //             }
   //             newMap[i + (negative ? widthToAdd : 0)][j + (negative ? heightToAdd : 0)] = map[i][j];
   //          }
   //       }
      if (negative) {
         camera.translate(camera.applyZoom((int) (widthToAdd * (64 / camera.getZoom()))), camera.applyZoom((int) (heightToAdd * (64 / camera.getZoom()))));
         editor.updateUndoPositions(widthToAdd, heightToAdd);
         for (Point point : playerTrace) {
            point.x += widthToAdd * 64;
            point.y += heightToAdd * 64;
         }
         lastResizeOffset.x = widthToAdd;
         lastResizeOffset.y = heightToAdd;
      } else {
         lastResizeOffset.x = 0;
         lastResizeOffset.y = 0;
      }
      map = newMap;
   }
   
   public void cycleSelector(boolean positive) {
      selectionIndex += positive ? 1 : -1;
      if (selectionIndex >= tileSelection.length) {
         selectionIndex = 0;
         menuSelectionIndex++;
         if (menuSelectionIndex >= customTileMenus.size()) {
            menuSelectionIndex = 0;
         }
      } else if (selectionIndex < 0) {
         selectionIndex = tileSelection.length - 1;
         menuSelectionIndex--;
         if (menuSelectionIndex < 0) {
            menuSelectionIndex = customTileMenus.size() - 1;
            selectionIndex = customTileMenus.get(menuSelectionIndex).items.length - 3;
         }
      }
      if (currentMenu == tileMenu) currentMenu = customTileMenus.get(menuSelectionIndex);
      tileMenu = customTileMenus.get(menuSelectionIndex);
      tileSelection = new Tile[tileMenu.items.length - 2];
      for (int i = 0; i < tileMenu.items.length - 2; i++) {
         tileSelection[i] = new Tile(tileMenu.items[i + 1], getTags(tileMenu.items[i + 1]));
      }
      if (currentMenu.subMenu != null && !tileSelection[selectionIndex].hasTag("properties")) currentMenu.subMenu = null;
   }
   
   @Override
   public void update() {
      settingsButton = new Rectangle(TileEditor.program.getWidth() - 68, TileEditor.program.getHeight() - 68, 68, 68);
      
      if (mapSettings[CHANGE_TIME_LIMIT]) {
         mapSettings[SELECT_BACKGROUND] = false;
         if (timerSet == null) {
            if (timeLimit < 0) timerSet = "";
            else timerSet = "" + timeLimit;
         }
         return;
      }
      if (currentMenu != settingsMenu) {
         mapSettings[SELECT_BACKGROUND] = false;
      }
      if (modifyExtra != null) 
         return;
      if (saveMap) {
         saveToFile();   
      }
   //       if (saveTBHL) {
   //          // saveToTBHL();   
   //       }
      
      for (Particle particle : newParticles) {
         particles.add(particle);
      }
      newParticles.clear();
      List<Particle> deadParticles = new ArrayList<>();
      for (Particle particle : particles) {
         particle.update();
         if (!particle.alive) deadParticles.add(particle);
      }
      particles.removeAll(deadParticles);
      
      if (currentMenu == settingsMenu) {
         int buttonIndex = settingsMenu.getOverlappedElement(MouseUtils.pointerScreenLocation());
         switch (buttonIndex) {
            case 0 :
               TileEditor.program.setHoverInfo("Select a background");
               break;
            case 1 :
               TileEditor.program.setHoverInfo("Toggle background visibility");
               break;
            case 2 :
               TileEditor.program.setHoverInfo("Set level time limit");
               break;
            case 3 :
               TileEditor.program.setHoverInfo("Toggle air jumping");
               break;
            case 4 :
               TileEditor.program.setHoverInfo("Toggle wall sliding");
               break;
            case 5 :
               TileEditor.program.setHoverInfo("Toggle crouching");
               break;
         }
      }
   
      if (player == null) {
         if (leftMouse) {
            if (shiftPressed) {
               Point difference = new Point();
               difference.x = (int) (prevMousePos.x - MouseUtils.pointerCameraLocation().x);
               difference.y = (int) (prevMousePos.y - MouseUtils.pointerCameraLocation().y);
               camera.translate(difference.x, difference.y);
            } else if (getMousePlaceable()) {
               int mX = getMouseTile().x;
               int mY = getMouseTile().y;
               if (toolIndex == 0) {
                  editor.addTile(new Tile(mX, mY, tileSelection[selectionIndex].image, tileSelection[selectionIndex].tileType, tileSelection[selectionIndex].getTags()), mX, mY);
               } else if (toolIndex == 1) {
                  editor.addTile(null, mX, mY);
               } else if (toolIndex == 2) {
                  int pMX = (int) Math.floor(prevMousePos.x / 64f) * 64 / 64;
                  int pMY = (int) Math.floor(prevMousePos.y / 64f) * 64 / 64;
                  editor.drawLine(new Tile(mX, mY, tileSelection[selectionIndex].image, tileSelection[selectionIndex].tileType, tileSelection[selectionIndex].getTags()), new Point(pMX, pMY), new Point(mX, mY));
               } else if (toolIndex == 3 && mouseClicked) {
                  editor.fill(new Tile(mX, mY, tileSelection[selectionIndex].image, tileSelection[selectionIndex].tileType, tileSelection[selectionIndex].getTags()), mX, mY, false);
               } else if (toolIndex == 4 && mouseClicked) {
                  for (int i = 0; i < tileSelection.length; i++) {
                     if (!(mX > 0 && mY > 0 && mX < map.length && mY < map[0].length) || map[mX][mY] == null) 
                        break;
                     if (tileSelection[i].image.equals(map[mX][mY].image)) {
                        selectionIndex = i;
                        if (tileSelection[i].hasTag("has_properties")) {
                           tileSelection[i].setPropertyValue(map[mX][mY].getPropertyIndex());
                           if (currentMenu != null) currentMenu.setSubMenu(new Menu<>(new Object[tileSelection[selectionIndex].getPropertyValues().length + 1], 64, 64, false));
                        } else {
                           tileMenu.subMenu = null;
                        }
                        break;
                     }
                  }
               } else if (toolIndex == 5 && mouseClicked) {
                  toolIndex = 0;
                  TileEditor.window.getContentPane().setCursor(Toolkit.getDefaultToolkit().createCustomCursor(Images.getImage(getToolCursor(toolMenu.items[toolIndex])), new Point(0, 0), "H!"));
                  startTesting();
                  player = new Player(new Point(mX * 64, mY * 64), this);
                  return;
               }
            }
         }
         if (rightMouse && !shiftPressed) {
            if (toolIndex == 3) {
               editor.fill(new Tile("delete"), getMouseTile().x, getMouseTile().y, false);
            } else {
               editor.addTile(null, getMouseTile().x, getMouseTile().y);
            }
         }
      } else if (map != null) {
         for (Tile tile : updatableTiles) {
            float playerDist = (float) Math.sqrt((player.x - tile.x * 64) * (player.x - tile.x * 64) + (player.y - tile.y * 64) * (player.y - tile.y * 64));
            if (playerDist < 1380) tile.notify(this, "notify_update", new int[] {(int) playerDist});
         }
         List<Enemy> removeEnemies = new ArrayList<>();
         for (Enemy enemy : enemies.values()) {
            enemy.update(this);
            if (enemy.remove) {
               removeEnemies.add(enemy);
               inGameRemoveTile(enemy.getParent());
            } else if (enemy.despawn) {
               removeEnemies.add(enemy);
            }
         }
         for (Enemy enemy : removeEnemies) {
            enemies.remove(enemy.getParent());
         }
         player.update(this); 
         gameTick++;
         if (timeUp > 0) {
            currentPopup = new Popup("OUT OF TIME!", 60);
         } else {
            checkTimer();
         }
         if (player != null) {
            if (!player.win) {
               camera.setTargetCenter(player.getCenterX(), player.getCenterY());
               if (shiftPressed) {
                  camera.setCenter(player.getCenterX(), player.getCenterY());
                  camera.setCenter(MouseUtils.pointerCameraLocation().x, MouseUtils.pointerCameraLocation().y);
               }
            }
         } else {
         }
      }
      if (toolMenu != null && mouseClicked) {
         int maxY = (TileEditor.window.getHeight() - 72 * 2) / 72 + 1;
         int offsetX = TileEditor.window.getWidth() - 80;
         for (int i = 0; i < toolMenu.items.length; i++) {
            // int x = i / maxY;
            int y = i % maxY;
            Rectangle button = new Rectangle(offsetX, y * 72, 64, 64);
            if (button.contains(MouseUtils.pointerScreenLocation())) {
               toolIndex = i;
               TileEditor.window.getContentPane().setCursor(Toolkit.getDefaultToolkit().createCustomCursor(Images.getImage(getToolCursor(toolMenu.items[i])), new Point(0, 0), "H!"));
            }
         }
      }
      if (mouseClicked) {
         if (currentMenu != tileMenu && selectorButton.contains(MouseUtils.pointerScreenLocation()) && !mapSettings[SELECT_BACKGROUND]) {
            currentMenu = tileMenu;
            mouseClicked = false;
         } else if (currentMenu != settingsMenu && settingsButton.contains(MouseUtils.pointerScreenLocation())) {
            currentMenu = settingsMenu;
            mouseClicked = false;
         }
      }
      if (currentMenu == tileMenu) {
         if (leftMouse) {
            int subMenuX = 0;
            int maxY = (TileEditor.window.getHeight() - 72 * 2) / 72 + 1;
            for (int i = 0; i < currentMenu.items.length; i++) {
               int x = i / maxY;
               int y = i % maxY;
               Rectangle button = new Rectangle(x * 72, y * 72, 64, 64);
               if (button.contains(MouseUtils.pointerScreenLocation())) {
                  if (i == 0) { 
                     if (mouseClicked) {
                        currentMenu = null;
                        mouseClicked = false;
                        break;
                     }
                  } else if (i == tileMenu.items.length - 1) {
                     menuSelectionIndex++;
                     if (menuSelectionIndex >= customTileMenus.size()) {
                        menuSelectionIndex = 0;
                     }
                     selectionIndex = 0;
                     mouseClicked = false;
                     leftMouse = false;
                     tileMenu = customTileMenus.get(menuSelectionIndex);
                     currentMenu = tileMenu;
                     tileSelection = new Tile[tileMenu.items.length - 2];
                     for (int j = 0; j < tileMenu.items.length - 2; j++) {
                        tileSelection[j] = new Tile(tileMenu.items[j + 1], getTags(tileMenu.items[j + 1]));
                     }
                  } else {
                     selectionIndex = i - 1;
                     if (tileSelection[selectionIndex].hasTag("has_properties")) {
                        currentMenu.setSubMenu(new Menu<>(new Object[tileSelection[selectionIndex].getPropertyValues().length + 1], 64, 64, false));
                     } else {
                        currentMenu.subMenu = null;
                     }
                  }
               }
               subMenuX = x + 1;
            }
            if (currentMenu != null && currentMenu.hasSubMenu()) {
               int minY = 1;
               maxY = (TileEditor.window.getHeight() - 72 * 2) / 72;
               for (int i = 0; i < currentMenu.subMenu.items.length; i++) {
                  int x = i / maxY + subMenuX;
                  int y = i % maxY + minY;
                  // System.out.println(x + ", " + y + ". " + i + ", " + maxY);
                  Rectangle button = new Rectangle(x * 72, y * 72, 64, 64);
                  if (button.contains(MouseUtils.pointerScreenLocation())) {
                     if (i == 0) { 
                        if (mouseClicked) {
                           currentMenu.subMenu = null;
                           leftMouse = false; 
                           break;
                        }
                     } else {
                        tileSelection[selectionIndex].setPropertyValue(i - 1);
                     }
                  }
               }
            }
         }
      } else if (currentMenu == settingsMenu) {
         currentMenu.setMenuRectangle(TileEditor.program.getWidth() - 68 * currentMenu.items.length - 4, TileEditor.program.getHeight() - 72, 1, false);
         if (mouseClicked) {
            int buttonIndex = settingsMenu.getOverlappedElement(MouseUtils.pointerScreenLocation());
            if (buttonIndex >= 0) {
               if (buttonIndex == settingsMenu.items.length - 1) {
                  currentMenu = null;
               } else {
                  mapSettings[buttonIndex] = !mapSettings[buttonIndex];
                  setUnsaved();
               }
            }
         }
      }
      
      if (mapSettings[SELECT_BACKGROUND]) {
         if (leftMouse) {
            int buttonIndex = backgroundMenu.getOverlappedElement(MouseUtils.pointerScreenLocation());
            if (buttonIndex != -1) {
               background = backgroundMenu.items[buttonIndex];
            }
         }
      }
      
      prevMousePos = MouseUtils.pointerCameraLocation();
      mouseClicked = false;         
   }
   
   public Tile getTile(String image, int x, int y) {
      return new Tile(x, y, image, 0, getTags(image));
   }
   
   public Point getMouseTile() {
      Point location = MouseUtils.getGridSnappedLocation();
      location.x /= 64;
      location.y /= 64;
      return location;
   }
         
   public Rectangle menuRectangle() {
      if (currentMenu == null) 
         return new Rectangle(0, 0, 0, 0);
      if (currentMenu == settingsMenu) 
         return settingsMenu.getMenuRectangle();
      int maxY = (TileEditor.window.getHeight() - 72 * 2) / 72 + 1;
      return new Rectangle(-1, -1, 72 * ((currentMenu.items.length - (customTileMenus.size() > 1 ? 1 : 2)) / maxY + 1) + 2, maxY * 72 + 2);
   }
   
   public Rectangle toolMenuRectangle() {
      if (toolMenu == null) 
         return new Rectangle(0, 0, 0, 0);
      int maxY = 72 * toolMenu.items.length;
      int offsetX = TileEditor.window.getWidth() - 82;
      return new Rectangle(offsetX, 0, 83, maxY);
   }
   
   public Rectangle subMenuRectangle() {
      if (currentMenu == null || !currentMenu.hasSubMenu()) 
         return new Rectangle(0, 0, 0, 0);
      int maxY = (TileEditor.window.getHeight() - 72 * 2) / 72;
      return new Rectangle(menuRectangle().width, 73, 72 * ((currentMenu.subMenu.items.length - 1) / maxY + 1) + 2, maxY * 72 + 2);
   }
   
   public void modifyExtra(Tile tile) {
      modifyExtra = tile;
      typeInsertPos = tile.extra.length();
   }
   
   @Override
   public void render(Graphics2D g) {
      if (map != null && finishedLoading) {
         if (mapSettings[BACKGROUND_VISIBLE]) {
            renderBackground(g);
            g.setColor(new Color(0, 0, 0, 127));
            if (player == null) g.fillRect(0, 0, TileEditor.program.getWidth(), TileEditor.program.getHeight());
//             g.setColor(new Color(0, 0, 0, 127));
//             g.fillPolygon(new int[] { 0, screenWidth, screenWidth, 0, Math.max(0, mapX - screenWidth) }, new int[] { 0, 0, screenHeight, Math.max(0, mapY - screenHeight), Math.max(0, mapY - screenHeight) }, 5);
         } else {
            g.setColor(TileEditor.getBackgroundColor());
            g.fillRect(0, 0, TileEditor.program.getWidth(), TileEditor.program.getHeight());
         }
      } else {
         g.setColor(TileEditor.getBackgroundColor());
         g.fillRect(0, 0, TileEditor.program.getWidth(), TileEditor.program.getHeight());
      }
      super.render(g);
      if (!finishedLoading) 
         return;
      for (int i = 0; i < playerTrace.size(); i += 4) {
         Point point = playerTrace.get(i);
         camera.drawImage(g, new Rectangle(point.x - 8, point.y - 16, 64, 64), new Rectangle(0, 0, 8, 8), Images.getImage("h_trace.png"));
      }
      if (player == null) {
         if (getMousePlaceable()) {
            camera.drawRect(g, MouseUtils.getGridSnappedLocation().x, MouseUtils.getGridSnappedLocation().y, 64, 64, Color.RED);   
         } else {
            
         }
         if (toolMenu != null) {
            int maxY = (TileEditor.window.getHeight() - 72 * 2) / 72 + 1;
            int offsetX = TileEditor.window.getWidth() - 80;
            Rectangle rect = toolMenuRectangle();
            g.setColor(TileEditor.getSecondaryColor());
            g.fillRect(rect.x - 1, rect.y - 1, rect.width, rect.height);
            g.setColor(TileEditor.getMainColor());
            g.fillRect(rect.x, rect.y, rect.width - 2, rect.height - 2);
            for (int i = 0; i < toolMenu.items.length; i++) {
               // int x = i / maxY;
               int y = i % maxY;
               // System.out.println(x + ", " + y + ". " + i + ", " + maxY);
               if (i == toolIndex) {
                  g.setColor(TileEditor.getSecondaryColor());
                  g.fillRect(offsetX - 1, 1 + y * 72, 68, 68);
               }
               Images.drawImage(g, Images.getImage(toolMenu.getItem(i)), new Rectangle(offsetX + 2, y * 72 + 2, 64, 64), new Rectangle(0, 0, 8, 8));
            }
         }
      
         if (currentMenu == tileMenu) {
            int subMenuX = 0;
            int maxY = (TileEditor.window.getHeight() - 72 * 2) / 72 + 1;
            g.setColor(TileEditor.getSecondaryColor());
            g.fillRect(0, 0, 72 * ((currentMenu.items.length - 1) / maxY + 1) + 4, maxY * 72 + 4);
            g.setColor(TileEditor.getMainColor());
            g.fillRect(1, 1, 72 * ((currentMenu.items.length - 1) / maxY + 1) + 2, maxY * 72 + 2);
            for (int i = 0; i < currentMenu.items.length; i++) {
               int x = i / maxY;
               int y = i % maxY;
               // System.out.println(x + ", " + y + ". " + i + ", " + maxY);
               if (i == selectionIndex) {
                  int relX = (i + 1) / maxY;
                  int relY = (i + 1) % maxY;
                  g.setColor(TileEditor.getSecondaryColor());
                  g.fillRect(2 + relX * 72, 2 + relY * 72, 68, 68);
               }
               if (customTileMenus.size() > 1 && i == currentMenu.items.length - 1) {
                  Images.drawImage(g, Images.getImage("forward_arrow.png"), new Rectangle(4 + x * 72, 4 + y * 72, 64, 64), new Rectangle(0, 0, 8, 8));
               } else if (i < currentMenu.items.length - 1) {
                  Images.drawImage(g, new Tile((String) currentMenu.getItem(i)).getImage(), new Rectangle(4 + x * 72, 4 + y * 72, 64, 64), i < tileSelection.length ? tileSelection[Math.max(0, i - 1)].getDrawSection() : new Rectangle(0, 0, 8, 8));
                  if (i < tileSelection.length && tileSelection[Math.max(0, i - 1)].hasTag("has_properties")) {
                     Images.drawImage(g, Images.getImage("properties_gear.png"), new Rectangle(4 + x * 72, 4 + y * 72, 64, 64), new Rectangle(0, 0, 8, 8));
                  }
               }
               subMenuX = x + 1;
            }
            if (currentMenu != null && currentMenu.hasSubMenu()) {
               int minY = 1;
               maxY = (TileEditor.window.getHeight() - 72 * 2) / 72;
               g.setColor(TileEditor.getSecondaryColor());
               g.fillRect(8 + subMenuX * 72, 8 + minY * 72, 72 * ((currentMenu.subMenu.items.length - 1) / maxY + 1), maxY * 72 - minY * 72 + 72);
               g.setColor(TileEditor.getMainColor());
               g.fillRect(8 + subMenuX * 72 + 1, 8 + minY * 72 + 1, 72 * ((currentMenu.subMenu.items.length - 1) / maxY + 1) - 2, maxY * 72 +70 - minY * 72);
               Tile currentTile = tileSelection[selectionIndex];
               for (int i = 0; i < currentMenu.subMenu.items.length; i++) {
                  int x = i / maxY + subMenuX;
                  int y = i % maxY + minY;
                  // System.out.println(x + ", " + y + ". " + i + ", " + maxY);
                  if (i == tileSelection[selectionIndex].getPropertyIndex()) {
                     int relX = (i + 1) / maxY + subMenuX;
                     int relY = (i + 1) % maxY + minY;
                     g.setColor(TileEditor.getSecondaryColor());
                     g.fillRect(10 + relX * 72, 8 + 2 + relY * 72, 68, 68);
                  }
                  if (i == 0) {
                     Images.drawImage(g, Images.getImage("back_arrow.png"), new Rectangle(12 + x * 72, 8 + 4 + y * 72, 64, 64), new Rectangle(0, 0, 8, 8));
                  } else {
                     Tile currentTileClone = currentTile.copy();
                     currentTileClone.setPropertyValue(currentTile.getPropertyValues()[Math.max(0, Math.min(currentTile.getPropertyValues().length - 1, i - 1))]);
                     Images.drawImage(g, currentTile.getImage(), new Rectangle(12 + x * 72, 12 + y * 72, 64, 64), currentTileClone.getDrawSection());
                  }
               }
            }
         } else if (currentMenu == settingsMenu) {
            Rectangle menuRect = currentMenu.getMenuRectangle();
            g.setColor(TileEditor.getSecondaryColor());
            g.fillRect(menuRect.x, menuRect.y, menuRect.width, menuRect.height + 4);
            g.setColor(TileEditor.getMainColor());
            g.fillRect(menuRect.x + 1, menuRect.y + 1, menuRect.width - 1, menuRect.height + 2);
            
            Rectangle[] menuButtons = currentMenu.getItemButtons();
            for (int i = 0; i < currentMenu.items.length; i++) {
               Rectangle button = menuButtons[i];
               if (i > 2 && i < mapSettings.length || i == 1) {
                  if (mapSettings[i]) {
                     g.setColor(Color.GREEN);
                  } else {
                     g.setColor(Color.RED);
                  }
                  g.fillRect(button.x - 2, button.y - 2, button.width + 4, button.height + 4);
               }
               Images.drawImage(g, Images.getImage(currentMenu.items[i]), button, new Rectangle(0, 0, 8, 8));
            }
         }
         if (currentMenu != tileMenu) {
            g.setColor(TileEditor.getSecondaryColor());
            g.fillRect(0, 0, 68, 68);
            g.setColor(TileEditor.getMainColor());
            g.fillRect(1, 1, 66, 66);
            Images.drawImage(g, tileSelection[selectionIndex].getImage(), new Rectangle(2, 2, 64, 64), tileSelection[selectionIndex].getDrawSection());
         }
         if (currentMenu != settingsMenu) {
            g.setColor(TileEditor.getSecondaryColor());
            g.fillRect(settingsButton.x, settingsButton.y, settingsButton.width, settingsButton.height);
            g.setColor(TileEditor.getMainColor());
            g.fillRect(settingsButton.x + 1, settingsButton.y + 1, settingsButton.width - 2, settingsButton.height - 2);
            Images.drawImage(g, Images.getImage("ui/gear.png"), new Rectangle(settingsButton.x + 2, settingsButton.y + 2, settingsButton.width - 4, settingsButton.height - 4), new Rectangle(0, 0, 8, 8));
         }
         if (mapSettings[SELECT_BACKGROUND]) {
            backgroundMenu.setElementDimensions(256, 144, 8);
            backgroundMenu.setMenuRectangle(8, 8, TileEditor.program.getHeight() / 152 * 152, false);
            
            Rectangle menuRect = backgroundMenu.getMenuRectangle();
            g.setColor(TileEditor.getSecondaryColor());
            g.fillRect(menuRect.x - 4, menuRect.y - 4, menuRect.width + 8, menuRect.height + 8);
            g.setColor(TileEditor.getMainColor());
            g.fillRect(menuRect.x - 2, menuRect.y - 2, menuRect.width + 4, menuRect.height + 4);
            
            Rectangle[] menuButtons = backgroundMenu.getItemButtons();
            for (int i = 0; i < menuButtons.length; i++) {
               Rectangle button = menuButtons[i];
               button.x -= 4;
               button.y -= 4;
               if (background.equals(backgroundMenu.items[i])) {
                  g.setColor(Color.WHITE);
                  g.fillRect(button.x - 2, button.y - 2, button.width + 4, button.height + 4);
               }
               Images.drawImage(g, Images.getImage("backgrounds/" + backgroundMenu.items[i] + "/whole.png"), button, new Rectangle(0, 0, 256, 144));
            }
         }
      } else {
         updateWindowZoom();
         renderTimer(g);
      }
      
      if (modifyExtra != null) {
         String prompt = "Enter extra tile data";
         if (modifyExtra.hasTag("text")) {
            prompt = "Enter display text";
         } else if (modifyExtra.hasTag("dialogue")) {
            prompt = "Enter dialogue key";
         }
         String text = modifyExtra.extra;
         text = new StringBuilder(text).insert(typeInsertPos, TileEditor.program.tick % 60 < 30 ? "<" : "  ").toString();
         Rectangle rect = TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2 - 32, 20, prompt, TileEditor.getSecondaryColor(), 0);
         Rectangle otherRect = TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2, 24, text, TileEditor.getSecondaryColor(), 0);
         int x = Math.min(rect.x, otherRect.x);
         int y = Math.min(rect.y, otherRect.y);
         int width = Math.max(rect.width, otherRect.width);
         int height = Math.max(rect.height, otherRect.height) + otherRect.y - rect.y;
         
         g.setColor(TileEditor.getSecondaryColor());
         g.fillRect(x - 8, y - 8, width + 16, height + 16);
         g.setColor(TileEditor.getMainColor());
         g.fillRect(x - 4, y - 4, width + 8, height + 8);
         TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2 - 32, 20, prompt, TileEditor.getSecondaryColor(), 0);
         TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2, 24, text, TileEditor.getSecondaryColor(), 0);
      }
      if (timerSet != null) {
         Rectangle rect = TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2 - 32, 20, "Number of seconds the player has to beat the level ( infinite)", TileEditor.getSecondaryColor(), 0);
         Rectangle otherRect = TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2, 24, timerSet, TileEditor.getSecondaryColor(), 0);
         int x = Math.min(rect.x, otherRect.x);
         int y = Math.min(rect.y, otherRect.y);
         int width = Math.max(rect.width, otherRect.width);
         int height = Math.max(rect.height, otherRect.height) + otherRect.y - rect.y;
         
         g.setColor(TileEditor.getSecondaryColor());
         g.fillRect(x - 8, y - 8, width + 16, height + 16);
         g.setColor(TileEditor.getMainColor());
         g.fillRect(x - 4, y - 4, width + 8, height + 8);
         TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2 - 32, 20, "Number of seconds the player has to beat the level (leave blank for infinite)", TileEditor.getSecondaryColor(), 0);
         TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2, 24, timerSet, TileEditor.getSecondaryColor(), 0);
      }
   }
   
   @Override
   public void reset() {
      startEditing();
   }
}