package clicktracking.no.uio.kulu;


// GGUIsManager.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/* The manager does four tasks:
    * positions the components in a full-screen panel so they all lie
      along one edge of the screen (NORTH, SOUTH, EAST, or WEST).

    * calculate the component's on-screen locations once the JFrame has been
     made visible

    * updateGGUIs() compares the screen-relative hand point to the screen rectangles
      for each GGUI, and makes the one that the point falls within active (the
      others become inactive). The point is passed to the GGUI, but converted
      to use GGUI panel-relative coordinates.

    * resetGGUIs() makes all the GGUI components inactive.
*/

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;


// screen edge position for line of GGUI components
enum Compass {
    NORTH, SOUTH, EAST, WEST
}


public class GGUIsManager
{
  private JPanel compsPanel;     // panel holding the GGUI components

  private ArrayList<GestureGUIPanel> ggComps;    // the GGUI components

  private ArrayList<Rectangle> ggScrRects;     
     /* GGUI rectangular areas on-screen; 
        the coordinates are relative to the screen  */



  public GGUIsManager(int scrWidth, int scrHeight)
  {
    // create a full-screen panel for the components
    compsPanel = new JPanel();
    compsPanel.setLayout(new BorderLayout());
    compsPanel.setBounds(0, 0, scrWidth, scrHeight);
    compsPanel.setOpaque(false);
    compsPanel.setPreferredSize( new Dimension(scrWidth, scrHeight));

    ggComps = new ArrayList<GestureGUIPanel>();
  } // end of GGUIsManager()


  public JPanel getPanel()
  {  return compsPanel;  }



  public void add(GestureGUIPanel ggui)
  {
    System.out.println("Adding gesture GUI: " + ggui.getName());
    ggComps.add(ggui);
  }


  public void locateComponents()
  /* store screen location rectangles for the components;
     this is called from the top-level once the components have been 
     positioned on-screen. */
  {
    ggScrRects = new ArrayList<Rectangle>();
    for(GestureGUIPanel ggui : ggComps) {
      Point pos = ggui.getLocationOnScreen();    // get panel's on-screen coordinate
      Dimension dim = ggui.getSize();
      ggScrRects.add( new Rectangle(pos.x, pos.y, dim.width, dim.height) );
    }
  }  // end of locateComponents()



  public void updateGGUIs(Point scrPt)
  /* scrPt is a hand point defined using screen coordinates.
     Determine if it is inside a GGUI component, make the GGUI active, and
     the other components become inactive. Pass the point, with values
     relative to the GGUI panel, to the GGUI.
  */
  {
    int xPos = scrPt.x;
    int yPos = scrPt.y;
    for(int i=0; i < ggComps.size(); i++) {
      GestureGUIPanel ggui = ggComps.get(i);
      if (ggScrRects.get(i).contains(xPos, yPos)) { 
        Point guiPt = new Point(xPos, yPos);    // this point will now be changed
        SwingUtilities.convertPointFromScreen(guiPt, ggui);    
            // convert values in guiPt so relative to the the component's rectangle
        ggui.updateState(guiPt);    // make this component active
      }
      else
        ggui.updateState(null);    // this component will become inactive
    }
  }  // end of updateGGUIs()



  public void inactivateGGUIs()
  // make all the components inactive
  { for(GestureGUIPanel ggui : ggComps)
      ggui.updateState(null);
  } 



  // ------------------- compass setting ------------------------


  public void setEdge(Compass heading)
  // position the components along one edge of the panel
  {
    switch (heading) {
      case NORTH: positionNorth(); break;
      case EAST: positionEast(); break;
      case SOUTH: positionSouth(); break;
      case WEST: positionWest(); break;
    }
  }  // end of setEdge()



  private void positionNorth()
  // place the components in a flowlayout panel to the north (top of screen) 
  {
    JPanel p = new JPanel();
    p.setOpaque(false);
    p.setLayout(new FlowLayout(FlowLayout.CENTER));

    for(GestureGUIPanel ggui : ggComps) {
      ggui.setAlignmentY(Component.TOP_ALIGNMENT);
      p.add(ggui);
    }
    compsPanel.add(p, BorderLayout.NORTH);
  } // end of positionNorth()



  private void positionEast()
  // place the components in a vertical layout to the east (right side of screen) 
  {
    JPanel p = new JPanel();
    p.setOpaque(false);
    p.setLayout(new VerticalLayout(5, VerticalLayout.RIGHT, VerticalLayout.CENTER));

    for(GestureGUIPanel ggui : ggComps) {
      ggui.setAlignmentX(Component.RIGHT_ALIGNMENT);
      p.add(ggui);
    }
    compsPanel.add(p, BorderLayout.EAST);
  } // end of positionEast()



  private void positionSouth()
  // place the components in a flowlayout to the south (bottom of screen) 
  {
    JPanel p = new JPanel();
    p.setOpaque(false);
    p.setLayout(new FlowLayout(FlowLayout.CENTER));

    for(GestureGUIPanel ggui : ggComps) {
      ggui.setAlignmentY(Component.BOTTOM_ALIGNMENT);
      p.add(ggui);
    }
    compsPanel.add(p, BorderLayout.SOUTH);
  } // end of positionSouth()



  private void positionWest()
  // place the components in a vertical layout to the west (left side of screen) 

  {
    JPanel p = new JPanel();
    p.setOpaque(false);
    p.setLayout(new VerticalLayout(5, VerticalLayout.LEFT, VerticalLayout.CENTER));

    for(GestureGUIPanel ggui : ggComps) {
      ggui.setAlignmentX(Component.LEFT_ALIGNMENT);
      p.add(ggui);
    }
    compsPanel.add(p, BorderLayout.WEST);
  } // end of positionWest()


 }  // end of GGUIsManager