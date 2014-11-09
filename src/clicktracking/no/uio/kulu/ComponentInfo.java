package clicktracking.no.uio.kulu;


// ComponentInfo.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/* This class contains information that is sent to the top-level
   object when a GGUI component first enters it's pressed state
   from the active state.
   ComponentInfo should be subclassed to add additional info.
*/

enum ComponentType {
    BUTTON, SLIDER, DIAL
}


public class ComponentInfo
{
  protected String name = null;
  protected ComponentType type;


  public ComponentInfo(String nm, ComponentType ct)
  { name = nm;
    type = ct;
  }  // end of ComponentInfo()

  public String getName()
  {  return name;  }

  public ComponentType getType()
  {  return type;  }

  public String toString()
  {  return "\"" + name + "\" " + type; } 

}  // end of ComponentInfo class