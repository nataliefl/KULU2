package clicktracking.no.uio.kulu;


// DialPanel.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/* A class for updating and drawing a dial gesture GUI (GGUI) component.

   The active state is rendered as a full-size active image, a rotated
   knob image, and a little hand over the current hand position.
   The knob image includes a line, which is initially pointing along the
   +x axis.

   The knob is rotated using the angle value, which records how the hand
   has turned the knob's line from its starting orientation.


   A DialInfo object is sent to the top-level when the pressed state
   is entered. It contains the current angle of the dial knob's line.
*/

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;



public class DialPanel extends GestureGUIPanel
{
  // dial and knob details
  private static final String DIAL_INACTIVE = "dialInactive.png";
  private static final String DIAL_ACTIVE = "dialActive.png";
  private static final String DIAL_KNOB = "knob.png";
  private static final String HAND = "hand.png";


  private BufferedImage handImage = null;
  private BufferedImage knobImage = null;
  private int knobWidth, knobHeight;

  private int angle; // in degrees
  /* angle of the line drawn on the dial's knob
     relative to the +x axis. Plus values are counterclockwise,
     and negative values clockwise. The range is 180 to -180 degrees.
  */

  private DialInfo dialInfo = null;



  public DialPanel(String label, TestGestureGUIs top)
  {
    super(label, ComponentType.DIAL, DIAL_INACTIVE, DIAL_ACTIVE, top);

    // load hand and knob images
    handImage = loadImage(HAND);
    knobImage = loadImage(DIAL_KNOB);
    knobWidth = knobImage.getWidth();
    knobHeight = knobImage.getHeight();

    angle = 0;    // initial angle of knob line to +x-axis
    dialInfo = new DialInfo(label, angle);
  }  // end of DialPanel()



  // -------------updating --------------------------


  public void updateActive(Point newPt)
  { updateKnobAngle(newPt);
    super.updateActive(newPt);   // make new hand point the current point 
  }  // end of updateActive()



  private void updateKnobAngle(Point newPt)
  /* calculate the new angle of the knob line relative to the +x axis,
     by adding the angle change between the new hand position and
     the current hand point.  */
  {
    Point currPt = getPoint();   // current hand position
    if (currPt == null)
      return;

    // calculate current and new hand positions relative to dial's center
    // (which is also the knob's center
    int xCurr = currPt.x - getWidth()/2;
    int yCurr = -1*(currPt.y - getHeight()/2);       // so +y is up the screen

    int xNew = newPt.x - getWidth()/2;
    int yNew = -1*(newPt.y - getHeight()/2); 

    int angleChange = signedAngleChg(xCurr, yCurr, xNew, yNew);
    angle -= angleChange;    // since clockwise rotation is a negative change
    if (angle > 180)
      angle -= 360;
    else if (angle < -180)
      angle += 360;
  }  // end of updateKnobAngle()



  private int signedAngleChg(int xCurr, int yCurr, int xNew, int yNew)
  /* returns the minimum clockwise or counterclockwise rotation from 
     (xCurr, yCurr) to (xNew, yNew)
     in integer degrees.
     A positive sign indicates a rotation from +x-axis towards +y-axis. 
     A negative sign indicates a rotation from +x-axis towards -y-axis.
     (see http://www.euclideanspace.com/maths/algebra/vectors/angleBetween/)
  */
  {
    double angleChg = Math.atan2(yNew, xNew) - Math.atan2(yCurr, xCurr);
    if (angleChg > Math.PI)
      angleChg -= 2*Math.PI;
    else if (angleChg < -Math.PI)
      angleChg += 2*Math.PI;
  
    int angleChgDeg = (int) Math.round( Math.toDegrees(angleChg) );
    // System.out.println("Angle moved (in degrees): " + angleChgDeg);
    return angleChgDeg;
  }  // end of signedAngleChg()



  public void updatePressed()
  // send dial info to top-level
  { dialInfo.setAngle(-angle);   // due to mirroring
    topLevel.announcePress(dialInfo);
  }  // end of updatePressed()



  // --------------- drawing -----------------------------


  public void drawActive(Graphics g)
  /* The active state is rendered by as the full-size active image and
     a knob image (suitably rotated) and a little hand over the 
     current hand point. */
  { 
    super.drawActive(g);    // draw full-size active image
    drawKnob(g, angle);

    // draw hand image centered at (x, y)
    int x, y = 0;
    Point currPt = getPoint();     // current hand position
    if (currPt == null) {    // if no point, then place hand in center of GGUI
      x = (getWidth() - handImage.getWidth())/2;
      y = (getHeight() - handImage.getHeight())/2;
    }
    else {
      x = currPt.x - handImage.getWidth()/2;
      y = currPt.y - handImage.getHeight()/2;
    }
    g.drawImage(handImage, x, y, null);
  }  // end of drawActive()



  private void drawKnob(Graphics g, int angle)
  /* draw the knob image after rotating it so the knob line is 
     angle degrees to the +x axis.  */
  {
    Graphics2D g2d = (Graphics2D) g;

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                         RenderingHints.VALUE_ANTIALIAS_ON);

    AffineTransform origTF = g2d.getTransform();
    AffineTransform newTF = (AffineTransform)(origTF.clone());

    // center of rotation is the center of the dial image
    newTF.rotate( Math.toRadians(angle), getWidth()/2, getHeight()/2);
    g2d.setTransform(newTF);

    //draw knob image centered in dial image
    int x = (getWidth() - knobWidth)/2;
    int y = (getHeight() - knobHeight)/2;
    g2d.drawImage(knobImage, x, y, null);

    g2d.setTransform(origTF);
  }  // end of drawKnob()


}  // end of DialPanel class

