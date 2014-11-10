package clicktracking.no.uio.kulu;


// SliderPanel.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th


/* A class for updating and drawing a slider gesture GUI (GGUI) component.

   Two versions of the slider are possible: a horizontal slider or a vertical
   slider.

   The slider is made up of a background image and a foreground slider tab image
   which can move (either horizontally or vertically). The tab can only
   move within the range limits (MIN_HORIZ_POS-MAX_HORIZ_POS or
   MIN_VERT_POS-MAX_VERT_POS). 

   The tab image is changed when the slider is in the pressed state.
   
   A SliderInfo object is sent to the top-level when the pressed state
   is entered. It contains the current range value for the slider, scaled to
   be an integer between 0 and 100.
*/


import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;



public class SliderPanel extends GestureGUIPanel
{
  // horizontal sliders and tabs
  private static final String HORIZ_INACTIVE = "hSliderInactive.png";
  private static final String HORIZ_ACTIVE = "hSliderActive.png";
  private static final String HORIZ_TAB = "downTab.png";       // (in)active tab
  private static final String HORIZ_ON_TAB = "downOnTab.png";   // pressed

  // range limits of the horizontal slider
  private static final int MIN_HORIZ_POS = 55;    // left
  private static final int MAX_HORIZ_POS = 440;   // right
     // these x-axis values come from examining the HORIZ_ACTIVE image


  // vertical sliders and tabs
  private static final String VERT_INACTIVE = "vSliderInactive.png";
  private static final String VERT_ACTIVE = "vSliderActive.png";
  private static final String VERT_TAB = "rightTab.png";      // (in)active tab
  private static final String VERT_ON_TAB = "rightOnTab.png";   // pressed

  // range limits of the vertical slider
  private static final int MIN_VERT_POS = 55;     // top
  private static final int MAX_VERT_POS = 447;    // bottom
     // these y-axis values come from examining the VERT_ACTIVE image


  private boolean isHorizontal;     // orientation of the slider (and tab)

  // active and pressed version of the tab image
  private BufferedImage tabIm = null;        // active
  private BufferedImage tabOnIm = null;      // pressed
  private int tabWidth, tabHeight;
  private int xTabPos, yTabPos;   // current position of the tab

  private SliderInfo sliderInfo = null;




  public SliderPanel(String label, boolean isHoriz, GestureGUI gestureGUI)
  {
    super(label, ComponentType.SLIDER,
             (isHoriz) ? HORIZ_INACTIVE : VERT_INACTIVE,
             (isHoriz) ? HORIZ_ACTIVE : VERT_ACTIVE, 
          gestureGUI);
    setOpaque(false);
    isHorizontal = isHoriz;
    initSliderTabs();
    sliderInfo = new SliderInfo(label);
  }  // end of SliderPanel()



  private void initSliderTabs()
  /* Load either the horizontal or vertical versions of
     the active and pressed tabs. Also set the tab's
     initial position in (xTabPos, yTabPos). */
  {
    // load active tab
    String tabFnm = (isHorizontal) ? HORIZ_TAB : VERT_TAB;
    tabIm = loadImage(tabFnm);
    tabWidth = tabIm.getWidth();
    tabHeight = tabIm.getHeight();

    // load pressed tab
    String tabOnFnm = (isHorizontal) ? HORIZ_ON_TAB : 
                                               VERT_ON_TAB;
    tabOnIm = loadImage(tabOnFnm);

    // set tab's position depending on slider orientation
    if (isHorizontal) {
      xTabPos = MIN_HORIZ_POS - tabWidth/2;
      yTabPos = getHeight()/2 - tabHeight/2;
    }
    else {   // slider is vertical
      xTabPos = getWidth()/2 - tabWidth/2;
      yTabPos = MIN_VERT_POS - tabHeight/2;
    }
  }  // end of initSliderTabs()



  // -------------updating --------------------------


  public void updateActive(Point newPt)
  { updateTabPos(newPt);
    super.updateActive(newPt);   // make new hand point the current point 
  }  // end of updateActive()



  private void updateTabPos(Point newPt)
  /* Use the new hand position to update the tab's position, but
     only within the range limits for the slider. */
  {
    xTabPos = newPt.x;
    yTabPos = newPt.y;

    // apply slider limits to (xTabPos, yTabPos)
    if (isHorizontal) {
      if (xTabPos < (MIN_HORIZ_POS - tabWidth/2))
        xTabPos = MIN_HORIZ_POS - tabWidth/2;
      else if (xTabPos > (MAX_HORIZ_POS - tabWidth/2))
        xTabPos = MAX_HORIZ_POS - tabWidth/2;
      yTabPos = getHeight()/2 - tabHeight/2;
    }
    else {   // slider is vertical
      if (yTabPos < (MIN_VERT_POS - tabHeight/2))
        yTabPos = MIN_VERT_POS - tabHeight/2;
      else if (yTabPos > (MAX_VERT_POS - tabHeight/2))
        yTabPos = MAX_VERT_POS - tabHeight/2;
      xTabPos = getWidth()/2 - tabWidth/2;
    }
  }  // end of updateTabPos()




  public void updatePressed()
  // send slider info to top-level
  { sliderInfo.setRange( calcRange() );
    topLevel.announcePress(sliderInfo);
  }  // end of updatePressed()



  private int calcRange()
  /*  Convert the tab position into a range value (an 
      integer between 0 and 100).  */
  {
    double range = 0;
    if (isHorizontal) {
       int maxSpan = MAX_HORIZ_POS - MIN_HORIZ_POS;
       range = ((double)(xTabPos + tabWidth/2 - MIN_HORIZ_POS))/maxSpan;
    }
    else {   // slider is vertical
       int maxSpan = MAX_VERT_POS - MIN_VERT_POS;
       range = ((double)(yTabPos + tabHeight/2 - MIN_VERT_POS))/maxSpan;
    }
    // System.out.println("Current range (0-100): " + range*100);
    return (int) Math.round(range*100);
  }  // end of calcRange()



  // --------------- drawing -----------------------------


  public void drawInactive(Graphics g)
  // draw tab on top of inactive image
  { 
    super.drawInactive(g); 
    g.drawImage(tabIm, xTabPos, yTabPos, null);
  }  // end of drawInactive()


  public void drawActive(Graphics g)
  // draw tab on top of active image
  { 
    super.drawActive(g); 
    g.drawImage(tabIm, xTabPos, yTabPos, null);
  }  // end of drawActive()


  public void drawPressed(Graphics g)
  // pressed state is represented by the pressed tab
  {  g.drawImage(tabOnIm, xTabPos, yTabPos, null);  }  


}  // end of SliderPanel class
