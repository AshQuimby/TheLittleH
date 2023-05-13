package src.map.enemy;

import java.util.HashSet;

import src.map.Player;
import src.map.Tile;
import src.map.GameState;
import src.util.AABB;
import src.util.Animation;

public class E extends Enemy {
   protected Animation runAnimation;
   protected Animation deathAnimation;
   protected Tile parent;
   protected boolean deathWarn;
   protected boolean dead;
   public boolean despawn;
   public boolean remove;

   public E(int x, int y, Player player, Tile parent) {
      super(x, y, player, parent);
      this.x = x * 64 + 8;
      this.y = y * 64 + 8;
      this.direction = (int) Math.signum(player.x - this.x);
      if (direction == 0) direction = -1;
      this.width = 48;
      this.height = 48;
      image = "enemies/e.png";
      frame = 0;
      lastTouchedTiles = new HashSet<>();
      this.parent = parent;
      runAnimation = new Animation(8, 1, 2, 3, 4);
      deathAnimation = new Animation(4, 7, 8, 9, 10, 11, 12);
      despawn = false;
      dead = false;
      remove = false;
      deathWarn = false;
   }
   
   @Override
   public void update(GameState game) {
      if (dead) {
         frame = deathAnimation.step();
         if (deathAnimation.getFinished()) remove = true;
         return;
      }
      frame = runAnimation.stepLooping();
      super.update(game);
      if (!touchingGround) {
         if (velocityY > 0) {
            frame = 5;
         } else {
            frame = 6;
         }
      }
      float playerDist = (float) Math.sqrt((game.player.x - x) * (game.player.x - x) + (game.player.y - y) * (game.player.y - y));
      if (playerDist > 1480) {
         remove = true;
      }
      Tile tileAhead = getTile(direction, 0, game.map);
      if (tileAhead != null && (tileAhead.hasTag("death") || tileAhead.isSolid())) {
         direction *= -1;
      }
      tileAhead = getTile(direction, 1, game.map);
      if (tileAhead == null || !tileAhead.isSolid()) {
         direction *= -1;
      }
      velocityY += 1f;
      velocityX += 0.7f * direction;
      velocityX *= 0.9f;
      velocityY *= 0.98f;
      if (new AABB(x, y, width, height).overlaps(new AABB(game.player.x, game.player.y, game.player.width, game.player.height))) game.player.touchingEnemy(this);
      touchingGround = false;
   }
   
   @Override
   public void onCollision(boolean horizontal, boolean vertical) {
      if (horizontal) {
         velocityX *= -1;
         direction *= -1;
      }
      if (vertical) {
         velocityY = 0;
      }
   }
}