package src.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import src.TileEditor;
import src.Particle;
import src.Images;
import src.util.Camera;
import src.util.MouseUtils;
import src.util.TextUtils;
import src.util.Settings;
import src.util.SABDecoder;
import src.map.enemy.Enemy;

public class GameState {
   // Setting values
   public static final int SELECT_BACKGROUND = 0;
   public static final int BACKGROUND_VISIBLE = 1;
   public static final int CHANGE_TIME_LIMIT = 2;
   public static final int ALLOW_AIR_JUMP = 3;
   public static final int ALLOW_WALLSLIDE = 4;
   public static final int ALLOW_CROUCH = 5;

   public boolean shiftPressed, controlPressed, saveMap, saveTBHL, leftMouse, rightMouse, mouseClicked;
   public String name;
   public int gameTick;
   protected boolean saved;

   public Camera camera;
   public Player player;
   public File referenceFile;
   public String author;
   protected InputStream referenceStream;
   protected String referenceStreamPath;

   protected long timer, speedrunTimer, checkedTime;
   
   public static boolean finishedLoading = false;
   protected static int tilesLoaded = 0;
   protected static int totalLines = 0;
   protected static boolean finalizing, updateReadTiles, hasVersion = false;
   
   protected List<Particle> particles;
   protected List<Particle> newParticles;
   
   public Tile[][] map;
   
   // Settings
   public boolean[] mapSettings;
   public int timeLimit, timeLeft;
   public String background;

   public Set<Tile> allTiles;
   public Set<Tile> checkpointState;
   public Set<Tile> volatileTiles;
   public Set<Tile> notifiableTiles;
   public Set<Tile> updatableTiles;
   public java.util.Map<Tile, Enemy> enemies;
   protected List<Point> playerTrace;
   protected Point startPosition;
   protected Popup currentPopup;
   protected double mapVersion;
   protected int timeUp;
   private boolean panningCamera;
   
   public GameState(String referenceStreamPath) {
      this();
      this.referenceStreamPath = referenceStreamPath;
      this.referenceStream = GameState.class.getResourceAsStream(this.referenceStreamPath);
      loadMap();
   }
   
   public GameState(File referenceFile) {
      this();
      this.referenceFile = referenceFile;
      loadMap();
   }
   
   public GameState() {
      camera = new Camera();
      
      allTiles = new HashSet<>();
      volatileTiles = new HashSet<>();
      notifiableTiles = new HashSet<>();
      checkpointState = new HashSet<>();
      updatableTiles = new HashSet<>();
      enemies = new HashMap<>();
      
      particles = new ArrayList<>();
      newParticles = new ArrayList<>();
      
      playerTrace = new ArrayList<>();
      
      currentPopup = null;    
      panningCamera = true;
      // Settings, from left to right: Change Background, Background visible, Change time limit, Allow double jump, Allow wall sliding/jumping, Allow crouching/sliding. 
      mapSettings = new boolean[]{ false, Settings.getBooleanSetting("default_background_visibility"), false, true, true, true };
      background = "mountains";
      saved = true;
      timeLimit = -1;
      timeLeft = 0;
      gameTick = 0;
      timeLimit = 0;
   }
   
