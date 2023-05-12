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
import src.util.SoundEngine;
import src.util.TextUtils;

public class WingedH extends Player {
   
   private static Animation flapAnimation = new Animation(14, 6, 7);
   private static Animation glideAnimation = new Animation(1, 7);
   private float flyingRotation;
   private boolean gliding;
   
   public WingedH(Point startPos, GameState game) {
      super(startPos, game);
      image = "player/wing_h";
   }
   
   public WingedH(Player player) {
      super(player);
      if (player instanceof WingedH) this.flyingRotation = ((WingedH) player).flyingRotation;
      image = "player/wing_h";
   }
      
   @Override
   public void updateVelocity() {
      if (!gliding) {
         if (!(slippery && crouched)) velocityX *= 0.9f;
         else velocityX *= 0.95f;
         velocityY *= 0.99f;
         velocityY += 0.9f;
      } else {
         velocityX *= 0.9999f;
         velocityY *= 0.9999f;
         velocityY += 1.5f;
         float magnitude = velocityMagnitude();
         velocityX = (velocityX * 2 + magnitude * (float) Math.cos(flyingRotation)) / 3;
         velocityY = (velocityY * 2 + magnitude * (float) Math.sin(flyingRotation)) / 3;
      }
   }
   
   @Override
   public void jump(GameState game) {
      if (crushed) return;
      if (leftGroundFor < 8) {
         SoundEngine.playSound("effects/wing_jump.wav");
         velocityY = -12;
         leftGroundFor = 8;
         jumpStrength++;
         game.addParticle(new Particle(x - 24, y + 32, 0f, 0f, 96, 16, 12, 2, 1, 0f, 0f, 0, 2, "particles/jump.png", 9));
      } else if (jumpReleased && leftWallFor < 8) {
         SoundEngine.playSound("effects/double_jump.wav");
         velocityY = -16;
         leftWallFor = 8;
         x += -2 * wallDirection;
         velocityX = -24 * wallDirection;
      } else if (jumpReleased && doubleJump && velocityY > 4) {
         SoundEngine.playSound("effects/double_jump.wav");
         game.addParticle(new Particle(x, y + 16, 0f, 0f, 48, 32, 6, 4, 1, 0f, 0f, 0, 2, "particles/double_jump.png", 9));
         velocityY = -18;
         doubleJump = false;
      }
   }
   
   @Override
   public void update(GameState game) {
      if (game.mapSettings[GameState.ALLOW_AIR_JUMP]) doubleJump = true;
      super.update(game);
      if (!dead && !win) {
         gliding = keysPressed[0] > 30;
         if (gliding) {
            if (keys[1]) flyingRotation -= Math.PI / 24;
            if (keys[3]) flyingRotation += Math.PI / 24;
            if (touchingGround) {
               gliding = false;
               keys[0] = false;
               keysPressed[0] = 0;
            }
            if (velocityMagnitude() < 20 && velocityY < 8) {
               frame = flapAnimation.stepLooping();
            } else {
               frame = glideAnimation.stepLooping();
            }
         } else {
            flyingRotation = (float) -Math.PI / 2;
         }
      } else {
         flyingRotation = 0;
      }
   }
   
   public void render(Graphics2D g, GameState game) {
      for (int i = keyCount; i > 0; i--) {
         if (previousPositions.size() > 0) {
            Point position = previousPositions.get(Math.max(0, Math.min(previousPositions.size() - 1, 5 * (2 + i))));
            game.camera.drawImage(g, new Rectangle(position.x - 8, position.y - 16, 64, 64), new Rectangle(0, 0, 8, 8), Images.getImage("tiles/key.png"));
         }
      }
      if (evilKey != null) {
         evilKey.render(g, this, game);
      }
      renderTrail(g, game);
      Point point = game.camera.getScreenLocation(new Point((int) x + 24, (int) y + 24));
      rotation = velocityX * (touchingGround ? 1f / 96f : 1f / 128f) * (velocityY / 16f + 1f);
      rotation -= coolRoll * direction;
      if (crouched || win) rotation = 0;
      if (gliding) rotation = flyingRotation + (float) Math.PI / 2;
      g.rotate(rotation, point.x, point.y);
      drawPlayer(g, game);
      g.setTransform(new AffineTransform());
      for (int i = 0; i < totalCoinCounts.length; i++) {
         int total = totalCoinCounts[i];
         if (total > 0) {
            TextUtils.drawText(g, TileEditor.program.getWidth() - 48, 48 + 48 * i + 12, 24, coinCounts[i] + "/" + totalCoinCounts[i], TileEditor.getSecondaryColor(), 1);
            Images.drawImage(g, Images.getImage("ui/coins.png"), new Rectangle(TileEditor.program.getWidth() - 40, 48 + 48 * i, 32, 40), new Rectangle(0, 5 * i, 4, 5));
         }
      }
   }
}