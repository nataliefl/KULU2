package clicktracking.no.uio.kulu;


// ButtonPanel.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/* A class for updating and drawing a button gesture GUI (GGUI) component.

   The pressed state is rendered using a full-size
   PRESSED_FNM image rather than as a yellow circle.

   A ButtonInfo object is sent to the top-level when the pressed state
   is entered.
*/

import java.awt.Graphics;
import java.awt.image.BufferedImage;



public class ButtonPanel extends GestureGUIPanel
{
  // button images for each of its states
  private static final String INACTIVE_FNM = "buttonInactive.png";
  private static final String ACTIVE_FNM = "buttonActive.png";
  private static final String PRESSED_FNM = "buttonPressed.png";

  private BufferedImage pressedIm = null;
  private ButtonInfo buttonInfo = null;


  public ButtonPanel(String label, GestureGUI gestureGUI)
  {
    super(label, ComponentType.BUTTON, INACTIVE_FNM, ACTIVE_FNM, gestureGUI);
    setOpaque(false);
    pressedIm = loadImage(PRESSED_FNM);
    buttonInfo = new ButtonInfo(label);
  }  // end of ButtonPanel()


  // -------------updating --------------------------

  public void updatePressed()
  // send button info to top-level
  {  topLevel.announcePress(buttonInfo);  }  


  // --------------- drawing -----------------------------

  public void drawPressed(Graphics g)
  // draw the pressed image loaded from PRESSED_FNM
  {  g.drawImage(pressedIm, 0, 0, null); }


}  // end of ButtonPanel class



