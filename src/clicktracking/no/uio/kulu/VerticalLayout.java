package clicktracking.no.uio.kulu;


// VerticalLayout.java
// Colin Mummery, colin_mummery@yahoo.com 

// Homepage: http://www.kagi.com/equitysoft, 12th July 2001
// http://www.java2s.com/Code/Java/Swing-JFC/
//           AverticallayoutmanagersimilartojavaawtFlowLayout.htm

/* A vertical layout manager similar to java.awt.FlowLayout.

  Like FlowLayout, components do not expand to fill the available 
  space except when the horizontal alignment is BOTH, in which case 
  components are stretched horizontally.

  Unlike FlowLayout, components will not wrap to form another
  column if there isn't enough space vertically.

  VerticalLayout can optionally anchor components
  to the top or bottom of the display area or center them between the 
  top and bottom.

  Based on 'FlexLayout' in "Java Class Libraries", Vol 2, 
  Patrick Chan and Rosanna Lee, Addison-Wesley, 1997
*/



import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Hashtable;



public class VerticalLayout implements LayoutManager
{
  /* horizontal alignment constant for centering. 
     Also used for center anchoring. */
  public final static int CENTER = 0;

  // horizontal right justification
  public final static int RIGHT = 1;

  // horizontal left justification
  public final static int LEFT = 2;

  // alignment constant for stretching the component horizontally
  public final static int BOTH = 3;

  // for anchoring to the top of the display area
  public final static int TOP = 1;

  // for anchoring to the bottom of the display area
  public final static int BOTTOM = 2;


  private int vgap;       // vertical gap between components... defaults to 5

  private int alignment;  // LEFT, RIGHT, CENTER or BOTH... 
                          // how the components are justified

  private int anchor;     // TOP, BOTTOM or CENTER ...
                          // where the components are positioned in a large space

  private Hashtable comps;


  public VerticalLayout()
  /* Constructs an instance with a vertical vgap of 5 pixels, 
     horizontal centering and anchored to the top of the display area. */
  {  this(5, CENTER, TOP); }


  public VerticalLayout(int vgap)
  /* Constructs an instance with horizontal centering, 
     anchored to the top with the specified vgap.  */
  {  this(vgap, CENTER, TOP);  }


  public VerticalLayout(int vgap, int alignment)
  /* Constructs an instance anchored to the top with the specified 
     vgap and horizontal alignment. */
  {  this(vgap, alignment, TOP); }



  /* Constructs an instance with the specified vgap, horizontal 
     alignment and anchoring.

     *  vgap is an int value indicating the vertical seperation 
        of the components.

     * alignment is an int value (RIGHT, LEFT, CENTER, or BOTH) 
       for the horizontal alignment.

     * anchor is an int value (TOP, BOTTOM, or CENTER) indicating 
       where the components are to appear if the display area exceeds 
       the minimum necessary.
   */
  public VerticalLayout(int vgap, int alignment, int anchor)
  {
    this.vgap = vgap;
    this.alignment = alignment;
    this.anchor = anchor;
  }


  private Dimension layoutSize(Container parent, boolean minimum)
  {
    Dimension dim = new Dimension(0, 0);
    Dimension d;

    synchronized (parent.getTreeLock()) {
      int n = parent.getComponentCount();
      for (int i = 0; i < n; i++) {
        Component c = parent.getComponent(i);
        if (c.isVisible()) {
          d = minimum ? c.getMinimumSize() : c.getPreferredSize();
          dim.width = Math.max(dim.width, d.width);
          dim.height += d.height;
          if (i > 0)
            dim.height += vgap;
        }
      }
    }
    Insets insets = parent.getInsets();
    dim.width += insets.left + insets.right;
    dim.height += insets.top + insets.bottom + vgap + vgap;
    return dim;
  }  // end of layoutSize()


  public void layoutContainer(Container parent)
  {
    Insets insets = parent.getInsets();

    synchronized (parent.getTreeLock()) {
      int n = parent.getComponentCount();
      Dimension pd = parent.getSize();
      int y = 0;

      // work out the total size
      for (int i = 0; i < n; i++) {
        Component c = parent.getComponent(i);
        Dimension d = c.getPreferredSize();
        y += d.height + vgap;
      }
      y -= vgap; // otherwise there's a vgap too many

      // Work out the anchor paint
      if (anchor == TOP)
        y = insets.top;
      else if (anchor == CENTER)
        y = (pd.height - y) / 2;
      else
        y = pd.height - y - insets.bottom;

      // do layout
      for (int i = 0; i < n; i++) {
        Component c = parent.getComponent(i);
        Dimension d = c.getPreferredSize();
        int x = insets.left;
        int wid = d.width;

        if (alignment == CENTER)
          x = (pd.width - d.width) / 2;
        else if (alignment == RIGHT)
          x = pd.width - d.width - insets.right;
        else if (alignment == BOTH)
          wid = pd.width - insets.left - insets.right;
        c.setBounds(x, y, wid, d.height);
        y += d.height + vgap;
      }
    }
  }  // end of layoutContainer()


  public Dimension minimumLayoutSize(Container parent)
  {  return layoutSize(parent, false);  }


  public Dimension preferredLayoutSize(Container parent)
  {  return layoutSize(parent, false);  }


  // not used
  public void addLayoutComponent(String name, Component comp) {}

  // not used
  public void removeLayoutComponent(Component comp) {}


  public String toString()
  { return getClass().getName() + "[vgap=" + vgap + 
                " align=" + alignment + " anchor=" + anchor + "]";
  }

}  // end of VerticalLayout class

