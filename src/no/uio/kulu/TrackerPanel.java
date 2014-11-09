package no.uio.kulu;


// TrackerPanel.java
// Andrew Davison, September 2011, ad@fivedots.psu.ac.th

/* Based on the Java OpenNI UserTracker sample

   Displays a depth map where each user is coloured differently. A 2D skeleton is
   drawn over each user's depth image, which shows how the user's 
   joints move over time.

   The skeletons are maintained, updated, and drawn by the Skeletons class.
 */


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.OpenNI.AlternativeViewpointCapability;
import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.DepthMetaData;
import org.OpenNI.GeneralException;
import org.OpenNI.ImageGenerator;
import org.OpenNI.License;
import org.OpenNI.MapOutputMode;
import org.OpenNI.SceneMetaData;
import org.OpenNI.StatusException;
import org.OpenNI.UserGenerator;




interface CameraListener {
	public void userPictureUpdate(int i, BufferedImage userImage);
}

public class TrackerPanel extends JPanel implements Runnable
{
	private static final int MAX_DEPTH_SIZE = 10000;  

	private Color USER_COLORS[] = {
			Color.RED, Color.BLUE, Color.CYAN, Color.GREEN,
			Color.MAGENTA, Color.PINK, Color.YELLOW, Color.WHITE};
	/* colors used to draw each user's depth image, except the last
             (white) which is for the background */ 

	private byte[] imgbytes;
	private int imWidth, imHeight;
	private float histogram[];        // for the depth values
	private int maxDepth = 0;         // largest depth value

	private PlayerController playerController;
	private ArrayList<CameraListener> cameraListeners = new ArrayList<CameraListener>();

	/* the background image and final camera image (with only the users showing).
  The camera image will be built from the Kinect RGB image on each update,
  and then drawn over the static background image. 
	 */
	private BufferedImage backIm, cameraImage;
	private int[] cameraPixels; // holds the pixels that will fill the cameraImage image
	private volatile boolean isRunning;
	// used for the average ms processing information
	private int imageCount = 0, hideBGPixel; // the "hide the background" pixel: this could be any colour so long as its alpha value is 0 
	private long totalTime = 0;
	private DecimalFormat df;
	private Font msgFont;
	//Debugging
	private boolean debug = false;
	// OpenNI
	private Context context;
	private DepthMetaData depthMD;
	private ImageGenerator imageGen; 
	private SceneMetaData sceneMD;
	private DepthGenerator depthGen; 
	private Skeletons skels;   // the users' skeletons
	private Player [] players;
	int [][] userPixels; // Each user's image pixels

	public TrackerPanel(String backFnm)
	{
		playerController = new PlayerController();
		addCameraListener(playerController);
		setBackground(Color.WHITE);
		configOpenNI();

		df = new DecimalFormat("0.#");  // 1 dp
		msgFont = new Font("SansSerif", Font.BOLD, 18);
		histogram = new float[MAX_DEPTH_SIZE];
		backIm = loadImage(backFnm); //Background image
		imWidth = depthMD.getFullXRes();
		imHeight = depthMD.getFullYRes();
		imgbytes = new byte[imWidth * imHeight * 3];  // create empty image bytes array of correct size and type
		System.out.println("Image dimensions (" + imWidth + ", " +
				imHeight + ")");
		hideBGPixel =  new Color(0, 0, 255, 0).getRGB();   // transparent blue
		cameraPixels = new int[imWidth * imHeight]; // create d.s for holding camera pixels and image
		cameraImage =  new BufferedImage( imWidth, imHeight, BufferedImage.TYPE_INT_ARGB);  // the image must have an alpha channel for the transparent blue pixels       


		new Thread(this).start();   // start updating the panel
	} 

	public void addCameraListener(CameraListener cl){
		cameraListeners.add(cl);
	}

