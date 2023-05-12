package src.map;

import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import src.Images;
import src.Particle;
import src.TileEditor;
import src.util.AABB;
import src.util.Animation;
import src.util.SoundEngine;
import src.util.TextUtils;
import src.util.dialogue.Dialogues;
import src.map.enemy.Enemy;

public class Player extends Entity {
   protected static Animation idleAnimation = new Animation(12, 0, 1);
   protected static Animation runAnimation = new Animation(4, 2, 3, 4, 5);
   protected static Animation deathAnimation = new Animation(4, 9, 10, 11, 12, 13, 14);
   protected static Animation wallSlideAnimation = new Animation(1, 8);
   protected static Animation fallAnimation = new Animation(1, 6);
   protected static Animation jumpAnimation = new Animation(1, 7);
   protected static Animation crouchAnimation = new Animation(1, 24);
   protected static Animation slideAnimation = new Animation(4, 25, 26);
   protected static Animation winAnimation = new Animation(8, 15, 16, 15, 16, 15, 16, 15, 16, 15, 16, 15, 16, 17, 18, 19, 20, 21, 22, 23);
   
   public boolean win;
   public boolean end;
   protected boolean startTick;
   protected Point startPos;

   protected boolean jumpReleased;
   protected int leftGroundFor;
   protected int leftWallFor;
   protected int jumpStrength;
   protected boolean touchingWall, doubleJump, dead, trueKill;
   // Style points
   protected float coolRoll;
   protected Animation currentAnimation;
   
   protected boolean crouched;
   protected boolean crushed;
   protected int wallDirection;
   protected int keyCount;
   protected boolean hasEvilKey;
   protected boolean savedEvilKey;
   protected EvilKey evilKey;
   protected int savedKeyCount;
   protected int[] totalCoinCounts;
   protected int[] coinCounts;
   protected Player savedPlayer;
   
   public List<Point> previousPositions;
   public List<Float> previousSpeeds;
   
   public boolean[] keys = {
      false, // JUMP
      false, // LEFT
      false, // DOWN
      false // RIGHT
   };
   public int[] keysPressed = {
      0, // JUMP
      0, // LEFT
      0, // DOWN
      0 // RIGHT
   };
   
   public Player(Point startPos) {
      dead = false;
      currentAnimation = idleAnimation;
      x = startPos.x + 8;
      y = startPos.y + 8;
      this.startPos = startPos;
      direction = 1;
      velocityX = 0;
      velocityY = 0;
      width = 48;
      height = 48;
      slippery = false;
      rotation = 0;
      win = false;
      trueKill = false;
      previousPositions = new ArrayList<>();
      previousSpeeds = new ArrayList<>();
      totalCoinCounts = new int[4];
      coinCounts = new int[4];
      startTick = true;
      savedPlayer = this;
      lastTouchedOneWays = new HashSet<>();
      image = "player/h";
      crushed = false;
   }
   
   public Player(Point startPos, GameState game) {
      this(startPos);
      init(game);
   }
   
   public Player(Player player) {
      this(player.startPos);
      this.velocityX = player.velocityX;
      this.velocityY = player.velocityY;
      this.width = player.width;
      this.height = player.height;
      this.doubleJump = player.doubleJump;
      this.savedPlayer = player.savedPlayer;
      this.totalCoinCounts = player.totalCoinCounts;
      this.coinCounts = player.coinCounts;
      this.startTick = player.startTick;
      this.previousPositions = player.previousPositions;
      this.direction = player.direction;
      this.keys = player.keys;
      this.keysPressed = player.keysPressed;
      this.win = player.win;
      this.currentAnimation = player.currentAnimation;
      this.rotation = player.rotation;
      this.jumpReleased = player.jumpReleased;
      this.end = player.end;
      this.leftWallFor = player.leftWallFor;
      this.leftGroundFor = player.leftGroundFor;
      this.startPos = player.startPos;
      this.x = player.x + player.width / 2 - width / 2;
      this.y = player.y + player.width / 2 - height / 2 - 8;
   }
   
   public Player() {
      this(new Point(0, 0));
   }
   
