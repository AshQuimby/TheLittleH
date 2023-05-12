package src.map;

import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class MapEditor {
   private Map map;
   public List<UndoAction> previousActions;
   private int undoPosition;
   private boolean previouslyRedid;
   private Set<Point> fillTiles;
   // private int fillCount;
   
   public MapEditor(Map map) {
      this.map = map;
      undoPosition = 0;
      previouslyRedid = false;
      previousActions = new ArrayList<>();
      fillTiles = new HashSet<>();
      // previousActionPositions = new ArrayList<>();
   }
   
   public void drawLine(Tile tile, Point start, Point end) {
      float[] vector = new float[]{ end.x - start.x, end.y - start.y };
      float len = (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
      vector[0] /= len;
      vector[1] /= len;
      Point placeAt = new Point(start.x, start.y);
      Tile newTile = tile.copy();
      float[] point = new float[]{ start.x, start.y };
      if (placeAt.equals(end)) {
         newTile.x = placeAt.x;
         newTile.y = placeAt.y;
         addTile(newTile, placeAt.x, placeAt.y);
         return;
      }
      do {
         // if (!(placeAt.x > 0 && placeAt.y > 0 && placeAt.x < map.map.length && placeAt.y < map.map[0].length)) break;
         start.x += map.lastResizeOffset.x;
         start.y += map.lastResizeOffset.y;
         end.x += map.lastResizeOffset.x;
         end.y += map.lastResizeOffset.y;
         placeAt.x += map.lastResizeOffset.x;
         placeAt.y += map.lastResizeOffset.y;
         point[0] += map.lastResizeOffset.x;
         point[1] += map.lastResizeOffset.y;

         newTile = tile.copy();
         newTile.x = placeAt.x;
         newTile.y = placeAt.y;
         addTile(newTile, placeAt.x, placeAt.y);
         placeAt = new Point((int) Math.round(point[0]), (int) Math.round(point[1]));
         point[0] += vector[0];
         point[1] += vector[1];
      } while(!placeAt.equals(end));
   }
   
   // Simplified addTile
   public void addTile(Tile tile, int tileX, int tileY) {
      addTile(tile, tileX, tileY, false);
   }
   
   // Full, raw addTile
   public void addTile(Tile tile, int tileX, int tileY, boolean undoCaused) {
      if (map.map == null) return;
      // Allows you to update text by clicking it intead of rewriting the text completely
      if (tile != null && tile.hasTag("modify_extra")) {
         if (tileX > 0 && tileY > 0 && tileX < map.map.length && tileY < map.map[0].length) {
            if (map.map[tileX][tileY] != null && map.map[tileX][tileY].hasTag("modify_extra")) {
               map.map[tileX][tileY].tileType = tile.tileType;
               map.modifyExtra(map.map[tileX][tileY]);
               // Let the user know that the map is no longer saved
               map.setUnsaved();
               return;
            }
         }
         tile.extra = "";
         map.modifyExtra(tile);
      }
      
      // Reset the resize offset
      map.lastResizeOffset.x = 0;
      map.lastResizeOffset.y = 0;
      
      // Dont place a tile if they are the same tile
      if (tileX > 0 && tileY > 0 && tileX < map.map.length && tileY < map.map[0].length) {      
         if (Tile.tilesEqual(tile, map.map[tileX][tileY])) return;
      }

      // Let the user know that the map is no longer saved 
      map.setUnsaved();

      // Resize the map if a tile is placed positively outside current bounds
      if (map.map.length <= tileX) {
         map.resize(tileX - map.map.length + 1, 0, false);
      }
      if (map.map[0].length <= tileY) {
         map.resize(0, tileY - map.map[0].length + 1, false);
      }
      
      // Resize the map if a tile is placed negatively outside current bounds
      if (tileX < 0) {
         map.resize(-tileX, 0, true);
         tileX = 0;
      }
      if (tileY < 0) {
         map.resize(0, -tileY, true);
         tileY = 0;
      }
      
      // Remove the deleted tile from the set of all tiles      
      if (map.map[tileX][tileY] != null) map.allTiles.remove(map.map[tileX][tileY]);
      
            // Bug testing
//       List<Tile> duplicateTiles = new ArrayList<>();
//       for (Tile other : map.allTiles) {
//          if (other == tile) duplicateTiles.add(other);
//       }
//       map.allTiles.removeAll(duplicateTiles);
      
      // Don't save to the undo if undo caused
      if (!undoCaused) {
         Tile oldTile = map.map[tileX][tileY];
         if (oldTile == null) {
            // Save undo actions of null instead as tiles with the tag "delete"
            oldTile = new Tile(tileX, tileY, "", 0);
            oldTile.setTags(new String[]{ "delete" });
            // System.out.println(oldTile.hasTag("delete"));
         } else {
            oldTile = map.map[tileX][tileY].copy();
         }
         
         Tile newTile = tile;
         if (newTile == null) {
            // Save redo actions of null instead as tiles with the tag "delete"
            newTile = new Tile(tileX, tileY, "", 0);
            newTile.setTags(new String[]{ "delete" });
         } else {
            newTile = tile.copy();
         }
         
         // Add the action to undo queue
         addUndoAction(new UndoAction(oldTile, newTile));
      }
      
      // Finally set the tile on the map to the correct one
      map.map[tileX][tileY] = tile;
      
      // Add the tile to the set of all non-air tiles
      if (tile != null) {
         tile.x = tileX;
         tile.y = tileY;
         map.allTiles.add(tile);
      }
      
      // Check the tiling of it and all adjacent tiles
      for (int i = Math.max(Math.min(tileX, map.map.length - 1) - 1, 0); i < tileX + 2 && i < map.map.length; i++) {
         for (int j = Math.max(Math.min(tileY, map.map[0].length - 1) - 1, 0); j < tileY + 2 && j < map.map[0].length; j++) {
            if (map.map[i][j] != null) checkTiling(map.map[i][j], i, j);
         }
      }
   }
   
   public void addUndoAction(UndoAction action) {
      // Add the undo action to the list
      previousActions.add(action);
      
      // Trim the undo list if it gets too big
      if (previousActions.size() > 512) {
         previousActions.remove(0);
      }
       
      // Don't allow redoing after a change is made
      if (undoPosition + 1 < previousActions.size() - 1) {
         for (int i = undoPosition + 1; i < previousActions.size(); i++) {
            previousActions.remove(i);
         }
      }
      
      undoPosition = previousActions.size() - 1;
   }
   
   public void fill(Tile fillTile, int originX, int originY, boolean undoCaused) {
      // Dont let people fill out of bounds   
      if (!(originX >= 0 && originY >= 0 && originX < map.map.length && originY < map.map[0].length)) return;
      if (Tile.tilesEqual(fillTile, map.map[originX][originY]) || fillTile.image.equals("delete") && map.map[originX][originY] == null) return;
      fillTiles.clear();
      
      List<Point> open = new ArrayList<>();
      Set<Point> closed = new HashSet<>();
      
      Point origin = new Point(originX, originY);
      Tile tileToFill = map.map[originX][originY];
      open.add(origin);
      closed.add(origin);
      
      int tilesFilled = 0;
      while (open.size() > 0) {
         Point current = open.get(0);
         open.remove(0);
         
         fillTiles.add(current);
         tilesFilled++;
         
         int x = current.x;
         int y = current.y;
         
         if (x + 1 < map.map.length && !Tile.tilesEqual(map.map[x + 1][y], fillTile) && Tile.tilesEqual(map.map[x + 1][y], tileToFill) && !closed.contains(new Point(x + 1, y))) {
            open.add(new Point(x + 1, y));
            closed.add(new Point(x + 1, y));
         }
         if (x - 1 >= 0 && !Tile.tilesEqual(map.map[x - 1][y], fillTile) && Tile.tilesEqual(map.map[x - 1][y], tileToFill) && !closed.contains(new Point(x - 1, y))) {
            open.add(new Point(x - 1, y));
            closed.add(new Point(x - 1, y));
         }
         if (y + 1 < map.map[0].length && !Tile.tilesEqual(map.map[x][y + 1], fillTile) && Tile.tilesEqual(map.map[x][y + 1], tileToFill) && !closed.contains(new Point(x, y + 1))) {
            open.add(new Point(x, y + 1));
            closed.add(new Point(x, y + 1));
         }
         if (y - 1 >= 0 && !Tile.tilesEqual(map.map[x][y - 1], fillTile) && Tile.tilesEqual(map.map[x][y - 1], tileToFill) && !closed.contains(new Point(x, y - 1))) {
            open.add(new Point(x, y - 1));
            closed.add(new Point(x, y - 1));
         }
      }
      
      // Add a "fill" action to the undo queue
      if (tileToFill == null) {
         // Save undo actions of null instead as tiles with the tag "delete"
         tileToFill = new Tile(originX, originY, "delete", 0);
         tileToFill.setTags(new String[]{ "delete" });
      }
      fillTile = fillTile.copy();
      fillTile.addTag("delete");
      UndoAction.UndoFill action = new UndoAction.UndoFill(tileToFill, fillTile, tilesFilled);
      if (!undoCaused) {
         addUndoAction(action);
      }
      
      
      // Add/delete all tiles
      int i = 0;
      for (Point point : fillTiles) {
         action.addPoint(i, point);
         if (fillTile.image.equals("delete")) {
            addTile(null, point.x, point.y, true);
         } else {
            addTile(fillTile.copy(), point.x, point.y, true);
         }
         i++;
      }
      
      // Empty the list for good measure
      fillTiles.clear();
   }
      
   public Tile[][] getNeighbors(int tileX, int tileY) {
   
      // Magic
      Tile[][] neighbors = new Tile[3][3];
      for (int i = tileX - 1; i < tileX + 2; i++) {
         for (int j = tileY - 1; j < tileY + 2; j++) {
            if (!(i < 0 || j < 0 || i >= map.map.length || j >= map.map[0].length) && map.map[i][j] != null && !map.map[i][j].ignoreTiling) neighbors[tileX - i + 1][tileY - j + 1] = map.map[i][j];
            else neighbors[tileX - i + 1][tileY - j + 1] = null;
         }
      }
      return neighbors;
   }
   
   public void checkTiling(Tile tile, int tileX, int tileY) {
      if (tile.ignoreTiling) return;
      int tileType = 0;
      
      Tile[][] neighbors = getNeighbors(tileX, tileY);
      
      int numNeighbors = numNeighbors(neighbors);
      // Full piece
      if (numNeighbors == 0) tileType = 0;
      // Empty piece
      else if (numNeighbors == 8) tileType = 5;
      else {
      
         // Seven connecting pieces
         if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 0), new Point(0, 1), new Point(0, 2), new Point(1, 2), new Point(2, 2), new Point(2, 1)) && numNeighbors == 7) {
            tileType = rotateType(6, 2);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(2, 0), new Point(0, 1), new Point(0, 2), new Point(1, 2), new Point(2, 2), new Point(2, 1)) && numNeighbors == 7) {
            tileType = rotateType(6, 1);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 0), new Point(0, 1), new Point(2, 0), new Point(1, 2), new Point(2, 2), new Point(2, 1)) && numNeighbors == 7) {
            tileType = 6;
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 0), new Point(0, 1), new Point(0, 2), new Point(1, 2), new Point(2, 0), new Point(2, 1)) && numNeighbors == 7) {
            tileType = rotateType(6, 3);
            
         // Six connecting pieces
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(1, 2), new Point(2, 0), new Point(2, 1), new Point(2, 2))) {
            tileType = 7;
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(1, 2), new Point(2, 1), new Point(0, 0), new Point(1, 0), new Point(2, 0))) {
            tileType = rotateType(7, 3);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(1, 2), new Point(2, 1), new Point(0, 0), new Point(0, 1), new Point(0, 2))) {
            tileType = rotateType(7, 2);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(2, 1), new Point(0, 2), new Point(1, 2), new Point(2, 2))) {
            tileType = rotateType(7, 1);
         } else if (checkNeighbors(neighbors, new Point(0, 0), new Point(1, 0), new Point(0, 1), new Point(1, 2), new Point(2, 1), new Point(2, 2))) {
            tileType = 8;
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(2, 0), new Point(0, 1), new Point(1, 2), new Point(2, 1), new Point(0, 2))) {
            tileType = rotateType(8, 1);
            
         // Five connecting pieces
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(2, 0), new Point(2, 1), new Point(2, 2), new Point(1, 2)) && neighbors[0][1] == null) {
            tileType = rotateType(4, 2);
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(0, 2), new Point(1, 2), new Point(2, 2), new Point(2, 1)) && neighbors[1][0] == null) {
            tileType = rotateType(4, 3);
         } else if (checkNeighbors(neighbors, new Point(0, 0), new Point(1, 0), new Point(0, 1), new Point(0, 2), new Point(1, 2)) && neighbors[2][1] == null) {
            tileType = 4;
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(0, 0), new Point(1, 0), new Point(2, 0), new Point(2, 1)) && neighbors[1][2] == null) {
            tileType = rotateType(4, 1);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(1, 2), new Point(2, 1), new Point(2, 0))) {
            tileType = rotateType(9, 1);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(1, 2), new Point(2, 1), new Point(2, 2))) {
            tileType = rotateType(9, 2);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(1, 2), new Point(2, 1), new Point(0, 2))) {
            tileType = rotateType(9, 3);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(1, 2), new Point(2, 1), new Point(0, 0))) {
            tileType = 9;
            
         // Four connecting pieces
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(1, 2), new Point(2, 1))) {
            tileType = 10;
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(1, 2), new Point(2, 1), new Point(2, 2))) {
            tileType = rotateType(11, 2);
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(0, 2), new Point(1, 2), new Point(2, 1))) {
            tileType = rotateType(11, 3);
         } else if (checkNeighbors(neighbors, new Point(0, 0), new Point(1, 0), new Point(0, 1), new Point(1, 2))) {
            tileType = 11;
         } else if (checkNeighbors(neighbors, new Point(2, 1), new Point(2, 0), new Point(1, 0), new Point(0, 1))) {
            tileType = rotateType(11, 1);
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(1, 2), new Point(2, 1), new Point(2, 2))) {
            tileType = rotateType(12, 3);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(2, 0), new Point(2, 1), new Point(1, 2))) {
            tileType = rotateType(12, 2);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(0, 2), new Point(1, 2))) {
            tileType = 12;
         } else if (checkNeighbors(neighbors, new Point(0, 0), new Point(1, 0), new Point(0, 1), new Point(2, 1))) {
            tileType = rotateType(12, 1);
            
         // Three connecting pieces
         
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(1, 2))) {
            tileType = 13;
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(1, 2), new Point(2, 1))) {
            tileType = rotateType(13, 3);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(1, 2), new Point(2, 1))) {
            tileType = rotateType(13, 2);
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(0, 1), new Point(2, 1))) {
            tileType = rotateType(13, 1);
         } else if (checkNeighbors(neighbors, new Point(0, 0), new Point(1, 0), new Point(0, 1))) {
            tileType = 2;
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(2, 0), new Point(2, 1))) {
            tileType = rotateType(2, 1);
         } else if (checkNeighbors(neighbors, new Point(2, 1), new Point(2, 2), new Point(1, 2))) {
            tileType = rotateType(2, 2);
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(0, 2), new Point(1, 2))) {
            tileType = rotateType(2, 3);
            
         // Two connecting pieces
         } else if (checkNeighbors(neighbors, new Point(1, 0), new Point(1, 2))) {
            tileType = 3;
         } else if (checkNeighbors(neighbors, new Point(2, 1), new Point(0, 1))) {
            tileType = rotateType(3, 1);
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(1, 0))) {
            tileType = 14;
         } else if (checkNeighbors(neighbors, new Point(2, 1), new Point(1, 0))) {
            tileType = rotateType(14, 1);
         } else if (checkNeighbors(neighbors, new Point(2, 1), new Point(1, 2))) {
            tileType = rotateType(14, 2);
         } else if (checkNeighbors(neighbors, new Point(0, 1), new Point(1, 2))) {
            tileType = rotateType(14, 3);
            
         // One connecting piece
         } else if (checkNeighbors(neighbors, new Point(1, 0))) {
            tileType = 1;
         } else if (checkNeighbors(neighbors, new Point(0, 1))) {
            tileType = rotateType(1, 3);
         } else if (checkNeighbors(neighbors, new Point(1, 2))) {
            tileType = rotateType(1, 2);
         } else if (checkNeighbors(neighbors, new Point(2, 1))) {
            tileType = rotateType(1, 1);
         }
      }
      
      tile.setTileType(tileType);
   }
   
   public int rotateType(int baseType, int rotations) {
      return baseType + rotations * 15;
   }
   
   public boolean checkNeighbors(Tile[][] neighbors, Point... positions) {
      for (Point relPos : positions) {
         if (neighbors[relPos.x][relPos.y] == null) return false;
      }
      return true;
   }
   
   public int numNeighbors(Tile[][] neighbors) {
      int n = 0;
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            if (neighbors[i][j] != null && !(i == 1 && j == 1)) {
               n++;
            }
         }
      }
      return n;
   }
   
   public void undo() {
      if (previouslyRedid) undoPosition--;

      previouslyRedid = false;
      undoPosition = Math.max(0, Math.min(previousActions.size() - 1, undoPosition));
      
      if (previousActions.size() > 0) {
         UndoAction action = previousActions.get(undoPosition);
         action.undo(this);
      }
      undoPosition--;
   }
   
   public void redo() {
      if (!previouslyRedid) undoPosition++;
      
      previouslyRedid = true;
      undoPosition = Math.max(0, Math.min(previousActions.size() - 1, undoPosition));
      
      if (previousActions.size() > 0) {
         UndoAction action = previousActions.get(undoPosition);
         action.redo(this);
      }
      undoPosition++;
   }
   
   public void updateUndoPositions(int x, int y) {
      for (UndoAction action : previousActions) {
         if (action instanceof UndoAction.UndoFill) {
            ((UndoAction.UndoFill) action).offsetPoints(x, y);
         }
         action.oldTile.x += x;
         action.oldTile.y += y;
         action.newTile.x += x;
         action.newTile.y += y;
      }
   }
   
   private static class UndoAction {
      public Tile oldTile;
      public Tile newTile;
            
      public UndoAction(Tile oldTile, Tile newTile) {
         this.oldTile = oldTile;
         this.newTile = newTile;
      }
      
      public void undo(MapEditor editor) {
         if (oldTile.hasTag("delete")) {
            editor.addTile(null, oldTile.x, oldTile.y, true);
         } else {
            editor.addTile(oldTile, oldTile.x, oldTile.y, true);
         }
      }
      
      public void redo(MapEditor editor) {
         if (newTile.hasTag("delete")) {
            editor.addTile(null, newTile.x, newTile.y, true);
         } else {
            editor.addTile(newTile, newTile.x, newTile.y, true);
         }
      }
      
      public static class UndoFill extends UndoAction {
         private Point[] filledPoints;
         private Point offset;

         public UndoFill(Tile oldTile, Tile newTile, int numTiles) {
            super(oldTile, newTile);
            filledPoints = new Point[numTiles];
            offset = new Point(0, 0);
         }
         
         public void addPoint(int index, Point point) {
            filledPoints[index] = point;
         }
         
         public void offsetPoints(int x, int y) {
            offset.x += x;
            offset.y += y;
         }
         
         @Override
         public void undo(MapEditor editor) {
            if (oldTile.image.equals("delete")) {
               for (Point point : filledPoints) {
                  editor.addTile(null, point.x, point.y, true);
               }
            } else {
               for (Point point : filledPoints) {
                  editor.addTile(oldTile.copy(), point.x, point.y, true);
               }
            }
         }
         
         @Override
         public void redo(MapEditor editor) {
            if (newTile.image.equals("delete")) {
               for (Point point : filledPoints) {
                  editor.addTile(null, point.x, point.y, true);
               }
            } else {
               for (Point point : filledPoints) {
                  editor.addTile(newTile.copy(), point.x, point.y, true);
               }
            }
         }
      }
   }
}