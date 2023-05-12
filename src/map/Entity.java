package src.map;

import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import src.Images;
import src.Particle;
import src.TileEditor;
import src.util.AABB;
import src.util.Animation;
import src.util.TextUtils;

public class Entity {
   public float x;
   public float y;
   public int width;
   public int height;
   public int direction;
   protected float rotation;
   protected float velocityX;
   protected float velocityY;
   protected int frame;
   protected boolean slippery;
   protected boolean touchingGround;
   protected String image;
   
   protected Set<Tile> lastTouchedOneWays;
   
   public void update(GameState game) {
      collide(game);
      lastTouchedOneWays = new HashSet<>();
   }
   
   // Return false to prevent the player from having their velocity set to 0
   public boolean onCollide(GameState game, AABB entityHitbox, AABB tileHitbox, Tile tile, boolean yCollision) {
      return true;
   }
   
   public Tile getTile(float relX, float relY, Tile[][] map) {
      int absX = (int) Math.round((x) / 64 + relX);
      int absY = (int) Math.round((y) / 64 + relY);
      if (absX >= map.length || absX <= 0 || absY >= map[0].length || absY <= 0) return null;
      return map[absX][absY];
   }
   
   public List<Tile> getNearbyTiles(Tile[][] map) {
      int minX = (int) Math.floor((x) / 64) - 1;
      int maxX = minX + 3;
      int minY = (int) Math.floor((y) / 64) - 1;
      int maxY = minY + 3;
      List<Tile> tiles = new ArrayList<>();
      for (int i = minX; i < maxX; i++) {
         for (int j = minY; j < maxY; j++) {
            if (i >= 0 && j >= 0 && i < map.length && j < map[0].length && map[i][j] != null) {
               tiles.add(map[i][j]);
            } 
         }
      }
      return tiles;
   }
   
   public void set(AABB setTo) {
      x = setTo.x;
      y = setTo.y;
      width = (int) setTo.width;
      height = (int) setTo.height;
   }
   
   public void collide(GameState game) {
      List<Tile> collisions = new ArrayList<Tile>();
      AABB entityHitbox = new AABB(x, y, width, height);
      
      solidInteractions(entityHitbox, collisions, game);
      collisions = getNearbyTiles(game.map);
      tileInteractions(entityHitbox, collisions, game);

      set(entityHitbox);
   }
   
   public void onCollision(boolean horizontal, boolean vertical) {
      if (horizontal) {
         velocityX = 0;
      }
      if (vertical) {
         velocityY = 0;
      }
   }
   
   public void solidInteractions(AABB entityHitbox, List<Tile> collisions, GameState game) {
      entityHitbox.x += velocityX;
      x = entityHitbox.x;

      boolean stopX = false;
      boolean stopY = false;
      
      collisions = getNearbyTiles(game.map);
      for (Tile tile : collisions) {
         if (!tile.isSolid()) continue;
         AABB tileHitbox = tile.toAABB();
         if (tile.hasTag("one_way") && (Math.signum(tile.tileType - 2) == Math.signum(velocityX) || velocityX == 0 || tile.tileType % 2 == 0) || lastTouchedOneWays.contains(tile)) continue;
         if (entityHitbox.resolveX(velocityX, tileHitbox)) {
            if (onCollide(game, entityHitbox, tileHitbox, tile, false)) stopX = true;
         }
      }
      
      entityHitbox.y += velocityY;
      y = entityHitbox.y;

      collisions = getNearbyTiles(game.map);
      for (Tile tile : collisions) {
         if (!tile.isSolid()) continue;
         AABB tileHitbox = tile.toAABB();     
         if (tile.hasTag("one_way") && (Math.signum(tile.tileType - 1) != Math.signum(velocityY) || tile.tileType % 2 != 0) || lastTouchedOneWays.contains(tile)) continue;
         if (entityHitbox.resolveY(velocityY, tileHitbox)) {
            if (velocityY > 0) touchingGround = true;
            if (onCollide(game, entityHitbox, tileHitbox, tile, true)) stopY = true;
         }
      }
      
      if (stopX || stopY) onCollision(stopX, stopY);
   }
   
   public void kill() {
   }
   
   public void touchingTile(Tile tile) {
      
   }
   
   public void render(Graphics2D g, GameState game) {
      Point point = game.camera.getScreenLocation(new Point((int) x + 24, (int) y + 24));
      game.camera.drawImage(g, new Rectangle((int) x - 8, (int) y - 16, 64, 64), new Rectangle((direction == 1 ? 0 : 8), 8 * frame, (direction == 1 ? 8 : -8), 8), Images.getImage(image));
   }
   
   public float velocityMagnitude() {
      return (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
   }
   
   public void tileInteractions(AABB playerHitbox, List<Tile> collisions, GameState game) {
      lastTouchedOneWays.clear();
      for (Tile tile : collisions) {
         AABB tileHitbox = tile.toAABB();
         if (playerHitbox.overlaps(tileHitbox)) {
            touchingTile(tile);
            if (tile.hasTag("one_way")) {
               lastTouchedOneWays.add(tile);
            }
         }
      }
   }
}