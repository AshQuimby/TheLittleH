package src.map;

import java.io.File;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import src.TileEditor;
import src.util.dialogue.*;

public class CampaignState extends PlayState {

   private Dialogue currentDialogue; 
   private int levelId;

   public CampaignState(String referenceStreamPath, int levelId) {
      super(referenceStreamPath);
      this.levelId = levelId;
   }
   
   public CampaignState(File referenceFile) {
      super(referenceFile);
   }
   
   @Override
   public void update() {
      if (player == null) return;
      
      if (currentDialogue != null) {
         for (int i = 0; i < player.keys.length; i++) {
            player.keys[i] = false;
         }
         if (currentDialogue.finished()) currentDialogue = null;
         // return;
      }
      // if (Dialogues.hasUnspentDialogue("intro.dlg")) currentDialogue = Dialogues.getDialogue("intro.dlg");
      super.update();
   }
   
   public void setDialogue(Dialogue dialogue) {
      this.currentDialogue = dialogue;
   }
   
   @Override
   public void resetToCheckpointState() {
      currentDialogue = null;
      super.resetToCheckpointState();
   }
   
   @Override
   public void keyReleased(int keyCode) {
      if (currentDialogue != null) return;
      super.keyReleased(keyCode);
   }
   
   @Override
   public void keyPressed(int keyCode, char character, boolean typed) {
      if (typed) return;
      if (currentDialogue != null) {
         if (keyCode == KeyEvent.VK_ENTER) {
            if (currentDialogue.finishedBlock()) currentDialogue.nextBlock();
            else currentDialogue.toEnd();
         }
         return;
      }
      super.keyPressed(keyCode, character, typed);
   }
   
   @Override
   public void render(Graphics2D g) {
      super.render(g);
      if (currentDialogue != null) {
         currentDialogue.render(g);
      }
   }
}