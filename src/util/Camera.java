package src.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

import src.TileEditor;

public class Camera {
   private Point position;
   public Point targetPosition;
   private int viewportWidth;
   private int viewportHeight;
   private float zoom;
   public float activeZoom;

   public Camera() {
      position = new Point(0, 0);
      targetPosition = new Point(0, 0);
      zoom = 1;
      activeZoom = zoom;
      update();
   }
   
   public Point getPosition() {
      return new Point(position.x, position.y);
   }
    
   public void update() {
      zoom = Math.max(1 / 32f, zoom);
      zoom = Math.min(4, zoom);
      position.x += (targetPosition.x - position.x) / 8;
      position.y += (targetPosition.y - position.y) / 8;
      Point oldCenter = getCenter();
      activeZoom += (zoom - activeZoom) / 5;
      viewportWidth = (int) (TileEditor.program.getWidth() / activeZoom);
      viewportHeight = (int) (TileEditor.program.getHeight() / activeZoom);
      setCenter(oldCenter.x, oldCenter.y);
   }

   public float getZoom() {
      return activeZoom;
   }
   
   public void setZoom(float zoom) {
      this.zoom = zoom;
      update();
   }
   
   public void addZoom(float zoom) {
      this.zoom += zoom;
      update();
   }
   
   public void setPosition(Point position) {
      this.position = position;
   }
   
   public int getBottomX() {
      return position.x + viewportWidth;
   }
   
   public int getBottomY() {
      return position.y + viewportHeight;
   }
   
   public void setCenter(float x, float y) {
      targetPosition.x = (int) x - viewportWidth / 2;
      targetPosition.y = (int) y - viewportHeight / 2;
      position.x = (int) x - viewportWidth / 2;
      position.y = (int) y - viewportHeight / 2;
   }
   
   public void setTargetCenter(float x, float y) {
      targetPosition.x = (int) x - viewportWidth / 2;
      targetPosition.y = (int) y - viewportHeight / 2;
   }
   
   public Point getCenter() {
      return new Point(position.x + viewportWidth / 2, position.y + viewportHeight / 2);
   }
   
   public Point getScreenLocation(Point point) {
      return new Point(applyZoom(point.x - position.x), applyZoom(point.y - position.y));
   }
 
   public void drawImage(Graphics2D g, Rectangle drawTo, Rectangle drawFrom, BufferedImage image) {
      drawTo.x = (applyZoom(drawTo.x - position.x));
      drawTo.y = (applyZoom(drawTo.y - position.y));
      drawTo.width = applyZoom(drawTo.width, true);
      drawTo.height = applyZoom(drawTo.height, true);
      g.drawImage(image, drawTo.x, drawTo.y, drawTo.x + drawTo.width, drawTo.y + drawTo.height, drawFrom.x, drawFrom.y, drawFrom.x + drawFrom.width, drawFrom.y + drawFrom.height, TileEditor.window);
   }
      
   public void drawText(Graphics2D g, int x, int y, float size, String text, Color color, int anchor) {
      x = applyZoom(x - position.x);
      y = applyZoom(y - position.y);
      TextUtils.drawText(g, x, y, size, text, color, anchor);
   }
   
   public void drawRect(Graphics2D g, int x, int y, int width, int height, Color color) {
      g.setColor(color);
      x = applyZoom(x - position.x);
      y = applyZoom(y - position.y);
      width = applyZoom(width, true);
      height = applyZoom(height, true);
      g.drawRect(x, y, width, height);
   }
   
   public void fillRect(Graphics2D g, int x, int y, int width, int height, Color color) {
      g.setColor(color);
      x = applyZoom(x - position.x);
      y = applyZoom(y - position.y);
      width = applyZoom(width, true);
      height = applyZoom(height, true);
      g.fillRect(x, y, width, height);
   }
   
   public void fillPolygon(Graphics2D g, Polygon polygon, Color color) {
      g.setColor(color);
      for (int i = 0; i < polygon.npoints; i++) {
         polygon.xpoints[i] = applyZoom(polygon.xpoints[i] - position.x);
         polygon.ypoints[i] = applyZoom(polygon.ypoints[i] - position.y);
      }
      g.fill(polygon);
   }
   
   public int applyZoom(int value) {
      return Math.round(value * activeZoom);
   }
   
   public int applyZoom(int value, boolean ceiling) {
      if (ceiling) return (int) Math.ceil(value * activeZoom);
      return Math.round(value * activeZoom);
   }
   
//    public Point applyCamera(int x, int y) {
//       return new Point((int) ((value - position.x) * zoom), (int) ((value - position.y) * zoom));
//    }
   
   public void translate(int x, int y) {
      position.x += x;
      targetPosition.x += x;
      position.y += y;
      targetPosition.y += y;
   }
   
   public Point transform(Point point) {
      point = new Point(point.x, point.y);
      point.x -= position.x;
      point.y -= position.y;
      point.x = applyZoom(point.x);
      point.y = applyZoom(point.y);
      return point;
   }
      
   public Rectangle getViewport() {
      return new Rectangle((int) (position.x), (int) (position.y), viewportWidth, viewportHeight);
   }
      
   public Rectangle getTiledViewPort() {
      int screenWidth = viewportWidth;
      int screenHeight = viewportHeight;
      screenWidth = (int) (Math.ceil(screenWidth / 64) + 1);
      screenHeight = (int) (Math.ceil(screenHeight / 64) + 1);
      int tiledX = (int) (Math.floor(position.x / 64)) - (screenWidth / 64);
      int tiledY = (int) (Math.floor(position.y / 64)) - (screenHeight / 64);
      return new Rectangle(tiledX, tiledY, screenWidth, screenHeight);
   }
}