	/* create context, depth generator, depth metadata, image generator,
  user generator, scene metadata, and skeletons
	 */
	private void configOpenNI() {
		try {
			context = new Context();

			// add the NITE Licence 
			License license = new License("PrimeSense", 
					"0KOIk2JeIBYClPWVnMoRKn5cdY4=");   // vendor, key
			context.addLicense(license); 

			depthGen = DepthGenerator.create(context);
			imageGen = ImageGenerator.create(context);

			// set the viewpoint of the DepthGenerator to match the ImageGenerator
			boolean hasAltView = 
					depthGen.isCapabilitySupported("AlternativeViewPoint");
			if (hasAltView) { 
				AlternativeViewpointCapability altViewCap = 
						depthGen.getAlternativeViewpointCapability();
				altViewCap.setViewpoint(imageGen); 
			}
			else {
				System.out.println("Alternative ViewPoint not supported"); 
				System.exit(1);
			}

			MapOutputMode mapMode = new MapOutputMode(640, 480, 30);   // xRes, yRes, FPS
			depthGen.setMapOutputMode(mapMode); 
			imageGen.setMapOutputMode(mapMode); 
			context.setGlobalMirror(true);         // set mirror mode 
			depthMD = depthGen.getMetaData(); // use depth metadata to access depth info (avoids bug with DepthGenerator)


			UserGenerator userGen = UserGenerator.create(context);
			sceneMD = userGen.getUserPixels(0);
			// used to return a map containing user IDs (or 0) at each depth location

			skels = new Skeletons(userGen, depthGen);
			skels.addListener(playerController);

			context.startGeneratingAll(); 
			System.out.println("Started context generating..."); 
		} 
		catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}
	}  // end of configOpenNI()



	public Dimension getPreferredSize()
	{ return new Dimension(imWidth, imHeight); }


	public void closeDown()
	{  isRunning = false;  } 


	public void run()
	/* update and display the users-coloured depth image and skeletons
     whenever the context is updated.
	 */
	{
		isRunning = true;
		while (isRunning) {
			try {
				context.waitAnyUpdateAll();
			}
			catch(StatusException e)
			{  System.out.println(e); 
			System.exit(1);
			}
			long startTime = System.currentTimeMillis();
			updateUserDepths();
			//Update player image and send to playercontroller
			screenUsers();
			int len = userPixels.length-1;
			for(int i = 1; i < len; i++){ //User IDs start at 1
				BufferedImage userImage = convertToUserImage(userPixels[i]);
				for(CameraListener cl : cameraListeners )
					cl.userPictureUpdate(i, userImage); // i is the user ID
			}

			skels.update();
			imageCount++;
			totalTime += (System.currentTimeMillis() - startTime);
			repaint();
		}
		// close down
		try {
			context.stopGeneratingAll();
		}
		catch (StatusException e) {}
		context.release();
		System.exit(0);
	}  // end of run()



	private void updateUserDepths()
	/* build a histogram of 8-bit depth values, and convert it to
     depth image bytes where each user is coloured differently */
	{
		ShortBuffer depthBuf = depthMD.getData().createShortBuffer();
		calcHistogram(depthBuf);
		depthBuf.rewind();

		// use user IDs to colour the depth map
		ShortBuffer usersBuf = sceneMD.getData().createShortBuffer();
		/* usersBuf is a labeled depth map, where each pixel holds an
         user ID (e.g. 1, 2, 3), or 0 to denote that the pixel is
         part of the background.  */

		while (depthBuf.remaining() > 0) {
			int pos = depthBuf.position();
			short depthVal = depthBuf.get();
			short userID = usersBuf.get();
			if(depthVal < maxDepth && depthVal >= 0){
				imgbytes[3*pos] = 0;     // default colour is black when there's no depth data
				imgbytes[3*pos + 1] = 0;
				imgbytes[3*pos + 2] = 0;

				if (depthVal != 0) {   // there is depth data
					// convert userID to index into USER_COLORS[]
					int colorIdx = userID % (USER_COLORS.length-1);   // skip last color

					if (userID == 0)    // not a user; actually the background
						colorIdx = USER_COLORS.length-1;   
					// use last index: the position of white in USER_COLORS[]

					// convert histogram value (0.0-1.0f) to a RGB color
					float histValue = histogram[depthVal];
					imgbytes[3*pos] = (byte) (histValue * USER_COLORS[colorIdx].getRed());
					imgbytes[3*pos + 1] = (byte) (histValue * USER_COLORS[colorIdx].getGreen());
					imgbytes[3*pos + 2] = (byte) (histValue * USER_COLORS[colorIdx].getBlue());
				}
			}
		}
	}  // end of updateUserDepths()


	private void calcHistogram(ShortBuffer depthBuf)
	{
		// reset histogram
		for (int i = 0; i <= maxDepth; i++)
			histogram[i] = 0;

		// record number of different depths in histogram[]
		int numPoints = 0;
		maxDepth = 0;
		while (depthBuf.remaining() > 0) {
			short depthVal = depthBuf.get();

			if ((depthVal > 0)  && (depthVal < MAX_DEPTH_SIZE) && (depthVal < histogram.length)){      // skip histogram[0] 	  
				histogram[depthVal]++;
				numPoints++;
			}
		}
		// System.out.println("No. of numPoints: " + numPoints);
		// System.out.println("Maximum depth: " + maxDepth);

		// convert into a cummulative depth count (skipping histogram[0])

		for (int i = 1; i <= maxDepth; i++){
			try{
				histogram[i] += histogram[i-1];
			}catch(Exception e){
				System.out.println("depth :"+maxDepth);
				System.out.println("arr length:"+histogram.length);
				System.out.println("i :"+i);
			}
		}

		/* convert cummulative depth into the range 0.0 - 1.0f
       which will later be used to modify a color from USER_COLORS[] */
		if (numPoints > 0) {
			for (int i = 1; i <= maxDepth; i++)    // skipping histogram[0]
				histogram[i] = 1.0f - (histogram[i] / (float) numPoints);
		}
	}  // end of calcHistogram()

	//-------------------- Draw User and Replace Background -------------------------

	private BufferedImage loadImage(String imFnm)
	{
		BufferedImage image = null;
		try {
			image = ImageIO.read( new File(imFnm));
			System.out.println("Loaded " + imFnm);
		}
		catch (IOException e) 
		{  System.out.println("Unable to load " + imFnm);  }
		return image;
	}  // end of loadImage()

	private void screenUsers()
	{
		// store the Kinect RGB image as a pixel array in cameraPixels
		try {
			ByteBuffer imageBB = imageGen.getImageMap().createByteBuffer();
			convertToPixels(imageBB, cameraPixels);
		}
		catch (GeneralException e) {
			System.out.println(e);
		}
		userPixels = extractUserPixels(cameraPixels);

	}  // end of screenUsers()

	private BufferedImage convertToUserImage(int [] pixels){
		// change the modified pixels into an image
		BufferedImage userImage = new BufferedImage(imWidth, imHeight, BufferedImage.TYPE_INT_ARGB);
		userImage.setRGB(0, 0, imWidth, imHeight, cameraPixels, 0, imWidth);
		return userImage;
	}

	private void convertToPixels(ByteBuffer pixelsRGB, int[] cameraPixels)
	/* Transform the ByteBuffer of pixel data into a pixel array
     Converts RGB bytes to ARGB ints with no transparency. 
	 */
	{
		int rowStart = 0;
		// rowStart will index the first byte (red) in each row;
		// starts with first row, and moves down

		int bbIdx;               // index into ByteBuffer
		int i = 0;               // index into pixels int[]
		int rowLen = imWidth * 3;    // number of bytes in each row
		for (int row = 0; row < imHeight; row++) {
			bbIdx = rowStart;
			// System.out.println("bbIdx: " + bbIdx);
			for (int col = 0; col < imWidth; col++) {
				int pixR = pixelsRGB.get( bbIdx++ );
				int pixG = pixelsRGB.get( bbIdx++ );
				int pixB = pixelsRGB.get( bbIdx++ );
				cameraPixels[i++] = 
						0xFF000000 | ((pixR & 0xFF) << 16) | 
						((pixG & 0xFF) << 8) | (pixB & 0xFF);
			}
			rowStart += rowLen;   // move to next row
		}
	}  // end of convertToPixels()

	private int[][] extractUserPixels(int[] cameraPixels)
	/* assign the "hide BG" value to any image pixels used for non-users
     thereby making it transparent
	 */
	{
		int [][] userPixels = new int[10][cameraPixels.length];

		depthMD = depthGen.getMetaData();    // reassignment to avoid a flickering viewpoint

		// update the user ID map
		ShortBuffer usersBuf = sceneMD.getData().createShortBuffer();
		/* each pixel holds an user ID (e.g. 1, 2, 3), or 0 to 
         denote that the pixel is part of the background.  */

		while (usersBuf.remaining() > 0) {
			int pos = usersBuf.position();
			short userID = usersBuf.get();
			if (userID == 0) {// if not a user (i.e. is part of the background)
				cameraPixels[pos] = hideBGPixel;   // make pixel transparent
				userPixels[userID][pos] = hideBGPixel;
			}
			else{
				userPixels[userID][pos] = cameraPixels[pos];
			}
		}
		return userPixels;
	}  // end of hideBackground()

	// -------------------- drawing -------------------------

	public void paintComponent(Graphics g)
	// Draw the depth image with coloured users, skeletons, and statistics info
	{ 
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;

		double scaleX = this.getSize().width,
				scaleY = this.getSize().height;

		g2d.setFont(msgFont);    // for user status and stats
		AffineTransform prevTransform = g2d.getTransform();      
		if(backIm != null)
			g2d.drawImage(backIm, new AffineTransform(scaleX / backIm.getWidth(), 0, 0, scaleY / backIm.getHeight(), 0, 0), this);
		g2d.transform(new AffineTransform(scaleX / cameraImage.getWidth(), 0, 0, scaleY / cameraImage.getHeight(), 0, 0));

		playerController.drawAll(g2d);
		if(debug)
			writeStats(g2d);	
		g2d.setTransform(prevTransform);
	} // end of paintComponent()




	private void writeStats(Graphics2D g2d)
	/* write statistics in bottom-left corner, or
     "Loading" at start time */
	{
		g2d.setColor(Color.BLUE);
		int panelHeight = getHeight();
		if (imageCount > 0) {
			double avgGrabTime = (double) totalTime / imageCount;
			g2d.drawString("Pic " + imageCount + "  " +
					df.format(avgGrabTime) + " ms", 
					5, panelHeight-10);  // bottom left
		}
		else  // no image yet
			g2d.drawString("Loading...", 5, panelHeight-10);
	}  // end of writeStats()


} // end of TrackerPanel class