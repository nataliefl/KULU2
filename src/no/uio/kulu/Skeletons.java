package no.uio.kulu;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.OpenNI.CalibrationProgressEventArgs;
import org.OpenNI.CalibrationProgressStatus;
import org.OpenNI.DepthGenerator;
import org.OpenNI.IObservable;
import org.OpenNI.IObserver;
import org.OpenNI.Point3D;
import org.OpenNI.PoseDetectionCapability;
import org.OpenNI.PoseDetectionEventArgs;
import org.OpenNI.SkeletonCapability;
import org.OpenNI.SkeletonJoint;
import org.OpenNI.SkeletonJointPosition;
import org.OpenNI.SkeletonProfile;
import org.OpenNI.StatusException;
import org.OpenNI.UserEventArgs;
import org.OpenNI.UserGenerator;


interface SkeletonEvent{
	public void updateHeadPosition(int userID, Point3D pivot, Point3D base, int angle);
	public void updateBodyPosition(int userID, Point3D pivot, Point3D base, int angle);
	public void updateFeetPosition(int userID, Point3D pivot, Point3D base, int angle);
	public void userObserved(int id);
	public void userLost(int id);
	public void userCalibrated(int id);
	public void updateLeftHandPosition(int userID, Point3D pivot,
			Point3D base, int angle);
	public void updateRightHandPosition(int userID, Point3D pivot,
			Point3D base, int angle);
}


// Skeletons.java
// Andrew Davison, September 2011, ad@fivedots.psu.ac.th

/* Skeletons sets up four 'observers' (listeners) so that 
   when a new user is detected in the scene, a standard pose for that 
   user is detected, the user skeleton is calibrated in the pose, and then the
   skeleton is tracked. The start of tracking adds a skeleton entry to userSkels.

   Each call to update() updates the joint positions for each user's
   skeleton.

   Each call to draw() draws each user's skeleton, with a rotated HEAD_FNM
   image for their head, and status text at the body's center-of-mass.
 */

public class Skeletons{

	// used to colour a user's limbs so they're different from the user's body color 
	private Color USER_COLORS[] = {
			Color.RED, Color.BLUE, Color.CYAN, Color.GREEN,
			Color.MAGENTA, Color.PINK, Color.YELLOW, Color.WHITE};
	// same user colors as in TrackersPanel

	private boolean debug = false; //Set true for on screen debug data

	// OpenNI
	private UserGenerator userGen;
	private DepthGenerator depthGen;
	private SkeletonCapability skelCap; // to output skeletal data, including the location of the joints
	private PoseDetectionCapability poseDetectionCap; 	// to recognize when the user is in a specific position
	private String calibPoseName = null;

	private HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>> userSkels;

	/* userSkels maps user IDs --> a joints map (i.e. a skeleton)
       skeleton maps joints --> positions (was positions + orientations)
	 */

	//Arrays of listeners who recives updates from the Skeleton class
	private List <SkeletonEvent> listeners = new ArrayList<SkeletonEvent>();

	public Skeletons(UserGenerator userGen, DepthGenerator depthGen)
	{
		this.userGen = userGen;
		this.depthGen = depthGen;

		configure();
		userSkels = new HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>>();
	} // end of Skeletons()



	/**
	 * Removes SkeletonImage from the Skeleton image collection. 
	 * @author magnuslien
	 * @param Value that matches the SkeletonImage's name property.
	 * @return Returns true if item found and removed, false otherwise.
	 * @see SkeletonImage
	 */
	//	public boolean removeSkeletonImage(String name){
	//		int index = skeletonImageList.indexOf(getSkeletonImage(name));
	//		if(index > 0){
	//			skeletonImageList.remove(index);
	//			return true;
	//		}else{
	//			System.out.print("Skeleton image not removed. Name did not match any items in the list.");
	//			return false;
	//		}
	//	}
	//	public boolean setSkeletonImage(){ //Int playerid, name of image, position
	//		return false;
	//	}
	//	private SkeletonImage getSkeletonImage(String name){
	//		for(SkeletonImage s: skeletonImageList){
	//			if(s.getName() == name) {
	//				return s;
	//			}
	//		}
	//		return null;
	//	}

	private BufferedImage loadImage(String fnm)
	// load the image from fnm
	{
		BufferedImage im = null;
		try {
			im = ImageIO.read( new File(fnm));   
			System.out.println("Loaded image from " + fnm); 
		}
		catch (Exception e) 
		{ System.out.println("Unable to load image from " + fnm);  }   

		return im;
	}  // end of loadImage()