   public void update() {
      gameTick++;
      if (timeUp > 0) {
         currentPopup = new Popup("OUT OF TIME!", 60);
      } else {
         checkTimer();
      }
      if (player != null) for (Tile tile : updatableTiles) {
         float playerDist = (float) Math.sqrt((player.x - tile.x * 64) * (player.x - tile.x * 64) + (player.y - tile.y * 64) * (player.y - tile.y * 64));
         if (playerDist < 1480) tile.notify(this, "notify_update", new int[] {(int) playerDist});
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
      if (player != null) {
         player.update(this);
         if (player == null) {
         } else {
            if (!player.win) camera.setTargetCenter(player.x + player.width / 2, player.y + player.height / 2);
            if (shiftPressed) {
               if (new Rectangle(-1, -1, TileEditor.program.getWidth() + 2, TileEditor.program.getHeight() + 2).contains(MouseUtils.pointerScreenLocation())) {
                  camera.setCenter(player.x + player.width / 2, player.y + player.height / 2);
                  camera.setCenter(MouseUtils.pointerCameraLocation().x, MouseUtils.pointerCameraLocation().y);
                  TileEditor.window.getContentPane().setCursor(Toolkit.getDefaultToolkit().createCustomCursor(Images.getImage("ui/cursor/magnifier_" + (TileEditor.darkMode ? "dark" : "light") + ".png"), new Point(0, 0), "zoom"));
                  if (!panningCamera) {
                     try {
                        Point screenPos = TileEditor.window.getLocationOnScreen();
                        new Robot().mouseMove(screenPos.x + TileEditor.program.getWidth() / 2 + 12, screenPos.y + TileEditor.program.getHeight() / 2 + 42);
                     } catch (Exception e) {
                        throw new RuntimeException(e);
                     }
                     panningCamera = true;
                  }
               }
            } else {
               if (panningCamera) {
                  TileEditor.window.getContentPane().setCursor(TileEditor.blankCursor);
                  try {
                     Point screenPos = TileEditor.window.getLocationOnScreen();
                     new Robot().mouseMove(screenPos.x + TileEditor.program.getWidth() / 2 + 12, screenPos.y + TileEditor.program.getHeight() / 2 + 42);
                  } catch (Exception e) {
                     throw new RuntimeException(e);
                  }
                  panningCamera = false;
               }
            }
         }
      }
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
      mouseClicked = false;
   }
   
   public void reset() {
      syncTilemap();
      loadMap();
      player = new Player(startPosition, this);
      timeUp = 0;
      enemies.clear();
      if (timeLimit > -1) startPopup("Time limit: " + timeLimit + " seconds", 120);
      timeLeft = timeLimit;
   }
   
   public void syncTilemap() {
      for (Tile tile : allTiles) {
         map[tile.x][tile.y] = tile;
         volatileTiles.clear();
         updatableTiles.clear();
         // notifiableTiles.clear();
      }
   }
   
   public void desyncTilemap() {
      playerTrace.clear();
      volatileTiles.clear();
      notifiableTiles.clear();
      updatableTiles.clear();
      for (Tile tile : allTiles) {
         Tile copy = tile.copy();
         map[copy.x][copy.y] = copy;;
         if (tile.hasTag("volatile")) {
            volatileTiles.add(copy);
         }
         if (tile.hasTag("notifiable")) {
            notifiableTiles.add(copy);
         }
         if (tile.hasTag("update")) {
            updatableTiles.add(tile);
         }
      }
      saveCheckpointState();
   }
   
   public void checkTimer() {
      // System.out.println(((long) timeLimit * 1000) + ", " + (System.currentTimeMillis() - speedrunTimer));
      if (timeLimit == -1 || player == null) return;
      if (gameTick % 60 == 0) timeLeft--;
      if (timeLeft < 1) {
         timeUp = 1;
         player.trueKill();
      }
   }
   
   public void loadMap() {
      finishedLoading = false;
      finalizing = false;
      updateReadTiles = false;
      hasVersion = false;
      Scanner scanner = null;
      
      // Reads the properties at the top of the file
      HashMap<String, String> properties;
      if (referenceStream == null) {
         properties = SABDecoder.decode(referenceFile);
      } else {
         properties = SABDecoder.decode(referenceStream);
      }
      
      // Handles getting the version      
      if (properties.containsKey("version")) {
         String version = properties.get("version");
         String[] wholeVersion = version.split("\\.");
         String majorVersion = wholeVersion[0] + "." + wholeVersion[1];
         mapVersion = Double.parseDouble(majorVersion);
      }
      
      // Handles getting the name
      if (properties.containsKey("name")) {
         name = properties.get("name");
         TileEditor.window.setTitle(TileEditor.WINDOW_TITLE + ": Playing level \"" + name + "\"");
      }
      
      // Handles getting the configurable settings
      if (properties.containsKey("background")) {
         background = properties.get("background");
      }
      
      // Handles time limit
      if (properties.containsKey("time_limit")) {
         try {
            timeLimit = Integer.parseInt(properties.get("time_limit"));
         } catch (Exception e) {
            endGame();
            TileEditor.error("Level's time left is not an integer. Open the file to troubleshoot.");
            return;
         }
      }
            
      // Handles movement options
      if (properties.containsKey("movement_options")) {
         String[] movementOptions = SABDecoder.decodeArray(properties.get("movement_options"));
         if (movementOptions == null) {
            mapSettings[ALLOW_AIR_JUMP] = true;
            mapSettings[ALLOW_WALLSLIDE] = true;
            mapSettings[ALLOW_CROUCH] = true;
         } else {
            try {
               mapSettings[ALLOW_AIR_JUMP] = Boolean.parseBoolean(movementOptions[0]);
               mapSettings[ALLOW_WALLSLIDE] = Boolean.parseBoolean(movementOptions[1]);
               mapSettings[ALLOW_CROUCH] = Boolean.parseBoolean(movementOptions[2]);
            } catch (Exception e) {
               endGame();
               TileEditor.error("One or more of level's movement options are not valid booleans. Open the file to troubleshoot.");
               return;
            }
         }
      }
            
      // Handles the author name
      if (properties.containsKey("author")) {
         author = properties.get("author");
      } else {
         author = Settings.getStringSetting("nickname");
      }
      
      Thread load = new Thread() {   
         @Override
         public void run() {
            readFile();
            // finishedLoading = false;
         }
      };
      try { load.start(); } catch (Exception e) { throw new RuntimeException(e); };
   }
   
   public void readFile() {
      int width = 16;
      int height = 16;
      Scanner scanner = null;
      try { if (referenceStream != null) referenceStream.close(); } catch (Exception e) { e.printStackTrace(); }
      if (referenceStreamPath != null) referenceStream = getClass().getResourceAsStream(referenceStreamPath);
      if (referenceFile != null) try { scanner = new Scanner(referenceFile); } catch (Exception e) { e.printStackTrace(); }
      else try { scanner = new Scanner(referenceStream); } catch (Exception e) { e.printStackTrace(); }
      
      while (scanner.hasNext()) {
         // System.out.println("line");
         totalLines++;
         scanner.nextLine();
      }
      scanner.close();
      try { if (referenceStream != null) referenceStream.close(); } catch (Exception e) { e.printStackTrace(); }
      if (referenceStreamPath != null) referenceStream = getClass().getResourceAsStream(referenceStreamPath);
      if (referenceFile != null) try { scanner = new Scanner(referenceFile); } catch (Exception e) { e.printStackTrace(); }
      else try { scanner = new Scanner(referenceStream); } catch (Exception e) { e.printStackTrace(); }

      String line = scanner.nextLine();

      while (line.startsWith("@")) {
         line = scanner.nextLine();
      }

      while (line != null) {
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
         else allTiles.add(new Tile(Integer.parseInt(data[0]), Integer.parseInt(data[1]), data[2], type, TileEditor.customTiles.containsKey(data[2]) ? getCustomTags(data[2]) : getTags(data[2]), extra));
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
      finalizing = false;
      map = new Tile[width][height];
      ArrayList<Tile> duplicates = new ArrayList<>();
//       System.out.println(map.length + ", " + map[0].length);
      for (Tile tile : allTiles) {
         if (map[tile.x][tile.y] != null) {
            duplicates.add(tile);
         } else {
            if (tile.hasTag("spawn")) {
               camera.setTargetCenter(tile.x * 64, tile.y * 64);
               startPosition = new Point(tile.x * 64, tile.y * 64);
            }
            if (tile.hasTag("volatile")) {
               volatileTiles.add(tile);
            }
            if (tile.hasTag("notifiable")) {
               notifiableTiles.add(tile);
            }
            // tile.setTags(getTags(tile.image));
            map[tile.x][tile.y] = tile;
         }
      }
      allTiles.removeAll(duplicates);
      try { if (referenceStream != null) referenceStream.close(); } catch (Exception e) { e.printStackTrace(); }
      finishedReading();
   }
   
   public void finishedReading() {
      finishedLoading = true;
      if (timeLimit > -1) {
         startPopup("Time Limit: " + timeLimit + " seconds", 120);
      }
      desyncTilemap();
   }
   
   public void saveCheckpointState() {
      checkpointState.clear();
      for (Tile tile : volatileTiles) {
         checkpointState.add(tile);
      }
      for (Tile tile : notifiableTiles) {
         if (tile.hasTag("changes_state") && !checkpointState.contains(tile)) checkpointState.add(tile.copy());
      }
   }
   
   public void resetToCheckpointState() {
      volatileTiles.clear();
      notifiableTiles.clear();
      updatableTiles.clear();
      enemies.clear();
      for (Tile tile : checkpointState) {
         if (tile.hasTag("volatile")) {
            volatileTiles.add(tile);
         }
         if (tile.hasTag("notifiable")) {
            notifiableTiles.add(tile);
         }
         if (tile.hasTag("update")) {
            updatableTiles.add(tile);
         }
         map[tile.x][tile.y] = tile;
      }  
      saveCheckpointState();    
   }
   
   public void startPopup(String message) {
      startPopup(message, 60);
   }
   
   public void startPopup(String message, int duration) {
      currentPopup = new Popup(message, duration);
   }
   
   public void notify(String tag, int[] data) {
      for (Tile tile : notifiableTiles) {
         tile.notify(this, tag, data);
      }
   }
   
   public void notify(String tag) {
      notify(tag, new int[0]);
   }
   
   // Auto updater for versions of 0.6 and up
   public static String getCurrentVersionEquivalent(String image) {
      String[] sets = new String[]{ "grass", "stone", "snowy_turf", "rock", "malice", "location_bricks", "ice", "bounce", "tileset", "slick" };
      for (int i = 0; i < sets.length; i++) {
         if (image.contains(sets[i])) return "tiles/sets/" + sets[i];
      }
      String[] sprites = new String[]{ "strong_checkpoint", "dialogue_trigger", "checkpoint", "enemy", "timer", "coin_box", "coin", "color_cube", "power_fruit", "end", "h_fragment", "half_spike", "invisible_death", "invisiblock", "one_way", "spawn", "text", "evil_key_box", "key_box", "evil_key", "key" };
      for (int i = 0; i < sprites.length; i++) {   
         if (image.contains(sprites[i])) return "tiles/" + sprites[i];
      }
      return Images.getImage("tiles/" + image) != null ? "tiles/" + image : Images.getImage("tiles/sets" + image) != null ? "tiles/sets" + image : "tiles/sets/grass";
   }
   
   // The Big H file saver
   public String getTBHEquivalent(String image) {
      image = removePath(image);
      if (image.equals("grass")) return "1Terrain1Grass";
      if (image.equals("stone")) return "1Terrain2Stone";
      if (image.equals("ice")) return "1Terrain3Ice";
      if (image.equals("malice")) return "2Danger1Malice";
      if (image.equals("checkpoint")) return "3Special1Respawn";
      if (image.equals("end")) return "3Special2End";
      if (image.equals("bounce")) return "3Special3Bounce";
      if (image.equals("spawn")) return "3Special4Start";
      return "1Terrain1Grass";
   }
   
   public String nameAndVersion() {
      return "@version " + TileEditor.VERSION + "\n@name " + name + "\n";
   }
   
   public void setSaved() {
      saved = true;
      TileEditor.window.setTitle(TileEditor.WINDOW_TITLE + ": Editing Map \"" + name + "\"");
   }
   
   public void setUnsaved() {
      saved = false;
      TileEditor.window.setTitle(TileEditor.WINDOW_TITLE + ": Editing Map \"" + name + "\" (Unsaved)");
   }
   
   public void saveToFile() {
      referenceFile.delete();
      File file = referenceFile;
      try {
         FileWriter fileWriter = new FileWriter(file, false);
         fileWriter.write(nameAndVersion());
         fileWriter.write("@background " + background + "\n");
         fileWriter.write("@time_limit " + timeLimit + "\n");
         fileWriter.write("@movement_options [ " + mapSettings[ALLOW_AIR_JUMP] + ", " + mapSettings[ALLOW_WALLSLIDE] + ", " + mapSettings[ALLOW_CROUCH] + " ]\n");
         fileWriter.write("@author " + author + "\n");
         for (Tile tile : allTiles) {
            fileWriter.write(tile.x + " " + tile.y + " " + tile.image + " " + tile.tileType + " " + tile.extra + "\n");
         }
         fileWriter.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
      setSaved();
      saveMap = false;
   }

   
   public String[] getCustomTags(String correspondingImage) {
      return TileEditor.customTiles.get(correspondingImage).getTags();
   }
   
   public String[] getTags(String correspondingImage) {
      if (TileEditor.customTiles.containsKey(correspondingImage)) return getCustomTags(correspondingImage);
      correspondingImage = removePath(correspondingImage);
      if (correspondingImage.equals("spawn")) {
         return new String[]{ "spawn" };
      } else if (correspondingImage.equals("grass") || correspondingImage.equals("stone") || correspondingImage.equals("snowy_turf") || correspondingImage.equals("rock") || correspondingImage.equals("tileset") || correspondingImage.equals("location_bricks")) {
         return new String[]{ "solid" };
      } else if (correspondingImage.equals("ice")) {
         return new String[]{ "solid", "slippery" };
      } else if (correspondingImage.equals("end")) {
         return new String[]{ "end", "pickup", "volatile" };
      } else if (correspondingImage.equals("checkpoint")) {
         return new String[]{ "checkpoint" };
      } else if (correspondingImage.equals("malice")) {
         return new String[]{ "death", "small" };
      } else if (correspondingImage.equals("bounce")) {
         return new String[]{ "bounce" };
      } else if (correspondingImage.equals("coin")) {
         return new String[]{ "pickup", "volatile", "small", "coin", "4_set", "has_properties", "property_set" };
      } else if (correspondingImage.equals("half_spike")) {
         return new String[]{ "half", "death", "has_properties", "4_set", "property_set" };
      } else if (correspondingImage.equals("text")) {
         return new String[]{ "text", "has_properties", "3_set", "property_set", "invisible", "modify_extra" };
      } else if (correspondingImage.equals("invisible_death")) {
         return new String[]{ "death", "invisible", "small" };
      } else if (correspondingImage.equals("color_cube")) {
         return new String[]{ "has_properties", "15_set", "property_set" };
      } else if (correspondingImage.equals("invisiblock")) {
         return new String[]{ "solid", "invisible", "no_wallslide" };
      } else if (correspondingImage.equals("one_way")) {
         return new String[]{ "solid", "one_way", "4_set", "property_set", "has_properties" };
      } else if (correspondingImage.equals("key")) {
         return new String[]{ "pickup", "volatile", "key" };
      } else if (correspondingImage.equals("key_box")) {
         return new String[]{ "solid", "volatile", "key_box" };
      } else if (correspondingImage.equals("slick_block")) {
         return new String[]{ "solid", "no_wallslide" };
      } else if (correspondingImage.equals("evil_key")) {
         return new String[]{ "pickup", "volatile", "key", "evil" };
      } else if (correspondingImage.equals("evil_key_box")) {
         return new String[]{ "solid", "volatile", "key_box", "evil" };
      } else if (correspondingImage.equals("timer")) {
         return new String[]{ "pickup", "has_properties", "timer", "4_set", "property_set" };
      } else if (correspondingImage.equals("h_fragment")) {
         return new String[]{ "end", "has_properties", "pickup", "volatile", "property_set", "7_set" };
      } else if (correspondingImage.equals("strong_checkpoint")) {
         return new String[]{ "checkpoint", "notifiable", "notify_collect_checkpoint", "notified_reset_checkpoint", "volatile" };
      } else if (correspondingImage.equals("coin_box")) {
         return new String[]{ "has_properties", "property_set", "8_set", "notifiable", "coin_box", "changes_state" };
      } else if (correspondingImage.equals("power_fruit")) {
         return new String[]{ "has_properties", "property_set", "4_set", "pickup", "volatile", "powerup" };
      } else if (correspondingImage.equals("enemy")) {
         return new String[]{ "has_properties", "update", "property_set", "3_set", "enemy", "volatile", "notifiable", "notified_spawn_enemy", "invisible" };
      } else if (correspondingImage.equals("dialogue_trigger")) {
         return new String[]{ "modify_extra", "pickup", "dialogue", "invisible" };
      }
      return new String[0];
   }
   
   public static String removePath(String string) {
      String[] splitString = string.split("/");
      return splitString[splitString.length - 1];
   }
   
   public void createEnemy(Tile parent) {
      Enemy enemy = parent.tileType == 0 ? new src.map.enemy.E(parent.x, parent.y, player, parent) : parent.tileType == 1 ? new src.map.enemy.A(parent.x, parent.y, player, parent) : new src.map.enemy.F(parent.x, parent.y, player, parent);
      if (enemies.containsKey(parent)) return;
      enemies.put(parent, enemy);
   }
   
   public int getVolatileTileCount(String tag, int tileType) {
      int count = 0;
      for (Tile tile : volatileTiles) {
         if (tile.hasTag(tag) && tile.tileType == tileType) count++;
      }
      return count;
   }
   
   public void showTimer() {
      long millis = (long) (gameTick / 60.0 * 1000.0);
      String time = String.format("%d:%d:%d", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)), millis % 1000);
      currentPopup = new Popup(time, 120);
      checkedTime = gameTick;
   }
   
   public void addParticle(Particle particle) {
      newParticles.add(particle);
   }
   
   public void endGame() {
      TileEditor.game = null;
      TileEditor.reset();
   }
   
   public void renderLoading(Graphics2D g) {
      String loading = "Loading";
      if (System.currentTimeMillis() % 4000 > 1000) loading += ".";
      if (System.currentTimeMillis() % 4000 > 2000) loading += ".";
      if (System.currentTimeMillis() % 4000 > 3000) loading += ".";
      int screenWidth = TileEditor.program.getWidth();
      int screenHeight = TileEditor.program.getHeight();
      TextUtils.drawText((Graphics2D) g, screenWidth / 2, screenHeight / 2, 24, loading, TileEditor.getSecondaryColor(), 0);
      String loadMessage = "Processing File";
      if (tilesLoaded > 0) loadMessage = tilesLoaded + "/" + (totalLines - 1) + " Tiles Loaded";
      if (finalizing) loadMessage = "Updating Level";
      TextUtils.drawText(g, screenWidth / 2, screenHeight / 2 + 64, 18, loadMessage, TileEditor.getSecondaryColor(), 0);
      Images.drawImage(g, Images.getImage("ui/loading_h.png"), new Rectangle(screenWidth - 128, screenHeight - 128, 128, 128), new Rectangle(0, (TileEditor.program.tick % 40) / 10 * 16, 16, 16));
   } 
   
   public void render(Graphics2D g) {
      float windowScalar = Math.max(TileEditor.program.getWidth() / 256f, TileEditor.program.getHeight() / 144f) / 6;
      camera.update();
      if (!finishedLoading) {
         g.setColor(new Color(0, 0, 0, 127));
         if (player == null) g.fillRect(0, 0, TileEditor.program.getWidth(), TileEditor.program.getHeight());
         renderLoading(g);
      } else if (map != null) {
         Rectangle viewPort = camera.getTiledViewPort();
         int startX = Math.max(viewPort.x, 0);
         int finishX = Math.min(map.length, viewPort.x + viewPort.width + 1);
         List<Tile> textElements = new ArrayList<>();
         boolean playerExists = player != null;
         for (int i = startX; i < finishX; i++) {
            int startY = Math.max(viewPort.y, 0);
            int finishY = Math.min(map[i].length, viewPort.y + viewPort.height + 1);
            for (int j = startY; j < finishY; j++) {
               Tile tile = map[i][j];
               if (!playerExists && !mapSettings[BACKGROUND_VISIBLE]) camera.fillRect(g, i * 64, j * 64, 64, 64, TileEditor.getMainColor());
               if (tile != null) {
                  if (tile.hasTag("text") && playerExists) {
                     textElements.add(tile);
                  }
                  tile.render(playerExists, camera, g);
               }
//                if (player != null) {
//                   double dist = Math.sqrt((player.getCenterX() - i * 64 - 32) * (player.getCenterX() - i * 64 - 32) + (player.getCenterY() - j * 64 - 32) * (player.getCenterY() - j * 64 - 32));
//                   int alpha = (int) Math.max(0, Math.min(255, dist / 512 * 255));
//                   camera.fillRect(g, i * 64, j * 64, 64, 64, new Color(0, 0, 0, alpha));
//                }
               if (player == null && camera.getZoom() > 0.5f) camera.drawRect(g, i * 64, j * 64, 64, 64, Color.GRAY);
            }
         }
         for (Tile tile : textElements) {
            Color color = TileEditor.getSecondaryColor();
            String text = tile.extra;
            try {
               if (text.startsWith("\\c")) {
                  String hexCode = text.substring(2, 9);
                  text = text.substring(9, text.length());
                  hexCode = hexCode.toUpperCase();
                  color = Color.decode(hexCode);
                  // System.out.println(defaultColor);
               }
            } catch (Exception e) {
               text = "Hex code formatting error";
            }
            camera.drawText(g, tile.x * 64 + 32, tile.y * 64 + 32, 8 + 8 * (3 - tile.tileType) * windowScalar, text, color, 0);
         }
         for (int i = 0; i < particles.size(); i++) {
            particles.get(i).render(g, camera);
         }
      }
      
      if (player != null) {
         for (Enemy enemy : enemies.values()) {
            enemy.render(g, this);
         }
         player.render(g, this);
      }
      
      if (currentPopup != null && currentPopup.getActive()) {
         TextUtils.drawText(g, TileEditor.program.getWidth() / 2, TileEditor.program.getHeight() / 2 - 32, 24, currentPopup.getMessage(), currentPopup.getColor(), 0);
         currentPopup.update();
      }
   }
   
   public Tile getTileAt(int tileX, int tileY) {
      if (tileX >= map.length || tileX <= 0 || tileY >= map[0].length || tileY <= 0) return null;
      return map[tileX][tileY];
   }
   
   public void updateWindowZoom() {
      float windowScalar = Math.max(TileEditor.program.getWidth() / 256f, TileEditor.program.getHeight() / 144f) / 6;
      camera.setZoom(windowScalar);
   }

   public void renderBackground(Graphics2D g) {
      if (!Settings.getBooleanSetting("parallax")) {
         g.drawImage(Images.getImage("backgrounds/" + background + "/whole.png"), 0, 0, TileEditor.program.getWidth(), TileEditor.program.getHeight(), TileEditor.program);
         return;
      }
      float backgroundScalar = Math.max(TileEditor.program.getWidth() / 256f, TileEditor.program.getHeight() / 144f);
      int patchWidth = (int) Math.ceil(256 * backgroundScalar);
      int windowOffset = (int) (TileEditor.program.getWidth() - patchWidth) / 2;
      Images.drawImage(g, Images.getImage("backgrounds/" + background + "/back.png"), new Rectangle(windowOffset, 0, patchWidth, (int) Math.ceil(144 * backgroundScalar)), new Rectangle(0, 0, 256, 144));
      if (Images.getImage("backgrounds/" + background + "/far.png") != null) {
         int xOffset = ((int) (-camera.getCenter().x * camera.getZoom()) / 40) % TileEditor.program.getWidth() + windowOffset;
         int yOffset = Math.max(0, (totalHeight() - camera.getCenter().y) / 40);
         for (int i = 0; i < 3; i++) {
            Images.drawImage(g, Images.getImage("backgrounds/" + background + "/far.png"), new Rectangle(xOffset + patchWidth * (i - 1), yOffset, (int) Math.ceil(256 * backgroundScalar), (int) Math.ceil(144 * backgroundScalar)), new Rectangle(0, 0, 256, 144));
         }
      }
      if (Images.getImage("backgrounds/" + background + "/middle.png") != null) {
         int xOffset = ((int) (-camera.getCenter().x * camera.getZoom()) / 32) % TileEditor.program.getWidth() + windowOffset;
         int yOffset = Math.max(0, (totalHeight() - camera.getCenter().y) / 32);
         for (int i = 0; i < 3; i++) {
            Images.drawImage(g, Images.getImage("backgrounds/" + background + "/middle.png"), new Rectangle(xOffset + patchWidth * (i - 1), yOffset, (int) Math.ceil(256 * backgroundScalar), (int) Math.ceil(144 * backgroundScalar)), new Rectangle(0, 0, 256, 144));
         }
      }
      if (Images.getImage("backgrounds/" + background + "/ambient.png") != null) {
         int xOffset = ((int) (-camera.getCenter().x * camera.getZoom() / 24 + gameTick * camera.getZoom() / 4)) % TileEditor.program.getWidth() + windowOffset;
         int yOffset = Math.max(0, (totalHeight() - camera.getCenter().y) / 24);
         for (int i = 0; i < 3; i++) {
            Images.drawImage(g, Images.getImage("backgrounds/" + background + "/ambient.png"), new Rectangle(xOffset + patchWidth * (i - 1), yOffset, (int) Math.ceil(256 * backgroundScalar), (int) Math.ceil(144 * backgroundScalar)), new Rectangle(0, 0, 256, 144));
         }
      }
      if (Images.getImage("backgrounds/" + background + "/front.png") != null) {
         int xOffset = ((int) (-camera.getCenter().x * camera.getZoom()) / 16) % TileEditor.program.getWidth() + windowOffset;
         int yOffset = Math.max(0, (totalHeight() - camera.getCenter().y) / 16);
         for (int i = 0; i < 3; i++) {
            Images.drawImage(g, Images.getImage("backgrounds/" + background + "/front.png"), new Rectangle(xOffset + patchWidth * (i - 1), yOffset, (int) Math.ceil(256 * backgroundScalar), (int) Math.ceil(144 * backgroundScalar)), new Rectangle(0, 0, 256, 144));
         }
         for (int i = 0; i < 3; i++) {
            Images.drawImage(g, Images.getImage("backgrounds/" + background + "/super_front.png"), new Rectangle(xOffset + patchWidth * (i - 1), (int) Math.min(0, yOffset - 144 * backgroundScalar), (int) Math.ceil(256 * backgroundScalar), (int) Math.ceil(144 * backgroundScalar)), new Rectangle(0, 0, 256, 144));
         }
      }
   }
   
   public int totalHeight() {
      return (map[0].length - 1) * 64;
   }
   
   public void renderTimer(Graphics2D g) {
      long millis = (long) (gameTick / 60.0 * 1000.0);
      String time = String.format("%d:%d:%d", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)), millis % 1000);
      TextUtils.drawText(g, 20, 20, 20, time, TileEditor.getSecondaryColor(), -1);
      if (timeLimit == -1) return;
      time = String.format(timeLeft + "\u23F1", TimeUnit.SECONDS.toMinutes(timeLimit) - TimeUnit.MILLISECONDS.toMinutes(millis) - 1, 60 - (millis / 1000) % 60 - 1, 1000 - millis % 1000);
      TextUtils.drawText(g, TileEditor.program.getWidth() - 20, 20, 24, time, TileEditor.getSecondaryColor(), 1);
   }
   
   public void keyPressed(int keyCode, char character, boolean typed) {
      if (typed) return;
      switch (keyCode) {
         case KeyEvent.VK_SHIFT :
            shiftPressed = true;
            break;
         case KeyEvent.VK_CONTROL : 
            controlPressed = true;
            break;
         default :
            break; 
      }
      if (player != null) {
         playerKeyPressed(keyCode);
      }
   }
   
   public int getPlayerKey(int keyCode) {
      if (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_SPACE) return 0;
      if (keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_LEFT) return 1;
      if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_DOWN) return 2;
      if (keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_RIGHT) return 3;   
      if (keyCode == KeyEvent.VK_K) return 4;
      if (keyCode == KeyEvent.VK_ESCAPE) return 5;
      return -1;   
   } 
   
   public void playerKeyPressed(int keyCode) {
      int playerKeyCode = getPlayerKey(keyCode);
      switch (playerKeyCode) {
         case 0 :
            player.keys[0] = true;
            break;
         case 1 :
            player.keys[1] = true;
            break;
         case 2 :
            player.keys[2] = true;
            break;
         case 3 :
            player.keys[3] = true;
            break;
         case 4 :
            player.kill();
            break;
         case 5 :
            player.end = true;
            break;
         default :
            break;
      }
   }
   
   public void playerKeyReleased(int keyCode) {
      int playerKeyCode = getPlayerKey(keyCode);
      switch (playerKeyCode) {
         case 0 :
            player.keys[0] = false;
            break;
         case 1 :
            player.keys[1] = false;
            break;
         case 2 :
            player.keys[2] = false;
            break;
         case 3 :
            player.keys[3] = false;
            break;
         default :
            break;
      }
   }
   
   public void keyReleased(int keyCode) {
      switch (keyCode) {
         case KeyEvent.VK_SHIFT :
            shiftPressed = false;
            break;
         case KeyEvent.VK_CONTROL : 
            controlPressed = false;
            break;
         default :
            break; 
      }
      if (player != null) {
         playerKeyReleased(keyCode);
      }
   }
   
   public void inGameRemoveTile(Tile tile) {
      if (player != null) {
         if (tile.hasTag("volatile")) {
            volatileTiles.remove(tile);
         }
         if (tile.hasTag("update")) {
            updatableTiles.remove(tile);
         }
         map[tile.x][tile.y] = null;
      }
   }
   
   public void mousePressed(int button) {
      if (button == 1) {
         leftMouse = true;
         mouseClicked = true;
      } else if (button == 3) {
         rightMouse = true;
      }
   }
   
   public void mouseReleased(int button) {
      if (button == 1) {
         leftMouse = false;
      } else if (button == 3) {
         rightMouse = false;
      }
   }
   
   protected class Popup {
      
      private String message;
      private int duration;
   
      public Popup(String message, int duration) {
         this.message = message;
         this.duration = duration;
      }
      
      public Popup(String message) {
         this(message, 60);
      }
      
      public Color getColor() {
         Color base = TileEditor.getSecondaryColor();
         return new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (255 * ((Math.max(Math.min(duration, 30), 1)) / 30f)));
      }
      
      public String getMessage() {
         return message;
      }
      
      public boolean getActive() {
         return duration > 0;
      }
      
      public void update() {
         duration--;
      }
   }
}