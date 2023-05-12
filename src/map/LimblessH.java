package src.map;

import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;

import src.Images;
import src.Particle;
import src.TileEditor;
import src.util.AABB;
import src.util.Animation;
import src.util.TextUtils;
import src.map.enemy.Enemy;

public class LimblessH extends Player {
      
   public LimblessH(Point startPos, GameState game) {
      super(startPos, game);
      image = "player/limbless_h";
      height = 32;
      width = 32;
   }
   
   public LimblessH(Player player) {
      super(player);
      image = "player/limbless_h";
      height = 32;
      width = 32;
   }
   
   public void updateVelocity() {
      if (!(slippery && crouched) && touchingGround) velocityX *= 0.92f;
      else velocityX *= 0.98f;
      velocityY *= 0.98f;
      velocityY += 1.2f;
   }

   @Override
   public void update(GameState game) {
      if (startTick) {
         setCoinCounts(game);
         startTick = false;
      }
      // game.addParticle(new Particle(x - 16, y - 16, 0, 0, 64, 64, 8, 8, direction, 0.9f, 0, "tiles/h_tile.png", 60));
      
      if (hasEvilKey) {
         evilKey.update(this, game);
      } else {
         evilKey = null;
      }
      
      previousPositions.add(0, new Point((int) x, (int) y));
      if (previousPositions.size() > 512) {
         previousPositions.remove(previousPositions.size() - 1);
      }
      previousSpeeds.add(0, velocityMagnitude());
      if (previousSpeeds.size() > 8) {
         previousSpeeds.remove(previousSpeeds.size() - 1);
      }
      
      frame = currentAnimation.stepLooping();
      if (end) {
         game.endGame();
         return;
      }
      if (win) {
         dead = false;
         currentAnimation = winAnimation;
         if (currentAnimation.getFrame() >= 21) {
            currentAnimation.setAnimationSpeed(4);
            velocityX = 0;
            velocityY -= 3f;
            y += velocityY;
         } else if (currentAnimation.getFrame() >= 17) {
            currentAnimation.setAnimationSpeed(2);
            velocityX *= 0;
            velocityY *= 0;
         } else {
            updateVelocity();
            collide(game);
         }
         if (currentAnimation.getFinished()) {
            end = true;
         }
         return;
      }
      if (dead) {
         if (currentAnimation.getFinished()) {
            if (trueKill) game.reset();
            init(game);
            game.player = savedPlayer;
            savedPlayer.init(game);
            game.resetToCheckpointState();
            updateCoinCounts(game);
         }
         return;
      }
      if (!jumpReleased && keys[0] && jumpStrength > 0 && jumpStrength < 8 || jumpStrength > 0 && jumpStrength < 5) {
         jumpStrength++;
         velocityY -= 3.5f;
      } else {
         jumpStrength = 0;
      }
      if (touchingGround) {
         leftGroundFor = 0;
         if (game.mapSettings[GameState.ALLOW_AIR_JUMP]) doubleJump = true;
         if (keys[1] ^ keys[3]) {
            currentAnimation = runAnimation;
         } else {
            if (!slippery && !crouched) velocityX *= 0.5f;
            currentAnimation = idleAnimation;
         }
         if (crouched) {
            if (Math.abs(velocityX) > 2f) {
               currentAnimation = slideAnimation;
            } else {
               currentAnimation = crouchAnimation;
            }
         }
      } else {
         leftGroundFor++;
         if (velocityY > 0) {
            currentAnimation = fallAnimation;
         } else {
            currentAnimation = jumpAnimation;
         }
         if (touchingWall) {
            if (velocityY > 0) {
               if (!slippery) velocityY *= 0.85f;
               currentAnimation = wallSlideAnimation;
            }
            if (game.mapSettings[GameState.ALLOW_AIR_JUMP]) doubleJump = true;
            leftWallFor = 0;
            wallDirection = direction;
         } else {
            leftWallFor++;
         }
      }
      
      coolRoll *= 0.85f;
      if (coolRoll > 0.025f) coolRoll -= 0.025f;
      if (Math.abs(velocityX) < 2f) coolRoll = 0;

      if (keys[0]) { 
         jump(game);
         jumpReleased = false;
      } else {
         jumpReleased = true;
      }
      if (!crouched) {
         if (keys[1]) { 
            if (!touchingWall) direction = -1;
            velocityX -= touchingGround ? 1.2f : 0.1f;
         }
   
         if (keys[3]) {
            if (!touchingWall) direction = 1; 
            velocityX += touchingGround ? 1.2f : 0.1f;
         }
      }
      if ((y / 64) - 4 > game.map[0].length) {
         kill();
      }
      
      // Crouching
      if (game.mapSettings[GameState.ALLOW_CROUCH]) {
         if (keysPressed[1] == 1 ^ keysPressed[3] == 1 || !keys[2] || !touchingGround) {
            if (crouched) {
               y -= 16;
            }
            height = 32;
            crouched = false;
         }
         if (keysPressed[2] == 1 && touchingGround) {
            if (!crouched) {
               y += 16;
            }
            touchingWall = false;
            height = 16;
            crouched = true;
         }
      }
      
      crushed = false;
      updateVelocity();
      touchingGround = false;
      touchingWall = false;
      slippery = false;
      collide(game);
      for (int i = 0; i < keys.length; i++) {
         if (keys[i]) keysPressed[i]++;
         else keysPressed[i] = 0;
      }
   }
   
   public void touchingEnemy(Enemy enemy) {
      if (previousPositions.get(1).y < enemy.y && velocityY > 0) {
         velocityY = -32;
         enemy.kill();
      } else {
         kill();
      }
   }
   
   public void drawPlayer(Graphics2D g, GameState game) {
      game.camera.drawImage(g, new Rectangle((int) x - 16, (int) y - 32 - (crouched ? 16 : 0), 64, 64), new Rectangle((direction == 1 ? 0 : 8), 8 * frame, (direction == 1 ? 8 : -8), 8), Images.getImage(image + ".png"));
      game.camera.drawImage(g, new Rectangle((int) x - 16, (int) y - 32 - (crouched ? 16 : 0), 64, 64), new Rectangle((direction == 1 ? 0 : 8), 8 * frame, (direction == 1 ? 8 : -8), 8), Images.hsvEffect(Images.getImage(image + "_color.png"), TileEditor.hHue, 1, 1, image + "_color" + TileEditor.hHue + ".png"));
   }
}