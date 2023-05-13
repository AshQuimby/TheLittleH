package src.map;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.util.*;

import src.Images;
import src.util.AABB;
import src.util.Camera;

public class Tile {
   public int x;
   public int y;
   public String image;
   public String extra;
   public int tileType;
   public boolean ignoreTiling;
   private Rectangle cachedDrawRect;
   
   // This saves SO much ram
   public static Set<Set<String>> tagsCache = new HashSet<>();
   
   public Set<String> tags;
   
   private Tile(int x, int y, String image, int tileType, Set<String> tags, String extra) {
      this.x = x;
      this.y = y;
      this.image = image;
      this.tags = getCachedTags(tags);
      this.extra = extra;
      setTileType(tileType);
   }
   
   public Tile(int x, int y, String image, int tileType, String tags, String extra) {
      this(x, y, image, tileType, tags.split(","), extra);
   }
   
   public Tile(int x, int y, String image, int tileType, String[] tags, String extra) {
      this.x = x;
      this.y = y;
      this.image = image;
      this.tags = new HashSet<>();
      for (String string : tags) {
         this.tags.add(string);
      }
      this.tags = getCachedTags(this.tags);
      this.extra = extra;
      setTileType(tileType);
   }
   
   public Tile(int x, int y, String image, int tileType, String[] tags) {
      this(x, y, image, tileType, tags, "");
   }
   
   public Tile(int x, int y, String image, int tileType, String tags) {
      this(x, y, image, tileType, tags, "");
   }
   
   public Tile(int x, int y, String image, int tileType) {
      this(x, y, image, tileType, "");  
   }
   
   public Tile(String image, int tileType) {
      this(0, 0, image, tileType);
   }
   
   public Tile(String image, String[] tags) {
      this(0, 0, image, 0, tags, "");   
   }
   
   public Tile(String image) {
      this(image, 0);   
   }
   
   public void setTags(String[] tags) {
      this.tags = new HashSet<>();
      for (String string : tags) {
         this.tags.add(string);
      }
      this.tags = getCachedTags(this.tags);
   }
   
   public boolean equals(Tile other) {
      return other != null && image.equals(other.image) && (tileType == other.tileType || !ignoreTiling) && extra.equals(other.extra);
   }
   
   public static boolean tilesEqual(Tile tile, Tile other) {
      return tile == null ? other == null : tile.equals(other);
   }
   
   public static void clearTagsCache() {
      tagsCache.clear();
   }
   
   public Set<String> getCachedTags(Set<String> tags) {
      if (tagsCache.contains(tags)) {
         for (Set<String> set : tagsCache) {
            if (tags == null ? set == null : tags.equals(set)) {
               return set;
            }
         }
      }
      tagsCache.add(tags);
      return tags;
   }
   
   public boolean isSolid() {
      if (hasTag("coin_box")) return tileType % 2 == 0;
      return hasTag("solid");
   }
      
   public boolean matches(Tile other) {
      return other != null && other.image.equals(image);
   }
   
   public int getOrientation() {
      return tileType / 15;
   }
   
   public void setTileType(int tileType) {
      if (Images.getImage(image + "_rotation_0.png") == null) {
         ignoreTiling = true;
         this.tileType = tileType;
         updateDrawSection();
         return;
      } else if (getImage() == null) {
         this.tileType = tileType % 15;
      } else {
         this.tileType = tileType;
      }
//       if (tileType == 0 || tileType == 5) {
//          long seed = (long) (Math.random() * 16384);
//          Random rand = new Random();
//          int randInt = rand.nextInt(4);
//          tileType += randInt * 15;
// //          System.out.println(x + ", " + y + ", " + seed + ", " + randInt + ", " + tileType);
//       }
      ignoreTiling = false;
      updateDrawSection();
   }
      
   public void updateDrawSection() {
      int localType = tileType % 15;
      int collumn = localType % 4;
      int row = localType / 4;
      Rectangle rect = new Rectangle(collumn * 8, row * 8, 8, 8);
      cacheDrawRect(rect);
   }
   
   public void cacheDrawRect(Rectangle rectangle) {
      cachedDrawRect = rectangle;
   }
   
   public Rectangle getDrawSection() {
      return cachedDrawRect;
   }
   
   public int getPropertyCount() {
      if (hasTag("property_set")) {
         for (String tag : tags) {
            if (!tag.contains("_set") || tag.equals("property_set")) continue;
            String count = tag.split("_")[0];
            return Integer.parseInt(count);
         }
      }
      return 0;
   }
   
   public int getPropertyIndex() {
      if (hasTag("property_set")) {
         return tileType;
      } 
      return 0;
   }
   
   public void setPropertyValue(int value) {
      if (hasTag("property_set")) {
        setTileType(value); 
      } 
   }
   
