
// TestGestureGUIs.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/*  This class creates a
    full-screen undecorated JFrame made up of two layered
    panels -- one at the back showing the scaled Kinect camera image,
    and a panel of gesture GUI (GGUI) components at the front, 
    oriented along one edge of the screen.

    A thread keeps the camera image uptodate.

    Currently this example creates three GGUIs -- a button GGUI, a 
    horizontal slider GGUI and a dial GGUI, positioned along the top
    edge (north) part of the screen. This is done in makeGUI().

    When a GGUI is 'pressed', it calls TestGestureGUIs.announcePress()
    which prints out GGUI information extracted from a superclass of
    ComponentInfo.

    The application is started by the user performing a click or wave
    gesture. A GGUI 'lights up' when activated, which occurs when the
    user's hand is over the component. The GGUI is 'pressed' when the 
    user hand remains still inside the GGUI area for about 2 seconds.
    The pressing is signalled by a yellow dot appearing on the GGUI
    image.

    You can quit the qpplication by typing ESC, 'q' or ctrl-c.
*/

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.image.*;

import org.OpenNI.*;
import com.primesense.NITE.*;



public class TestGestureGUIs extends JFrame implements Runnable
{
  private static final int XRES = 640;    // dimensions of Kinect camera image
  private static final int YRES = 480;


  private int scrWidth, scrHeight;     // dimensions of the screen
  private double scaleFactor = 1.0;   // for scaling image and hand points

  private CameraPanel camPanel;
  private GGUIsManager gguisMan;     // manager of GGUI components

  private volatile boolean isRunning = true;

  private Context context;
  private DepthGenerator depthGen;
  private ImageGenerator imageGen;
  private SessionManager sessionMan;



  public TestGestureGUIs()
  {
    super("Test Gesture GUI Components");
    
    setFullSize();
    /* calculate scale factor for image, so it will be as wide as 
       the screen; scaleFactor will also be applied to the hand points */
    scaleFactor = ((double)scrWidth)/XRES;
    // System.out.println("Scale factor: " + scaleFactor);

    configKinect();

    Container c = getContentPane();
    c.setLayout(new BorderLayout());
    makeGUI(c);

    initKeyListener();

    addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent e)
      {  isRunning = false; }
    });

    hideCursor(c);
 		setUndecorated(true);       // remove borders around the frame
 		setResizable(false);
 		setVisible(true);

    gguisMan.locateComponents();

    new Thread(this).start();   // start updating context
  } // end of TestGestureGUIs()


  private void setFullSize()
  // set size of JFrame to be full-screen
  {
    // set dimensions to be those of the entire screen
    Dimension scrDim = Toolkit.getDefaultToolkit().getScreenSize();   // screen size
    setSize(scrDim); 

    scrWidth = scrDim.width;
    scrHeight = scrDim.height;
    // System.out.println("Screen dimensions: " + scrWidth + " x " + scrHeight);
  }  // end of setFullSize()



  private void makeGUI(Container c)
  /* the gui is a two-layer panel -- the background is the scaled Kinect
     camera image, while the foreground is a line of three GGUI components
     (a button GGUI, a slider GGUI, and a dial GGUI)  */
  {
    // layer GGUI controls over camera view
    JLayeredPane layeredPane = new JLayeredPane();
    layeredPane.setBounds(0, 0, scrWidth, scrHeight);
    layeredPane.setPreferredSize(new Dimension(scrWidth, scrHeight));
    c.add(layeredPane, BorderLayout.CENTER);

    // camera image 
    camPanel = new CameraPanel(scrWidth, scrHeight, XRES, YRES, scaleFactor);
    layeredPane.add(camPanel, 0, 0);

    // GGUI controls manager
    gguisMan = new GGUIsManager(scrWidth, scrHeight);
    layeredPane.add(gguisMan.getPanel(), 1, 0);


    // ---- add 3 GGUI components to manager ----
    ButtonPanel buttonPan = new ButtonPanel("Press me", this);
    gguisMan.add(buttonPan);

    SliderPanel sliderPan = new SliderPanel("Slide me", true, this);   
                // true == horizontal; false == vertical
    gguisMan.add(sliderPan);

    DialPanel dialPan = new DialPanel("Turn me", this);
    gguisMan.add(dialPan);

    // set screen edge for positioning the components
    gguisMan.setEdge(Compass.NORTH);  // NORTH, SOUTH, EAST, WEST
  }  // end of makeGUI()



  public void announcePress(ComponentInfo ci)
  // called from GGUI panels for reporting component 'press' info
  {  System.out.println("GUI update: " + ci);   }  



  private void configKinect()
  // set up OpenNI and NITE generators and listerners
  {
    try {
      context = new Context();

      // add the NITE Licence
      License licence = new License("PrimeSense", "0KOIk2JeIBYClPWVnMoRKn5cdY4=");
      context.addLicense(licence);

      // set up image and depth generators
      imageGen = ImageGenerator.create(context);
           // for displaying the scene
      depthGen = DepthGenerator.create(context);
           // for converting real-world coords to screen coords

      MapOutputMode mapMode = new MapOutputMode(XRES, YRES, 30);
      imageGen.setMapOutputMode(mapMode);
      depthGen.setMapOutputMode(mapMode); 

      imageGen.setPixelFormat(PixelFormat.RGB24);

      // set Mirror mode for all
      context.setGlobalMirror(true);

      // set up hands and gesture generators
      HandsGenerator hands = HandsGenerator.create(context); 
      hands.SetSmoothing(0.1f);

      GestureGenerator gesture = GestureGenerator.create(context);

      context.startGeneratingAll(); 
      System.out.println("Started context generating..."); 

      // set up session manager and points listener
      sessionMan = new SessionManager(context, "Click,Wave", "RaiseHand");
	    setSessionEvents(sessionMan);

      sessionMan.addListener( initPointControl() );
    }
    catch (GeneralException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }  // end of configKinect()




  private void initKeyListener()
  // define keys for stopping
  {
    addKeyListener( new KeyAdapter() {
       public void keyPressed(KeyEvent e)
       { int keyCode = e.getKeyCode();
         if ((keyCode == KeyEvent.VK_ESCAPE) || (keyCode == KeyEvent.VK_Q) ||
             ((keyCode == KeyEvent.VK_C) && e.isControlDown()) )
                // ESC, q, ctrl-c to stop isRunning 
           isRunning = false;
       }
     });
  }  // end of initKeyListener()




  private void hideCursor(Container c)
  {
    // create a transparent 16 x 16 pixel cursor image
    BufferedImage cursorIm = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

    // create a new blank cursor
    Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                                    cursorIm, new Point(0, 0), "blank cursor");
    // assign the blank cursor to the JFrame
    c.setCursor(blankCursor);
  }  // end of hideCursor()



  public void run()
  // keep updating the Kinect camera panel
  {
    while (isRunning) {
      try {
        context.waitAnyUpdateAll();
        sessionMan.update(context);
        camPanel.update(imageGen);
      }
      catch(StatusException e)
      {  System.out.println(e); 
         break;
      }
    }

    // close down
    try {
      context.stopGeneratingAll();
    }
    catch (StatusException e) {}
    context.release();
    System.exit(0);
  }  // end of run()



  // ----------------------- NITE events ----------------------------------


  private void setSessionEvents(SessionManager sessionMan)
	// create session callback...
  {
    try {
      // session end
      sessionMan.getSessionEndEvent().addObserver( new IObserver<NullEventArgs>() {
        public void update(IObservable<NullEventArgs> observable, NullEventArgs args)
        { isRunning = false; }
      });
    }
    catch (StatusException e) {
      e.printStackTrace();
    }
  }  // end of setSessionEvents()




