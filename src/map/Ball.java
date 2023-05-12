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

// BALLMODE
// (BALLS!!!!!)

public class Ball extends Player {
   
   private static Animation rollAnimation = new Animation(1, 0);
   private float rotationSpeed;
   private float ballRotation;
   private boolean stopBounce;
   private boolean superSlam;
   
   public Ball(Point startPos, GameState game) {
      super(startPos, game);
      superSlam = false;
      // Makes the H look like a BALL
      image = "player/ball_h";
   }
   
   public Ball(Player player) {
      super(player);
      if (player instanceof Ball) this.superSlam = ((Ball) player).superSlam;

      // Makes the H look like a BALL
      image = "player/ball_h";

      // Re-center player just in case
      this.x = player.x + player.width / 2 - width / 2;
      this.y = player.y + player.width / 2 - height / 2 - 8;
   }
      
   @Override
   public void updateVelocity() {
      if (!(slippery && crouched)) velocityX *= 0.975f;
      else velocityX *= 0.985f;
      velocityY *= 0.985f;
      velocityY += 1f;
   }
   
   @Override
   public void update(GameState game) {
      if (win) {
         if (currentAnimation.getFrame() > 19) {
            ballRotation *= 0.5f;
         } else {
            ballRotation += rotationSpeed;
            rotationSpeed = velocityX / 32f;
         }
      }
      if ((keys[1] ^ keys[3]) || !keys[2]) {
         stopBounce = false;
      }
      if (touchingGround) superSlam = false;
      if (keysPressed[2] == 1 && game.mapSettings[GameState.ALLOW_CROUCH]) {
         if (!touchingGround && !superSlam) {
            superSlam = true;
         } else {
            stopBounce = true;
         }
      }
      // Makes the little H SLAM down with BIG BALL ENERGY
      if (superSlam) {
         velocityY += 2;
      }
      super.update(game);
      if (!dead && !win) currentAnimation = rollAnimation;
      if (win || dead) return;
      if (crouched) {
         ballRotation = 0;
         rotationSpeed = 0;
      } else {
         ballRotation += rotationSpeed;
         rotationSpeed = velocityX / 32f;
      }
   }
   
   // Bounce like BALL in BALLMODE
   @Override
   public void onCollision(boolean horizontal, boolean vertical) {
      if (horizontal) {
         velocityX *= -1f;
         if (Math.abs(velocityX) > 5f) SoundEngine.playSound("effects/ball_bounce.wav");
         // Minimum BOUNCING BALL SPEED
         velocityX = Math.max(15, Math.abs(velocityX)) * Math.signum(velocityX);
      }
      if (vertical) {
         if (stopBounce) {
            velocityY = 0;
         } else {
            velocityY *= -1f;
            // Minimum BOUNCING BALL SPEED
            velocityY = Math.max(15, Math.abs(velocityY)) * Math.signum(velocityY);
            if (Math.abs(velocityY) > 5f) SoundEngine.playSound("effects/ball_bounce.wav");
         }
      }
   }
   
   @Override
   public void touchingTile(Tile tile) {
      // Makes bouncy tiles LAUNCH the little H
      if (tile.hasTag("bounce")) {
         velocityY -= 0.5f;
         velocityY = Math.min(96f, velocityY);
      }
   }

   @Override
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
      g.rotate(ballRotation, point.x, point.y);
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