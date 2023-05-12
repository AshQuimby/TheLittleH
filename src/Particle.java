package src;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import src.util.Camera;
import src.map.Entity;

public class Particle extends Entity {
   public float x, y, velocityX, velocityY, drag, life, gravity;
   public int width, height, imageWidth, imageHeight, direction, frame, frameSpeed;
   public String image;
   public boolean alive;

   public Particle(float x, float y, float velocityX, float velocityY, int width, int height, int imageWidth, int imageHeight, int direction, float drag, float gravity, int frame, int frameSpeed, String image, int life) {
      this.x = x;
      this.y = y;
      this.velocityX = velocityX;
      this.velocityY = velocityY;
      this.width = width;
      this.height = height;
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
      this.direction = direction;
      this.drag = drag;
      this.gravity = gravity;
      this.frame = frame;
      this.image = image;
      this.life = life;
      this.frameSpeed = frameSpeed;
      alive = true;
   }
   
   public void update() {
      x += velocityX;
      y += velocityY;
      velocityX *= drag;
      velocityY *= drag;
      velocityY += gravity;
      if (frameSpeed != 0 && (life % (frameSpeed + 1)) == 0) {
         frame++;
      }
      if (--life < 0) alive = false;
   }
   
   public void render(Graphics2D g, Camera camera) {
      camera.drawImage(g, new Rectangle((int) x, (int) y, width, height), new Rectangle((direction == -1 ? imageWidth : 0), imageHeight * frame, direction * imageWidth, imageHeight), Images.getImage(image));
   }
}