	public void addListener(SkeletonEvent se){
		if(se != null) 
			listeners.add(se);
	}
	public void removeListener(SkeletonEvent se){
		if(se != null)
			listeners.remove(se);
	}

	/* create pose and skeleton detection capabilities for the user generator, 
    and set up observers (listeners)   */
	private void configure(){
		try {
			// setup UserGenerator pose and skeleton detection capabilities;
			// should really check these using ProductionNode.isCapabilitySupported()
			poseDetectionCap = userGen.getPoseDetectionCapability();

			skelCap = userGen.getSkeletonCapability();
			calibPoseName = skelCap.getSkeletonCalibrationPose();  // the 'psi' pose
			skelCap.setSkeletonProfile(SkeletonProfile.ALL);
			// other possible values: UPPER_BODY, LOWER_BODY, HEAD_HANDS

			// set up four observers
			userGen.getNewUserEvent().addObserver(new NewUserObserver());   // new user found
			userGen.getLostUserEvent().addObserver(new LostUserObserver()); // lost a user

			poseDetectionCap.getPoseDetectedEvent().addObserver(
					new PoseDetectedObserver());  
			// for when a pose is detected

			skelCap.getCalibrationCompleteEvent().addObserver(
					new CalibrationCompleteObserver());
			// for when skeleton calibration is completed, and tracking starts
		} 
		catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}
	}  // end of configure()

	// --------------- updating ----------------------------

	public void update()
	// update skeleton of each user
	{
		try {   
			int[] userIDs = userGen.getUsers();   // there may be many users in the scene
			for (int i = 0; i < userIDs.length; ++i) {
				int userID = userIDs[i];
				if (skelCap.isSkeletonCalibrating(userID))
					continue;    // test to avoid occassional crashes with isSkeletonTracking()
				if (skelCap.isSkeletonTracking(userID))
					updateJoints(userID);
			}
		}
		catch (StatusException e) 
		{  System.out.println(e); }
	}  // end of update()

	private void updateJoints(int userID)
	// update all the joints for this userID in userSkels
	{
		HashMap<SkeletonJoint, SkeletonJointPosition> skel = userSkels.get(userID);

		updateJoint(skel, userID, SkeletonJoint.HEAD);
		updateJoint(skel, userID, SkeletonJoint.NECK);

		updateJoint(skel, userID, SkeletonJoint.LEFT_SHOULDER);
		updateJoint(skel, userID, SkeletonJoint.LEFT_ELBOW);
		updateJoint(skel, userID, SkeletonJoint.LEFT_HAND);

		updateJoint(skel, userID, SkeletonJoint.RIGHT_SHOULDER);
		updateJoint(skel, userID, SkeletonJoint.RIGHT_ELBOW);
		updateJoint(skel, userID, SkeletonJoint.RIGHT_HAND);

		updateJoint(skel, userID, SkeletonJoint.TORSO);
		//updateJoint(skel, userID, SkeletonJoint.WAIST);

		updateJoint(skel, userID, SkeletonJoint.LEFT_HIP);
		updateJoint(skel, userID, SkeletonJoint.LEFT_KNEE);
		updateJoint(skel, userID, SkeletonJoint.LEFT_FOOT);

		updateJoint(skel, userID, SkeletonJoint.RIGHT_HIP);
		updateJoint(skel, userID, SkeletonJoint.RIGHT_KNEE);
		updateJoint(skel, userID, SkeletonJoint.RIGHT_FOOT);
	}  // end of updateJoints()

	/* update the position of the specified user's joint by 
    looking at the skeleton capability
	 */
	private void updateJoint(HashMap<SkeletonJoint, SkeletonJointPosition> skel,
			int userID, SkeletonJoint joint)		
	{
		try {
			// report unavailable joints (should not happen)
			if (!skelCap.isJointAvailable(joint) || !skelCap.isJointActive(joint)) {
								System.out.println(joint + " not available for updates");
				return;
			}

			SkeletonJointPosition pos = skelCap.getSkeletonJointPosition(userID, joint);
			if (pos == null) {
				System.out.println("No update for " + joint);
				return;
			}

			SkeletonJointPosition jPos = null;
			if (pos.getPosition().getZ() != 0)   // has a depth position
				jPos = new SkeletonJointPosition( 
						depthGen.convertRealWorldToProjective(pos.getPosition()),
						pos.getConfidence());
			else  // no info found for that user's joint
				jPos = new SkeletonJointPosition(new Point3D(), 0);
			skel.put(joint, jPos);
			updateHeadPosition(userID, skel);
			updateHandsPosition(userID, skel);
			updateBodyPosition(userID, skel);
		}
		catch (StatusException e) 
		{  System.out.println(e); }
	}  // end of updateJoint()

	private Point3D getJointPos(HashMap<SkeletonJoint, SkeletonJointPosition> skel, 
			SkeletonJoint j)
			// get the (x, y, z) coordinate for the joint (or return null)
	{
		SkeletonJointPosition pos = skel.get(j);
		if (pos == null)
			return null;

		if (pos.getConfidence() == 0)
			return null;   // don't draw a line to a joint with a zero-confidence pos

		return pos.getPosition();
	}  // end of getJointPos()

	//Find angle between two points
	private int findAngle(Point3D startPoint, Point3D endPoint){
		if(startPoint == null || endPoint == null)
			return 0;
		int angle = 90 - ((int) Math.round( Math.toDegrees(
				Math.atan2(startPoint.getY()-endPoint.getY(), 
						endPoint.getX()- startPoint.getX()) )));
		return angle;
	}

	private void updateHeadPosition(int userID, HashMap<SkeletonJoint, SkeletonJointPosition> skel) {

		Point3D headPt = getJointPos(skel, SkeletonJoint.HEAD);
		Point3D neckPt = getJointPos(skel, SkeletonJoint.NECK);

		if (headPt != null && neckPt != null){
			int angle = findAngle(neckPt, headPt);
			for(SkeletonEvent se : listeners){
				se.updateHeadPosition(userID, headPt, neckPt, angle);
			}
		}
	}  // end of drawHead()


	
		private void updateBodyPosition(int userID, HashMap<SkeletonJoint, SkeletonJointPosition> skel) {
	
			Point3D neckPt = getJointPos(skel, SkeletonJoint.NECK);
			//Prefer waist for more accurate image angle
			Point3D abdomenPt = getJointPos(skel, SkeletonJoint.TORSO);
	
			if (neckPt != null && abdomenPt != null){
				int angle = findAngle(abdomenPt, neckPt);
				for(SkeletonEvent se : listeners){
					se.updateBodyPosition(userID, neckPt, abdomenPt, angle);
				}
			}
		} 
	
	private void updateHandsPosition(int userID, HashMap<SkeletonJoint, SkeletonJointPosition> skel) {


		Point3D leftHandPt = getJointPos(skel, SkeletonJoint.LEFT_HAND);
		Point3D leftElbowPt = getJointPos(skel, SkeletonJoint.LEFT_ELBOW);

		Point3D rightHandPt = getJointPos(skel, SkeletonJoint.RIGHT_HAND);
		Point3D rightElbowPt = getJointPos(skel, SkeletonJoint.RIGHT_ELBOW);

		if (leftHandPt != null || leftElbowPt != null){
			int angle = findAngle(leftElbowPt, leftHandPt);
			for(SkeletonEvent se : listeners){
				se.updateLeftHandPosition(userID, leftHandPt, leftElbowPt, angle);
			}
		}

		if (rightHandPt != null || rightElbowPt != null){
			int angle = findAngle(rightElbowPt, rightHandPt);
			for(SkeletonEvent se : listeners){
				se.updateRightHandPosition(userID, rightHandPt, rightElbowPt, angle);
			}
		}
	} 
	
	//	private void drawFeet(Graphics2D g2d, 
	//			HashMap<SkeletonJoint, SkeletonJointPosition> skel,
	//			BufferedImage leftFootImage, 
	//			BufferedImage rightFootImage) 
	//			// draw a head image rotated around the z-axis to follow the neck-->head line
	//	{
	//		if (leftFootImage == null || rightFootImage == null)
	//			return;
	//
	//		Point3D leftAnklePt = getJointPos(skel, SkeletonJoint.LEFT_ANKLE);
	//		Point3D leftFootPt = getJointPos(skel, SkeletonJoint.LEFT_FOOT);
	//
	//		Point3D rightAnklePt = getJointPos(skel, SkeletonJoint.RIGHT_ANKLE);
	//		Point3D rightFootPt = getJointPos(skel, SkeletonJoint.RIGHT_FOOT);
	//
	//		if (leftAnklePt != null || leftFootPt != null){
	//			int angle = findAngle(leftAnklePt, leftFootPt);
	//			drawRotatedImage(g2d, leftFootPt, leftFootImage, angle);
	//		}
	//
	//		if (rightAnklePt != null || rightFootPt != null){
	//			int angle = findAngle(rightAnklePt, rightFootPt);
	//			drawRotatedImage(g2d, rightFootPt, rightFootImage, angle);
	//		}
	//	} 
	//
	//	


	//	private void drawUserStatus(Graphics2D g2d, int userID) throws StatusException
	//	// draw user ID and status on the skeleton at its center of mass (CoM)
	//	{
	//		Point3D massCenter = depthGen.convertRealWorldToProjective(
	//				userGen.getUserCoM(userID));
	//		String label = null;
	//		if (skelCap.isSkeletonTracking(userID))     // tracking
	//			label = new String("Tracking user " + userID);
	//		else if (skelCap.isSkeletonCalibrating(userID))  // calibrating
	//			label = new String("Calibrating user " + userID);
	//		else    // pose detection
	//			label = new String("Looking for " + calibPoseName + " pose for user " + userID);
	//
	//		if(debug)
	//			g2d.drawString(label, (int) massCenter.getX(), (int) massCenter.getY());
	//		else
	//			System.out.println(label.toString());
	//	}  // end of drawUserStatus()




	//--------------------- 4 observers -----------------------
	/*   user detection --> pose detection --> skeleton calibration -->
	    skeleton tracking (and creation of userSkels entry)
	    + may also lose a user (and so delete its userSkels entry)
	 */
	class NewUserObserver implements IObserver<UserEventArgs>
	{
		@SuppressWarnings("deprecation")
		public void update(IObservable<UserEventArgs> observable, UserEventArgs args)
		{
			System.out.println("Detected new user " + args.getId());
			try {
				// try to detect a pose for the new user
				poseDetectionCap.StartPoseDetection(calibPoseName, args.getId());   // big-S ?
				for(SkeletonEvent se : listeners)
					se.userObserved(args.getId());
			}
			catch (StatusException e)
			{ e.printStackTrace(); }
		}
	}  // end of NewUserObserver inner class

	class LostUserObserver implements IObserver<UserEventArgs>
	{
		public void update(IObservable<UserEventArgs> observable, UserEventArgs args)
		{ System.out.println("Lost track of user " + args.getId());
		userSkels.remove(args.getId());    // remove user from userSkels
		for(SkeletonEvent se : listeners)
			se.userLost(args.getId());
		}
	} // end of LostUserObserver inner class

	class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs>
	{
		public void update(IObservable<PoseDetectionEventArgs> observable,
				PoseDetectionEventArgs args)
		{
			int userID = args.getUser();
			System.out.println(args.getPose() + " pose detected for user " + userID);
			try {
				// finished pose detection; switch to skeleton calibration
				poseDetectionCap.StopPoseDetection(userID);    // big-S ?
				skelCap.requestSkeletonCalibration(userID, true);
			}
			catch (StatusException e)
			{  e.printStackTrace(); }
		}
	}  // end of PoseDetectedObserver inner class

	class CalibrationCompleteObserver implements IObserver<CalibrationProgressEventArgs>
	{
		public void update(IObservable<CalibrationProgressEventArgs> observable,
				CalibrationProgressEventArgs args)
		{
			int userID = args.getUser();
			System.out.println("Calibration status: " + args.getStatus() + " for user " + userID);
			try {
				if (args.getStatus() == CalibrationProgressStatus.OK) {
					// calibration succeeeded; move to skeleton tracking
					System.out.println("Starting tracking user " + userID);
					skelCap.startTracking(userID);
					userSkels.put(new Integer(userID),
							new HashMap<SkeletonJoint, SkeletonJointPosition>());  
					for(SkeletonEvent se : listeners)
						se.userCalibrated(new Integer(userID));
					// create new skeleton map for the user in userSkels
				}
				else    // calibration failed; return to pose detection
					poseDetectionCap.StartPoseDetection(calibPoseName, userID);    // big-S ?
			}
			catch (StatusException e)
			{  e.printStackTrace(); }
		}
	}  // end of CalibrationCompleteObserver inner class

} // end of Skeletons class

