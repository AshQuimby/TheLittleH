package src.map;

import java.io.File;
import java.awt.Graphics2D;

import src.TileEditor;

public class PlayState extends GameState {
   public PlayState(String referenceStreamPath) {
      super();
      this.referenceStreamPath = referenceStreamPath;
      this.referenceStream = GameState.class.getResourceAsStream(this.referenceStreamPath);
      loadMap();
   }
   
   public PlayState(File referenceFile) {
      super();
      this.referenceFile = referenceFile;
      loadMap();
   }
   
   public void finishedReading() {
      super.finishedReading();
      saveCheckpointState();
      if (startPosition == null) {
         endGame();
         TileEditor.error("Level lacks spawn position");
         return;
      }
      player = new Player(startPosition);
      camera.setCenter(player.x + player.width / 2, player.y + player.height / 2);
      timeLeft = timeLimit;
   }
   
   @Override
   public void render(Graphics2D g) {
      updateWindowZoom();
      if (map != null) renderBackground(g);
      super.render(g);
      renderTimer(g);
   }
}