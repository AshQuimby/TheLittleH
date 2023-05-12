package src.map.enemy;

import java.util.HashSet;
import java.awt.Graphics2D;

import src.map.Player;
import src.map.Tile;
import src.map.Entity;
import src.map.GameState;
import src.util.AABB;
import src.util.Animation;

public class Enemy extends Entity {
   protected Animation runAnimation;
   protected Animation deathAnimation;
   protected Tile parent;
   protected boolean deathWarn;
   protected boolean dead;
   public boolean despawn;
   public boolean remove;

   public Enemy(int x, int y, Player player, Tile parent) {
      this.x = x * 64 + 8;
      this.y = y * 64 + 8;
      this.direction = (int) Math.signum(player.x - this.x);
      if (direction == 0) direction = -1;
      this.width = 48;
      this.height = 48;
      image = "enemies/e.png";
      frame = 0;
      lastTouchedOneWays = new HashSet<>();
      this.parent = parent;
      runAnimation = new Animation(8, 1, 2, 3, 4);
      deathAnimation = new Animation(4, 7, 8, 9, 10, 11, 12);
      despawn = false;
      dead = false;
      remove = false;
      deathWarn = false;
   }
   
   public final Tile getParent() {
      return parent;
   }
   
   @Override
   public void kill() {
      dead = true;
   }
   
   @Override
   public void touchingTile(Tile tile) {
      if (tile.hasTag("death")) dead = true;
      if (tile.hasTag("bounce")) velocityY = -32;
   }
   
   @Override
   public void onCollision(boolean horizontal, boolean vertical) {
      if (horizontal) {
         velocityX = 0;
      }
      if (vertical) {
         velocityY = 0;
      }
   }
   
   @Override
   public void render(Graphics2D g, GameState game) {
      super.render(g, game);
   }
   
   @Override
   public boolean equals(Object o) {
      return o != null && ((Enemy) o).parent == parent;
   }
}