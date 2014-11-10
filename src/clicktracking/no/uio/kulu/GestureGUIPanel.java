package clicktracking.no.uio.kulu;


// GestureGUIPanel.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/*  A class for updating and drawing a gesture GUI (GGUI) component.

    The component can change between three states (inactive, active, and
    pressed). A component becomes active when the user's hand is inside
    the panel's rectangular area, otherwise it is inactive.

    The component moves from active to pressed when the user's
    hand has been still for at least STILL_TIME. When the pressed state
    is first entered, a ComponentInfo object (or a subclass) is sent to
    the top-level by calling its announceGGUIPress() method.

    The inactive, active, and pressed states are each rendered differently.

    The inactive and active states are represented by different images that
    are as big as the panel's area.

    The pressed state is represented by a yellow cirle drawn over the active 
    image at the center of the panel.

    There are three update() and draw() methods (one for each state) which
    can be overridden by subclasses.
*/


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import no.uio.kulu.TrackerPanel;


 // possible states for a gesture GUI (GGUI) component
enum GestureState {   
    INACTIVE, ACTIVE, PRESSED
}



public class GestureGUIPanel extends JPanel
{
  private static final String IM_DIR = "images/";

  private static final long STILL_TIME = 2000;   // ms
       // min. duration of no movement to trigger active --> pressed transition
 
  private static final int STILL_DIST2 = 300;   
      /* squared moved distance in pixels below which a hand 
         is considered to be still (i.e. a hand can move a bit) */

  private static final int PRESSED_SIZE = 30;   
         // size of circle drawn in the pressed state


  private BufferedImage inactiveIm = null;
  private BufferedImage activeIm = null;
  private int width, height;    // of inactive/active images, and the panel also

  private String guiLabel;
  private Font labelFont;
  private int xLabel, yLabel;   // drawing position for label

  private GestureState gState = GestureState.INACTIVE;
  private Point currPoint = null;    
          // hand point location inside the GGUI component;
          // the coordinate system is relative to the component panel
  private long lastMovedTime = -1;
          // time when the hand point last moved in the active state

  protected GestureGUI topLevel;   
  private boolean isPressed = false;
          // has the pressed state been announced at the top-level?
  private ComponentInfo compInfo = null;  // info passed to the top-level



  public GestureGUIPanel(String label, ComponentType type, 
                         String inActiveFnm, String activeFnm,
                         GestureGUI top)
  { guiLabel = label;
    setName("\"" + guiLabel + "\" " + type);    // combination of label & type
    topLevel = top;
    setOpaque(false);
    compInfo = new ComponentInfo(guiLabel, type);
    loadStateImages(inActiveFnm, activeFnm);
    positionLabel(guiLabel);
  }  // end of GestureGUIPanel()



  private void loadStateImages(String inActiveFnm, String activeFnm)
  // load the images drawn in the inactive and active states
  {
    inactiveIm = loadImage(inActiveFnm);
    width = inactiveIm.getWidth();
    height = inactiveIm.getHeight();

    Dimension d = new Dimension(width, height);
    setPreferredSize( d );   // fix the panel's dimensions
    setMinimumSize( d );
    setMaximumSize( d );

    activeIm = loadImage(activeFnm);
  }  // end of loadStateImages()



  public BufferedImage loadImage(String fnm)
  // load the image stored in fnm in IM_DIR
  {
    BufferedImage im = null;
    try {
      im = ImageIO.read( new File(IM_DIR+fnm));
    }
    catch(IOException e) {
      System.out.println("Could not load " + IM_DIR + fnm);
      System.exit(1);
    }
    return im;
  }  // end of loadImage()



  private void positionLabel(String guiLabel)
  // determine the drawing position (xLabel, yLabel) for the component's label
  {
    labelFont = new Font("SansSerif", Font.BOLD, 24);
    FontMetrics metrics = getFontMetrics(labelFont);

   // calculate drawing coordinates for label, so centered
   xLabel = (width - metrics.stringWidth(guiLabel))/2; 
   yLabel = (metrics.getAscent() + (height - (metrics.getAscent() + metrics.getDescent()))/2);
  }  // end of positionLabel()