   public int[] getPropertyValues() {
      if (hasTag("property_set")) {
         for (String tag : tags) {
            if (!tag.contains("_set") || tag.equals("property_set")) continue;
            String count = tag.split("_")[0];
            int[] values = new int[Integer.parseInt(count)];
            for (int i = 0; i < values.length; i++) {
               values[i] = i;
            }
            return values;
         }
      }
//       if (hasTag("3_set")) {
//          return new int[]{ 0, 1, 2 };
//       } else if (hasTag("4_set")) {
//          return new int[]{ 0, 1, 2, 3 };
//       } else if (hasTag("15_set")) {
//          return new int[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };
//       }
      return new int[]{ 0 };
   }
      
   public void cycleProperties(boolean forward) {
      if (hasTag("property_set")) {
         int max = getPropertyValues().length;
         tileType = tileType + (forward ? 1 : -1);
         if (tileType > max) tileType = 0;
         else if (tileType < 0) tileType = max - 1;
         updateDrawSection();
      }
   }
   
   public void notify(GameState game, String broadcastTag, int[] data) {
      switch (broadcastTag) {
         case "notify_all_coins" : {
            if (tileType / 2 == data[0] && hasTag("coin_box")) {
               cycleProperties(tileType % 2 == 0);
            }
            break;
         }
         case "notify_collect_checkpoint" : {
            if (hasTag("notified_reset_checkpoint") && hasTag("used")) {
               removeTag("used");
               image = "tiles/strong_checkpoint";
            }
            break;
         }
         case "notify_update" : {
            if (hasTag("notified_spawn_enemy")) {
               game.createEnemy(this);
            }
            break;
         }
      }
   }
   
//    public void replaceTag(String oldTag, String newTag) {
//       tags.remove(oldTag);
//       tags.add(newTag);
//    }
   
   public BufferedImage getImage() {
      if (ignoreTiling) return Images.getImage(image + ".png");
      return Images.getImage(image + "_rotation_" + getOrientation() + ".png");
   }
   
   public void render(boolean playerExists, Camera camera, Graphics2D g) {
      if (hasTag("invisible") && playerExists) {
         return;   
      }
      camera.drawImage(g, new Rectangle(x * 64, y * 64, 64, 64), getDrawSection(), getImage());
   }
   
   public AABB toAABB() {
      AABB tileHitbox = new AABB(x * 64, y * 64, 64, 64);
      if (hasTag("half")) {
         switch (tileType) {
            case 0 :
               tileHitbox.height = 32;
               tileHitbox.y += 32;
               break;
            case 1 :
               tileHitbox.width = 32;
               break;
            case 2 :
               tileHitbox.height = 32;
               break;
            case 3 :
               tileHitbox.width = 32;
               tileHitbox.x += 32;
               break;
            default :
               break;
         }
      } else if (hasTag("quarter")) {
         switch (tileType) {
            case 0 :
               tileHitbox.height = 16;
               tileHitbox.y += 48;
               break;
            case 1 :
               tileHitbox.width = 16;
               break;
            case 2 :
               tileHitbox.height = 16;
               break;
            case 3 :
               tileHitbox.width = 16;
               tileHitbox.x += 48;
               break;
            default :
               break;
         }
      } else if (hasTag("small")) {
         tileHitbox.transformDimensions(48, 48);
      }

      return tileHitbox;
   }
   
   @Override
   public String toString() {
      return image + ", " + tileType + ". " + x + ", " + y;
   }
   
   public boolean hasTag(String tag) {
      return tags.contains(tag);
   }
   
   public boolean hasTags() {
      return tags.isEmpty();
   }
   
   public String[] getTags() {
      String[] tagsArray = new String[tags.size()];
      int i = 0;
      for (String string : tags) {
         tagsArray[i] = string;
         i++;
      }
      return tagsArray;
   }
   
   public void addTag(String tag) {
      String[] tags = getTags();
      List<String> newTags = new ArrayList<>();
      for (String string : tags) {
         newTags.add(string);
      }
      newTags.add(tag);
      tags = new String[newTags.size()];
      for (int i = 0; i < tags.length; i++) {
         tags[i] = newTags.get(i);
      }
      setTags(tags);
   }
   
   public void removeTag(String tag) {
      String[] tags = getTags();
      List<String> newTags = new ArrayList<>();
      for (int i = 0; i < tags.length; i++) {
         if (!tags[i].equals(tag)) {
            newTags.add(tags[i]);
         }
      }
      tags = new String[newTags.size()];
      for (int i = 0; i < tags.length; i++) {
         tags[i] = newTags.get(i);
      }
      setTags(tags);
   }
   
   public Tile copy() {
      return new Tile(x, y, image, tileType, tags, extra);
   }
}