//----------------------- NITE events ----------------------------------


 private void setSessionEvents(SessionManager sessionMan)
	// create session callback...
 {
   try {
     // session end
     sessionMan.getSessionEndEvent().addObserver( new IObserver<NullEventArgs>() {
       public void update(IObservable<NullEventArgs> observable, NullEventArgs args)
       { isRunning = false; }
     });
   }
   catch (StatusException e) {
     e.printStackTrace();
   }
 }  // end of setSessionEvents()




 private PointControl initPointControl()
 {
   PointControl pointControl = null;
   try {
	    pointControl = new PointControl();

     // a hand is in a new position -- generates lots of events
     // activate the relevant component; deactivate others
	    pointControl.getPointUpdateEvent().addObserver( new IObserver<HandEventArgs>() {
       public void update(IObservable<HandEventArgs> observable, HandEventArgs args)
       { 
         HandPointContext hc = args.getHand();
         gguisMan.updateGGUIs( getScreenCoord(hc.getPosition()) );
       }
     });


     // create entry for a hand
	    pointControl.getPointCreateEvent().addObserver( new IObserver<HandEventArgs>() {
       public void update(IObservable<HandEventArgs> observable, HandEventArgs args)
       { camPanel.setMessage("Tracking...");  }
     });


     // no active points -- time to refocus
	    pointControl.getNoPointsEvent().addObserver( new IObserver<NullEventArgs>() {
       public void update(IObservable<NullEventArgs> observable, NullEventArgs args)
       { 
         gguisMan.inactivateGGUIs();   // make all components inactive
         camPanel.setMessage("Refocus please.");
       }
     });

   }
   catch (GeneralException e) {
     e.printStackTrace();
   }
   return pointControl;
 }  // end of initPointControl()



 private Point getScreenCoord(Point3D posPt)
 /*  Convert hand point in 3D space to Kinect camera coordinates, and
     then scale to screen coordinates  */
 {
    Point scrPt = new Point(scrWidth/2, scrHeight/2);    // default screen pos
    try {
      // convert from real-world 3D to camera coordinates
      Point3D projPt = depthGen.convertRealWorldToProjective(posPt);

      // scale to screen coordinates
      int xPos = (int) Math.round( projPt.getX() * scaleFactor);    
      int yPos = (int) Math.round( projPt.getY() * scaleFactor);
      scrPt.x = xPos;
      scrPt.y = yPos;
    }
    catch (StatusException e) {
      e.printStackTrace();
    }
    return scrPt;
 }  // end of getScreenCoord()



  // -------------------------------------------------------

  public static void main( String args[] )
  {  new TestGestureGUIs();  }

 
 }  // end of TestGestureGUIs