   public void setStartPos(Point startPos) {
      this.startPos.x = startPos.x;
      this.startPos.y = startPos.y;
   }
   
   public void init(GameState game) {
      leftGroundFor = 0;
      leftWallFor = 0;
      touchingGround = false;
      win = false;
      x = startPos.x + 8;
      y = startPos.y + 8;
      velocityX = 0;
      velocityY = 0;
      jumpReleased = false;
      doubleJump = false;
      dead = false;
      currentAnimation = idleAnimation;
      deathAnimation.reset();
      winAnimation.reset();
      winAnimation.setAnimationSpeed(8);
      slippery = false;
      keyCount = savedKeyCount;
      hasEvilKey = savedEvilKey;
      if (hasEvilKey) {
         evilKey = new EvilKey((int) x / 64, (int) y / 64);
      }
      keys = new boolean[] { false, false, false, false };
      keysPressed = new int[] { 0, 0, 0, 0 };
      previousPositions.clear();
   }
   
   public void setCoinCounts(GameState game) {
      for (int i = 0; i < totalCoinCounts.length; i++) {
         totalCoinCounts[i] = game.getVolatileTileCount("coin", i);
      }
   }
   
   public void updateCoinCounts(GameState game) {
      for (int i = 0; i < coinCounts.length; i++) {
         if (totalCoinCounts[i] > 0) coinCounts[i] = totalCoinCounts[i] - game.getVolatileTileCount("coin", i);
      }
   }
   
   public void jump(GameState game) {
      if (crushed) return;
      if (leftGroundFor < 6) {
         SoundEngine.playSound("effects/jump.wav");
         velocityY = -8;
         leftGroundFor = 8;
         jumpStrength++;
         game.addParticle(new Particle(x - 24, y + 32, 0f, 0f, 96, 16, 12, 2, 1, 0f, 0f, 0, 2, "particles/jump.png", 9));
      } else if (jumpReleased && leftWallFor < 8) {
         SoundEngine.playSound("effects/double_jump.wav");
         velocityY = -26;
         leftWallFor = 8;
         x += -2 * wallDirection;
         velocityX = -16 * wallDirection;
      } else if (jumpReleased && doubleJump) {
         game.addParticle(new Particle(x, y + 16, 0f, 0f, 48, 32, 6, 4, 1, 0f, 0f, 0, 2, "particles/double_jump.png", 9));
         SoundEngine.playSound("effects/double_jump.wav");
         if (velocityY > 16) coolRoll = (float) (Math.PI * 3);
         if (velocityY > -25) velocityY = -25;
         else velocityY -= 14f;
         doubleJump = false;
      }
   }
   
   public void updateVelocity() {
      if (!(slippery && crouched)) velocityX *= 0.92f;
      else velocityX *= 0.98f;
      velocityY *= 0.98f;
      velocityY += 1.2f;
   }
   
   public void touchingEnemy(Enemy enemy) {
      kill();
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

      if (currentAnimation == runAnimation) {
         if (runAnimation.tick == 0 && runAnimation.frame % 2 == 0) SoundEngine.playSound("effects/step.wav");
      }

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
            velocityX -= 1.2f;
         }
   
