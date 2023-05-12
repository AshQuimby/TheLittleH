package src.map.enemy;

import java.util.HashSet;
import java.awt.Graphics2D;

import src.map.Player;
import src.map.Tile;
import src.map.Entity;
import src.map.GameState;
import src.util.AABB;
import src.util.Animation;

public class F extends Enemy {
   protected Tile parent;

   public F(int x, int y, Player player, Tile parent) {
      super(x, y, player, parent);
      this.x = x * 64 + 8;
      this.y = y * 64 + 8;
      this.direction = (int) Math.signum(player.x - this.x);
      if (direction == 0) direction = -1;
      this.width = 48;
      this.height = 48;
      image = "enemies/f.png";
      frame = 0;
      lastTouchedOneWays = new HashSet<>();
      this.parent = parent;
      runAnimation = new Animation(8, 0, 1, 2, 3);
      deathAnimation = new Animation(4, 4, 5, 6, 7, 8, 9);
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
      float playerDist = (float) Math.sqrt((game.player.x - x) * (game.player.x - x) + (game.player.y - y) * (game.player.y - y));
      if (playerDist > 1480) {
         remove = true;
      }
      Tile tileAhead = getTile(direction, 0, game.map);
      if (tileAhead != null && (tileAhead.hasTag("death") || tileAhead.isSolid())) {
         direction *= -1;
      }
      velocityY = (float) Math.sin(game.gameTick / 8f);
      velocityX += 0.7f * direction;
      velocityX *= 0.9f;
      velocityY *= 0.98f;
      if (new AABB(x, y, width, height).overlaps(new AABB(game.player.x, game.player.y, game.player.width, game.player.height))) game.player.touchingEnemy(this);
      touchingGround = false;
   }
}