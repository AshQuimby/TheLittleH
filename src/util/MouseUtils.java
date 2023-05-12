package src.util;

import java.awt.Point;
import java.awt.MouseInfo;

import src.TileEditor;

public final class MouseUtils {
   
   private static Point mouseFramePosition = new Point(0, 0);
   
   public static void update() {
      mouseFramePosition = MouseInfo.getPointerInfo().getLocation();
   }
   
   public static Point getRawPointerLocation() {
      return new Point(mouseFramePosition.x, mouseFramePosition.y);
   }
   
   public static Point pointerScreenLocation() {
      Point windowLocation = TileEditor.window.getLocation();
      Point mouseLocation = getRawPointerLocation();
      return new Point(mouseLocation.x - windowLocation.x - 9, mouseLocation.y - windowLocation.y - 34);
   }
   
   public static Point pointerCameraLocation() {
      if (TileEditor.game == null) return pointerScreenLocation();
      float zoom = TileEditor.game.camera.getZoom();
      Point cameraPosition = TileEditor.game.camera.getPosition();
      Point windowLocation = TileEditor.window.getLocationOnScreen();
      windowLocation.x = (int) (windowLocation.x / zoom);
      windowLocation.y = (int) (windowLocation.y / zoom);
      int offsetX = (int) (8 / zoom);
      int offsetY = (int) (36 / zoom);
      windowLocation.x -= (int) (cameraPosition.x) - offsetX;
      windowLocation.y -= (int) (cameraPosition.y) - offsetY;
      Point mouseLocation = getRawPointerLocation();
      mouseLocation.x = (int) (mouseLocation.x / zoom);
      mouseLocation.y = (int) (mouseLocation.y / zoom);
      return new Point(mouseLocation.x - windowLocation.x, mouseLocation.y - windowLocation.y);
   }
   
   public static Point getGridSnappedLocation() {
      // float zoom = TileEditor.game.camera.getZoom();
      float gridSize = 64;
      Point location = pointerCameraLocation();
      location.x = (int) (Math.floor(location.x / gridSize) * gridSize);
      location.y = (int) (Math.floor(location.y / gridSize) * gridSize);
      return location;
   }
}