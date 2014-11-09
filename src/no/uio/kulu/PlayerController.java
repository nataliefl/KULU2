package no.uio.kulu;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.OpenNI.Point3D;

public class PlayerController implements SkeletonEvent, CameraListener{

	Map <Integer, Player> players = new LinkedHashMap <Integer, Player> ();

	public void drawAll(Graphics2D g2d){
		Iterator iterator = players.keySet().iterator();
		while(iterator.hasNext()){
			Integer id = (Integer) iterator.next();
			players.get(id).draw(g2d);
		}
	}

	@Override
	public void userObserved(int id) {
		players.put(id, new Player());

	}

	@Override
	public void userLost(int id) {
		players.remove(id);

	}

	@Override
	public void userCalibrated(int id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateHeadPosition(int userID, Point3D pivot, Point3D base,
			int angle) {
		Player p = players.get(userID);
		p.updateSkeletonImagePosition(Player.ImagePosition.HEAD, pivot, base, angle);
	}

	@Override
	public void updateBodyPosition(int userID, Point3D pivot, Point3D base,
			int angle) {
		Player p = players.get(userID);
		if(p != null)
			p.updateSkeletonImagePosition(Player.ImagePosition.BODY, pivot, base, angle);

	}

	@Override
	public void updateFeetPosition(int userID, Point3D pivot, Point3D base,
			int angle) {


	}

	@Override
	public void userPictureUpdate(int userID, BufferedImage userImage) {
		Player player = players.get(userID);
		if(player != null)
			player.setPlayerImage(userImage);

	}

	@Override
	public void updateLeftHandPosition(int userID, Point3D pivot,
			Point3D base, int angle) {
		Player p = players.get(userID);
		if(p != null)
			p.updateSkeletonImagePosition(Player.ImagePosition.LEFT_HAND, pivot, base, angle);

	}

	@Override
	public void updateRightHandPosition(int userID, Point3D pivot,
			Point3D base, int angle) {
		Player p = players.get(userID);
		if(p != null)
			p.updateSkeletonImagePosition(Player.ImagePosition.RIGHT_HAND, pivot, base, angle);

	}


}
