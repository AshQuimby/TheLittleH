package src.map.enemy;

import src.map.Player;
import src.map.Tile;

import src.map.Player;
import src.map.Tile;
import src.map.Entity;
import src.map.GameState;
import src.util.AABB;
import src.util.Animation;

public class A extends Enemy {
   public A(int x, int y, Player player, Tile parent) {
      super(x, y, player, parent);
      image = "enemies/a.png";
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
      Tile tileAhead = getTile(direction * 1, 0, game.map);
      boolean jump = touchingGround && tileAhead != null && (tileAhead.isSolid() || tileAhead.hasTag("death"));
      
      float testX = x;
      float testY = y;
      float testVX = velocityX;
      float testVY = velocityY;
      
      if (touchingGround) for (int i = 0; i < 90; i++) {
         testX += testVX;
         testY += testVY;
         testVY += 1f;
         testVX += 0.6f * direction;
         testVX *= 0.95f;
         testVY *= 0.98f;
         Tile tile = game.getTileAt((int) (testX / 64), (int) (testY / 64));
         if (tile != null && tile.hasTag("death")) {
            jump = true;
            break;
         }
      }
      if (jump) {
         velocityY = -28;
      }
      this.direction = (int) Math.signum(game.player.x - this.x);
      if (direction == 0) direction = -1;
      velocityY += 1f;
      velocityX += 0.6f * direction;
      velocityX *= 0.95f;
      velocityY *= 0.98f;
      if (new AABB(x, y, width, height).overlaps(new AABB(game.player.x, game.player.y, game.player.width, game.player.height))) game.player.touchingEnemy(this);
      touchingGround = false;

   }
}