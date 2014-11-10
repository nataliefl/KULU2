package clicktracking.no.uio.kulu;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JPanel;

import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.GeneralException;
import org.OpenNI.GestureGenerator;
import org.OpenNI.HandsGenerator;
import org.OpenNI.IObservable;
import org.OpenNI.IObserver;
import org.OpenNI.ImageGenerator;
import org.OpenNI.MapOutputMode;
import org.OpenNI.PixelFormat;
import org.OpenNI.Point3D;
import org.OpenNI.StatusException;

import com.primesense.NITE.HandEventArgs;
import com.primesense.NITE.HandPointContext;
import com.primesense.NITE.NullEventArgs;
import com.primesense.NITE.PointControl;
import com.primesense.NITE.SessionManager;



public class GestureGUI extends JPanel implements Runnable, ComponentListener
{

	//OpenNI
	private Context context;
	private DepthGenerator depthGen;
	private ImageGenerator imageGen;
	private SessionManager sessionMan;
	//Custom
	//	private CameraPanel camPanel;
	private GGUIsManager gguisMan;     // manager of GGUI components

	private static final int XRES = 640;    // dimensions of Kinect camera image
	private static final int YRES = 480;

	private int scrWidth, scrHeight;     // dimensions of the screen
	private double scaleFactor = 1.0;   // for scaling image and hand points

	public GestureGUI(Context context)
	{	
		super();
		this.context = context;

		configKinect();

		makeGUI();

		gguisMan.locateComponents();
		
		addComponentListener(this);

	} // end of TestGestureGUIs()

	private void configKinect()
	// set up OpenNI and NITE generators and listerners
	{
		try {
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

			// set up session manager and points listener
			sessionMan = new SessionManager(context, "Click,Wave", "RaiseHand");

			sessionMan.addListener( initPointControl() );
		}
		catch (GeneralException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}  // end of configKinect()

	// --------------------Make GUI--------------------
	private void makeGUI(){

		setOpaque(false);
		// GGUI controls manager
		gguisMan = new GGUIsManager();

		// ---- add GGUI components to manager ----
		ButtonPanel buttonPan = new ButtonPanel("Press me", this);
		gguisMan.add(buttonPan);

		add(gguisMan.getPanel());

		/*		SliderPanel sliderPan = new SliderPanel("Slide me", true, this);   
		// true == horizontal; false == vertical
		gguisMan.add(sliderPan);

		DialPanel dialPan = new DialPanel("Turn me", this);
		gguisMan.add(dialPan);*/

		// set screen edge for positioning the components
		gguisMan.setEdge(Compass.WEST);  // NORTH, SOUTH, EAST, WEST
	}  // end of makeGUI()

	// -------------------------------------------------------


	public void announcePress(ComponentInfo ci)
	// called from GGUI panels for reporting component 'press' info
	{  System.out.println("GUI update: " + ci);   }  



	public void run()
	// keep updating the Kinect camera panel
	{
		try {
			context.waitAnyUpdateAll();
			sessionMan.update(context);
		}
		catch(StatusException e){  
			System.out.println(e); 
		}
	}  // end of run()

	// ----------------------- NITE events ----------------------------------

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
				{ 
										System.out.println("Tracking..."); 
				}
			});


			// no active points -- time to refocus
			pointControl.getNoPointsEvent().addObserver( new IObserver<NullEventArgs>() {
				public void update(IObservable<NullEventArgs> observable, NullEventArgs args)
				{ 
					gguisMan.inactivateGGUIs();   // make all components inactive
					System.out.println("Refocus please.");
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


	@Override
	public void componentResized(ComponentEvent e) {
		Dimension scrDim = getSize();   // screen size
		scrWidth = scrDim.width;
		scrHeight = scrDim.height;
		scaleFactor = ((double)scrWidth)/XRES;
		if(gguisMan != null){
			gguisMan.getPanel().setSize(scrDim);
			gguisMan.getPanel().setPreferredSize(scrDim);
			gguisMan.getPanel().invalidate();
			System.out.println("GUI resize "+scrDim);
		}
		System.out.println(scrDim);
		
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

}  // end of TestGestureGUIs