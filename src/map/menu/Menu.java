package src.map.menu;

import java.awt.Point;
import java.awt.Rectangle;

public class Menu<E> {
   public E[] items;
   public int itemIndex;
   // Potential auto rendering and math
   // public Rectangle area;
   // public int popOutAnimation;
   // private Point restingPoint;
   // private Point activePoint;
   
   // This is pretty pointless
   // private boolean useImages;
   
   // These are not pointless
   public Menu<?> subMenu;
   private Rectangle rectangle;
   public int elementWidth;
   public int elementHeight;
   private int itemOffset;
   
   public Menu(E[] items, int elementWidth, int elementHeight, int itemOffset) {
      this.items = items;
      this.elementWidth = elementWidth;
      this.elementHeight = elementHeight;
      // this.useImages = useImages;
      this.itemOffset = itemOffset;
      rectangle = new Rectangle(0, 0, 1, 1);
   }
   
   public Menu(E[] items, int elementWidth, int elementHeight, boolean useless) {
      this(items, elementWidth, elementHeight, 4);
   }
   
   public E setItemIndex(int index) {
      itemIndex = index;
      return getSelectedItem();
   }
   
   public int getLastIndexInBounds(Rectangle bounds) {
      Rectangle[] buttons = getItemButtons();
      for (int i = 0; i < buttons.length; i++) {
         if (bounds.contains(buttons[i])) continue;
         return i--;
      }
      return items.length - 1;
   }
   
   public void setSubMenu(Menu<?> menu) {
      subMenu = menu;
   }
   
   public E getSelectedItem() {
      return items[itemIndex];
   }
   
   public void setPosition(int x, int y) {
      rectangle.x = x;
      rectangle.y = y;
   }
   
   public void setMenuRectangle(int x, int y, int maxY, boolean expandLeft) {
      int height = Math.max(elementHeight + itemOffset * 2, (maxY - y) / (elementHeight + itemOffset) * (elementHeight + itemOffset) + itemOffset);
      int maxElement = height / (elementHeight + itemOffset);
      // if (src.TileEditor.program != null && src.TileEditor.program.tick % 30 == 0) System.out.println((items.length) / Math.max(1, maxElement));
      rectangle = new Rectangle(x, y, (elementWidth + itemOffset) * ((items.length - 1) / Math.max(1, maxElement) + 1) + itemOffset, height);
      if (expandLeft) rectangle.x -= rectangle.width;
   }
   
   public int getRowCount() {
      int maxElement = rectangle.height / (elementHeight + itemOffset);
      return (items.length - 1) / Math.max(1, maxElement) + 1;
   }
   
   public void setElementDimensions(int width, int height, int offset) {
      elementWidth = width;
      elementHeight = height;
      itemOffset = offset;
   }
   
   public Rectangle[] getItemButtons() {
      Rectangle[] buttons = new Rectangle[items.length];
      for (int i = 0; i < items.length; i++) {
         int maxY = Math.max(1, rectangle.height / (elementHeight + itemOffset));
         int relY = i;
         int x = relY / maxY;
         int y = relY % maxY;
         
         buttons[i] = new Rectangle(rectangle.x + x * (elementWidth + itemOffset) + itemOffset, rectangle.y + y * (elementHeight + itemOffset) + itemOffset, elementWidth, elementHeight);
      }
      return buttons;
   }
   
   public int getOverlappedElement(Point point) {
      Rectangle[] buttons = getItemButtons();
      for (int i = 0; i < buttons.length; i++) {
         if (buttons[i].contains(point)) {
            return i;
         }
      }
      return -1;
   }
   
   public Rectangle getMenuRectangle() {
      return rectangle;
   }
   
   public boolean hasSubMenu() {
      return subMenu != null;
   }
   
   public E getItem(int index) {
      return items[index];
   }
   
//    public void update() {
//    }
}