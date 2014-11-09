package clicktracking.no.uio.kulu;


// SliderInfo.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/* This class contains GGUI slider information that is sent 
   to the top-level object when the component first 
   enters it's pressed state from the active state.

   The range can vary berween 0 and 100. For a horizontal slider,
   0 is on the left, and for a vertical slider it is at the top.
*/

public class SliderInfo extends ComponentInfo
{
  private int range = 0; 


  public SliderInfo(String nm)
  {  super(nm, ComponentType.SLIDER);  } 

  public void setRange (int r)
  {  range = r;  }

  public int getRange()
  {  return range; }

  public String toString()
  {  return super.toString() + "; range: " + range;  } 

}  // end of SliderInfo class