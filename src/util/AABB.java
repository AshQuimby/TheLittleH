package src.util;

public class AABB {
   public float x;
   public float y;
   public float width;
   public float height;

   public AABB(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public float getX2() {
      return x + width;
   }

   public float getY2() {
      return y + height;
   }
   
   public void translate(float x, float y) {
      this.x += x;
      this.y += y;
   }

   public void setX2(float x2) {
      this.x = x2 - this.width;
   }

   public void setY2(float y2) {
      this.y = y2 - this.height;
   }
   
   public float getCenterX() {
      return x + width / 2;
   }
   
   public float getCenterY() {
      return y + height / 2;
   }

   private boolean rangeOverlaps(float a, float b, float c, float d) {
      return a < d && b > c;
   }

   public boolean overlaps(AABB other) {
      return rangeOverlaps(x, x + width, other.x, other.x + other.width) &&
            rangeOverlaps(y, y + height, other.y, other.y + other.height);
   }

   public boolean resolveX(float dx, AABB other) {
      if (overlaps(other)) {
         if (dx > 0) {
            setX2(other.x);
            return true;
         } else {
            x = other.getX2();
            return true;
         }
      }
      return false;
   }

   public void transformDimensions(int width, int height) {
      double oldWidth = this.width;
      double oldHeight = this.height;
      
      this.width = width;
      this.height = height;
      
      x += oldWidth / 2 - this.width / 2;
      y += oldHeight / 2 - this.height / 2;
   }


   public boolean resolveY(float dy, AABB other) {
      if (overlaps(other)) {
         if (dy > 0) {
            setY2(other.y);
            return true;
         } else {
            y = other.getY2();
            return true;
         }
      }
      return false;
   }
}