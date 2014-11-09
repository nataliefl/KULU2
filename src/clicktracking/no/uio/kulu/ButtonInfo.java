package clicktracking.no.uio.kulu;


// ButtonInfo.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/* This class contains GGUI button information that is sent 
   to the top-level object when the component first 
   enters it's pressed state from the active state.
*/


public class ButtonInfo extends ComponentInfo
{
  public ButtonInfo(String nm)
  {  super(nm, ComponentType.BUTTON);  } 

}  // end of ButtonInfo class