         if (keys[3]) {
            if (!touchingWall) direction = 1; 
            velocityX += 1.2f;
         }
      }
      if ((y / 64) - 4 > game.map[0].length) {
         kill();
      }
      
      // Crouching
      if (game.mapSettings[GameState.ALLOW_CROUCH]) {
         if (keysPressed[1] == 1 ^ keysPressed[3] == 1 || !keys[2] || !touchingGround) {
            if (crouched) {
               y -= height;
            }
            height = 48;
            crouched = false;
         }
         if (keysPressed[2] == 1 && touchingGround) {
            if (!crouched) {
               y += height / 2;
            }
            touchingWall = false;
            height = 24;
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
   
   public int getCenterX() {
      return (int) (x + width / 2);
   }
   
   public int getCenterY() {
      return (int) (y + height / 2);
   }
   
   // Return false to prevent the player from having their velocity set to 0
   @Override
   public boolean onCollide(GameState game, AABB entityHitbox, AABB tileHitbox, Tile tile, boolean yCollision) {
      if (tile.hasTag("key_box")) {
         if (tile.hasTag("evil")) {
            if (hasEvilKey) {
               hasEvilKey = false;
               for (int i = 0; i < 4; i++) {
                  game.addParticle(new Particle(tileHitbox.getCenterX() - 16, tileHitbox.getCenterY() - 16, (float) ((Math.random() - 0.5) * -20), (float) (Math.random() * -10), 32, 32, 4, 4, 1, 0.98f, 0f, i, 0, "particles/evil_key_box_rubble.png", 30));
               }
               game.inGameRemoveTile(tile);
               return false;
            }
         } else if (keyCount > 0) {
            keyCount--;
            for (int i = 0; i < 4; i++) {
               game.addParticle(new Particle(tileHitbox.getCenterX() - 16, tileHitbox.getCenterY() - 16, (float) ((Math.random() - 0.5) * -8), (float) (Math.random() * -10), 32, 32, 4, 4, 1, 0.98f, 1.2f, i, 0, "particles/key_box_rubble.png", 30));
            }
            game.inGameRemoveTile(tile);
            return false;
         }
      }
      if (tile.hasTag("slippery")) {
         slippery = true;
      }
      if (yCollision) {
         if (velocityY > 0) touchingGround = true;
         if (tile.hasTag("slippery")) {
            slippery = true;
         }
      } else {
         if (game.mapSettings[GameState.ALLOW_WALLSLIDE] && !tile.hasTag("no_wallslide")) {
            touchingWall = true;
         }
      }
      return super.onCollide(game, entityHitbox, tileHitbox, tile, yCollision);
   }
   
   public void collide(GameState game) {
      List<Tile> collisions = new ArrayList<Tile>();
      AABB playerHitbox = new AABB(x, y, width, height);
      
      solidInteractions(playerHitbox, collisions, game);
      collisions = getNearbyTiles(game.map);
      tileInteractions(playerHitbox, collisions, game);

      set(playerHitbox);
   }
   
   public void onCollision(boolean horizontal, boolean vertical) {
      if (horizontal) {
         velocityX = 0;
      }
      if (vertical) {
         if (velocityY > 32) {
            SoundEngine.playSound("effects/hit.wav");
            coolRoll = (float) (Math.PI * 4);
         }
         velocityY = 0;
      }
   }
      
   public void tileInteractions(AABB playerHitbox, List<Tile> collisions, GameState game) {
      lastTouchedOneWays.clear();
      for (Tile tile : collisions) {
         if (crouched && tile.isSolid() && tile.hasTag("half") && (tile.tileType == 0 || tile.tileType == 2)) {
            keysPressed[2] = 0;
            keys[2] = true;
            crushed = true;
         }
         AABB tileHitbox = tile.toAABB();
         if (playerHitbox.overlaps(tileHitbox)) {
            touchingTile(tile);
            if (tile.hasTag("one_way")) {
               lastTouchedOneWays.add(tile);
            }
            if (tile.hasTag("death")) {
               if (playerHitbox.overlaps(tileHitbox)) kill();
            } else if (tile.hasTag("bounce")) {
               if (velocityY > -30) SoundEngine.playSound("effects/bounce.wav");
               if (velocityY > 36) velocityY *= -1.5f;
               else if (velocityY > -36) velocityY = -36;
            } 
            if (tile.hasTag("checkpoint") && !tile.hasTag("used")) {
               SoundEngine.playSound("effects/checkpoint.wav");
               startPos.x = tile.x * 64;
               startPos.y = tile.y * 64;
               game.notify("notify_collect_checkpoint", new int[0]);
               tile.image += "_on";
               tile.addTag("used");
               game.saveCheckpointState();
               savedKeyCount = keyCount;
               savedEvilKey = hasEvilKey;
               savedPlayer = this;
               game.showTimer();
            } else if (!win && tile.hasTag("end")) {
               game.showTimer();
               SoundEngine.playSound("effects/win.wav");
               for (int i = 0; i < 16; i++) {
                  game.addParticle(new Particle(x + width / 4, y + height / 4, (float) ((Math.random() - 0.5) * -16), (float) ((Math.random() - 0.5) * -16), 24, 24, 3, 3, 1, 0.96f, 0f, (int) (Math.random() * 2), 0, "particles/twinkle.png", 120));
               }
               win();
            }
            if (tile.hasTag("pickup")) {
               if (playerHitbox.overlaps(tileHitbox)) {
                  game.inGameRemoveTile(tile);
                  if (tile.hasTag("dialogue")) {
                     String key = tile.extra.substring(0, tile.extra.length() - 1);
                     if (game instanceof CampaignState && Dialogues.hasUnspentDialogue(key)) ((CampaignState) game).setDialogue(Dialogues.getDialogue(key));
                     continue;   
                  }
                  if (tile.hasTag("coin")) {
                     SoundEngine.playSound("effects/coin.wav");
                     coinCounts[tile.tileType]++;
                     if (game.getVolatileTileCount("coin", tile.tileType) == 0) {
                        SoundEngine.playSound("effects/all_coins_collected.wav");
                        game.notify("notify_all_coins", new int[]{ tile.tileType });
                     }
                  }
                  if (tile.hasTag("powerup")) {
                     SoundEngine.playSound("effects/powerup_get.wav");
                     if (tile.tileType == 0) game.player = new Player(this);
                     else if (tile.tileType == 1) game.player = new Ball(this);
                     else if (tile.tileType == 2) game.player = new WingedH(this);
                     else if (tile.tileType == 3) game.player = new LimblessH(this);
                  }
                  if (tile.hasTag("key")) {
                     SoundEngine.playSound("effects/coin.wav");
                     if (tile.hasTag("evil")) {
                        evilKey = new EvilKey(tile.x, tile.y);
                        hasEvilKey = true;
                        continue;
                     }
                     keyCount++;   
                  }
                  if (tile.hasTag("timer")) {
                     SoundEngine.playSound("effects/powerup_get.wav");
                     if (game.timeLimit > -1) {
                        switch (tile.getPropertyIndex()) {
                           case 0 :
                              game.timeLeft += 10;
                              game.startPopup("+10s");
                              break;
                           case 1 :
                              game.timeLeft += 60;
                              game.startPopup("+30s");
                              break;
                           case 2 :
                              game.timeLeft += 100;
                              game.startPopup("+60s");
                              break;
                           case 3 :
                              game.timeLeft += 600;
                              game.startPopup("+120s");
                              break;
                        }
                     }
                     game.timeLeft = Math.min(3600, game.timeLeft);
                  }
               }
            }
         }
      }
   }
   
   public void kill() {
      if (!win && !dead) {
         SoundEngine.playSound("effects/death.wav");
         dead = true;
         currentAnimation = deathAnimation;
      }
   }
   
   public void trueKill() {
      kill();
      trueKill = true;
   }
   
   public void win() {
      win = true;
      SoundEngine.playSound("effects/win.wav");
      velocityX *= 0.8f;
      velocityY *= 0.8f;
      currentAnimation = winAnimation;
   }
   
   public void touchingTile(Tile tile) {
      
   }
   
   public Point getPreviousCenter(int ticksBehind) {
      if (ticksBehind < previousPositions.size()) return new Point(previousPositions.get(ticksBehind).x + width / 2, previousPositions.get(ticksBehind).y + height / 2);
      return new Point (0, 0);
   }
   
   public void renderTrail(Graphics2D g, GameState game) {
      boolean speedy = false;
      float speed = 0;
      for (float f : previousSpeeds) {
         if (f > 24) {
            if (f > speed) speed = f;
            speedy = true;
         }
      }
      if (speedy) {
         int[][] trail = new int[2][9];
         trail[0][0] = (int) x + width / 2;
         trail[1][0] = (int) y + height / 2;
         for (int i = 1; i < 5; i++) {
            trail[0][i] = getPreviousCenter(i * 2).x;
            trail[1][i] = getPreviousCenter(i * 2).y;
         }
         double[] angles = new double[4];
         for (int i = 0; i < 4; i++) {
            angles[i] = Math.atan2(trail[1][i + 1] - trail[1][i], trail[0][i + 1] - trail[0][i]);
         }
         for (int i = 0; i < 4; i++) {
            trail[0][8 - i] = (int) (trail[0][i] + 6 * (5 - i) * Math.cos(angles[i] + Math.PI / 2));
            trail[1][8 - i] = (int) (trail[1][i] + 6 * (5 - i) * Math.sin(angles[i] + Math.PI / 2));
         }
         for (int i = 0; i < 4; i++) {
            trail[0][i] = (int) (trail[0][i] + 6 * (5 - i) * Math.cos(angles[i] + Math.PI / 2 * 3));
            trail[1][i] = (int) (trail[1][i] + 6 * (5 - i) * Math.sin(angles[i] + Math.PI / 2 * 3));
         }
         Polygon drawTrail = new Polygon(trail[0], trail[1], trail[0].length);
         game.camera.fillPolygon(g, drawTrail, new Color(255, 255, 255, Math.min(255, Math.max(0, (int) speed - 24) * 2)));
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
      Point point = game.camera.getScreenLocation(new Point((int) x + width / 2, (int) y + height / 2));
      rotation = velocityX * (touchingGround ? 1f / 96f : 1f / 128f) * (velocityY / 16f + 1f);
      rotation -= coolRoll * direction;
      if (crouched || win) rotation = 0;
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
   
   public void drawPlayer(Graphics2D g, GameState game) {
      game.camera.drawImage(g, new Rectangle((int) x - 8, (int) y - 16 - (crouched ? 24 : 0), 64, 64), new Rectangle((direction == 1 ? 0 : 8), 8 * frame, (direction == 1 ? 8 : -8), 8), Images.getImage(image + ".png"));
      game.camera.drawImage(g, new Rectangle((int) x - 8, (int) y - 16 - (crouched ? 24 : 0), 64, 64), new Rectangle((direction == 1 ? 0 : 8), 8 * frame, (direction == 1 ? 8 : -8), 8), Images.hsvEffect(Images.getImage(image + "_color.png"), TileEditor.hHue, 1, 1, image + "_color" + TileEditor.hHue + ".png"));
   }
   
   protected class EvilKey {
      protected AABB hitbox;
      protected float keyVelX, keyVelY;
      protected int startUp;
      
      public EvilKey(int tileX, int tileY) {
         hitbox = new AABB(tileX * 64 + 16, tileY * 64 + 16, 48, 48);
         startUp = 120;
      }
      
      public void update(Player player, GameState game) {
         if (startUp > 0) {
            if (startUp % 4 == 0) {
               game.addParticle(new Particle(hitbox.getCenterX() - 24, hitbox.getCenterY() - 16, (float) ((Math.random() - 0.5) * -6), (float) ((Math.random() - 0.5) * -6), 32, 32, 4, 4, 1, 0.98f, 0f, (int) (Math.random() * 4), 0, "particles/evil_smoke.png", 60));
            }
            startUp--;
            return;
         }
         if (Math.random() > 0.98) {
            game.addParticle(new Particle(hitbox.getCenterX() - 24, hitbox.getCenterY() - 16, (float) ((Math.random() - 0.5) * -6), (float) ((Math.random() - 0.5) * -6), 32, 32, 4, 4, 1, 0.98f, 0f, (int) (Math.random() * 4), 0, "particles/evil_smoke.png", 30));
         }
         Point target = player.previousPositions.get(30);
         keyVelX = (target.x - hitbox.x) / 3;
         keyVelY = (target.y - hitbox.y) / 3;
         hitbox.x += keyVelX;
         hitbox.y += keyVelY;
         if (hitbox.overlaps(new AABB(player.x ,player.y, player.width, player.height))) {
            player.kill();
         }
      }
      
      public void render(Graphics2D g, Player player, GameState game) {
         Point position = new Point((int) hitbox.x - 16, (int) hitbox.y - 16);
         game.camera.drawImage(g, new Rectangle(position.x, position.y, 64, 64), new Rectangle(0, 0, 8, 8), startUp > 60 ? Images.getImage("tiles/evil_key.png") : Images.getImage("tiles/evil_key_awake.png"));
      }
   }
}