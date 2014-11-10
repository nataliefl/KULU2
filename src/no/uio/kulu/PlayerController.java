package no.uio.kulu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ObjectInputStream.GetField;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import no.uio.kulu.Player.ImagePosition;

import org.OpenNI.Point3D;

public class PlayerController implements SkeletonEvent, CameraListener{

	Map <Integer, Player> players = new LinkedHashMap <Integer, Player> ();
	private String message; //On screen message
	
	public PlayerController(){
		setDefaultMessage();
	}
	
	public void drawAll(Graphics2D g2d, Dimension size){
		Iterator iterator = players.keySet().iterator();
		while(iterator.hasNext()){
			Integer id = (Integer) iterator.next();
			players.get(id).draw(g2d, size);
		}
		Font font = new Font("SansSerif", Font.BOLD, 36);
		 g2d.setFont(font);
		 g2d.setColor(Color.white);
		if(message != null){
			FontMetrics fontMetrics = g2d.getFontMetrics(font);
			Rectangle2D fontRect = fontMetrics.getStringBounds(message, g2d);
			g2d.drawString(message, (int)((size.getWidth() / 2 ) - fontRect.getCenterX()) , 50);
		}
	}

	@Override
	public void userObserved(int id) {
		players.put(id, new Player());
		toggleMessage("Hei, du er spiller "+id+". Hold hendene rett opp for å begynne.");

	}

	@Override
	public void userLost(int id) {
		players.remove(id);
		toggleMessage("Hvor ble du av nummer "+id+"?");
		//If no more players
		if(players.size() == 0){
			setDefaultMessage();
		}
	}

	@Override
	public void userCalibrated(int id) {
		toggleMessage("Da er jeg klar. Pynter litt på nummer "+ id +". :)");

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

	//Display on screen text to the Player
	public void toggleMessage(String message){
		if(message != null)
			this.message = message;
		else message = null;
	}

	public void toggleMessage(){
		message = null;
	}
	
	public void setDefaultMessage(){
		message = "Heisann, prøv å gå foran meg.";
	}
	
}