  // --------------- get methods --------------------


  public Point getPoint()
  {  return currPoint;  }

  public int getWidth()
  {  return width;  }

  public int getHeight()
  {  return height;  }



  // -------------updating --------------------------


  public void updateState(Point handPt)
  /* The hand point causes a transition between the inactive, active,
     and pressed states, and the relevant update method is called.
     The point's coordinates are defined relative to the component's panel.
  */
  {
    if (handPt == null) {     // ggui component is inactive
      gState = GestureState.INACTIVE;
      lastMovedTime = -1;
      updateInActive();
    }
    else {    // ggui component is active
      gState = GestureState.ACTIVE;
      if (lastMovedTime == -1) {  // newly active, so restart time
        lastMovedTime = System.currentTimeMillis();
        isPressed = false;
        updateActive(handPt);
      }
      else {   // previously active
        long duration = (System.currentTimeMillis() - lastMovedTime);
        if (duration > STILL_TIME) {    // waited long enough?
          if (closeTo(currPoint, handPt)) {    // hand has not moved
            gState = GestureState.PRESSED;
            if (!isPressed) {    // 'pressed' is announced only once at top-level
              updatePressed(); 
              isPressed = true;
            }
          }
          else {   // moved, so restart time
            lastMovedTime = System.currentTimeMillis();
            isPressed = false;
            updateActive(handPt);
          }
        }
        else { // not waited long enough yet
          if (!closeTo(currPoint, handPt)) {    // hand has moved, so restart time
            lastMovedTime = System.currentTimeMillis();
            isPressed = false;
            updateActive(handPt);
          }
        }
      }
    }
    repaint();
  }  // end of updateState()



  private boolean closeTo(Point currPt, Point newPt)
  // is the new hand pt close enough to the current hand pt?
  {
    if ((currPt == null) || (newPt == null))
      return false;

    int xDiff2 = (currPt.x - newPt.x) * (currPt.x - newPt.x);
    int yDiff2 = (currPt.y - newPt.y) * (currPt.y - newPt.y);
    return ((xDiff2 + yDiff2) <= STILL_DIST2);
  }  // end of closeTo()


  // these 3 update methods can be overridden in the subclasses

  public void updateInActive() 
  {  currPoint = null;  }


  public void updateActive(Point handPt) 
  // store hand position inside GGUI
  {  currPoint = handPt;  }


  public void updatePressed() 
  {  topLevel.announcePress(compInfo);  }


  // --------------- drawing -----------------------------


  public void paintComponent(Graphics g)
  // render the current state, and the component label
  { 
    super.paintComponent(g);

    // draw one of the states
    switch (gState) {
      case INACTIVE: 
        drawInactive(g); break;
      case ACTIVE: 
        drawActive(g); break;
      case PRESSED: 
        drawActive(g); 
        drawPressed(g);    // pressed is drawn on top of active
        break;
    }

    // draw the label
    g.setColor(Color.WHITE);
    g.setFont(labelFont);
    g.drawString(guiLabel, xLabel, yLabel);
  } // end of paintComponent()


  // these 3 draw methods can be overridden in the subclasses

  public void drawInactive(Graphics g) 
  {  g.drawImage(inactiveIm, 0, 0, null);  }


  public void drawActive(Graphics g) 
  {  g.drawImage(activeIm, 0, 0, null);  }


  public void drawPressed(Graphics g)
  {  
    int x = (width - PRESSED_SIZE)/2;
    int y = (height - PRESSED_SIZE)/2;
    g.setColor(Color.YELLOW);   // draw a yellow circle in center
    g.fillOval(x, y, PRESSED_SIZE, PRESSED_SIZE);
 }  // end of drawPressed()


}  // end of GestureGUIPanel class
