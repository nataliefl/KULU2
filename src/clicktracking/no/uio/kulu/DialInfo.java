package clicktracking.no.uio.kulu;


// DialInfo.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/* This class contains GGUI dial information that is sent 
   to the top-level object when the component first 
   enters it's pressed state from the active state.

   The angle is relative to the +x axis, with plus values counterclockwise,
   and negative values clockwise. The range is 180 to -180 degrees.
*/

public class DialInfo extends ComponentInfo
{
  private int angle;


  public DialInfo(String nm, int ang)
  {  super(nm, ComponentType.DIAL);  
     angle = ang;
  } 

  public void setAngle(int a)
  { angle = a; }

  public int getAngle()
  { return angle; }

  public String toString()
  {  return super.toString() + "; angle: " + angle;  } 

}  // end of DialInfo class