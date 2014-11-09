package no.uio.kulu;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import no.uio.kulu.SkeletonImage;

import org.OpenNI.Point3D;

public class Player {


	private Map<ImagePosition, SkeletonImage> imgList = new  LinkedHashMap<ImagePosition, SkeletonImage>();
	private BufferedImage playerImage;
	private static double SCALEDEFAULT = 1300; //At 1000 px distance (z) the scale value = 1 or no scaling

	public enum ImagePosition{
		HEAD,
		LEFT_HAND,
		RIGHT_HAND,
		BODY,
		FEET
	}

	public Player(){
		BufferedImage headMask = loadImage("files/costume/sombrero_small.png");
		BufferedImage leftHand = loadImage("files/costume/gun_small.png");
		BufferedImage rightHand = loadImage("files/costume/taco_small.png");
		BufferedImage body = loadImage("files/costume/poncho_small.png");

		setSkeletonImage(headMask, null, null, new Point3D(0,-60,0),ImagePosition.HEAD, 0);		
		setSkeletonImage(leftHand, null, null, new Point3D(-40,-20,0),ImagePosition.LEFT_HAND, 0);
		setSkeletonImage(rightHand, null, null, new Point3D(0, 0, 0), ImagePosition.RIGHT_HAND, 0);
		setSkeletonImage(body, null, null, new Point3D(0,50,0), ImagePosition.BODY, 0);

	}

	public void setSkeletonImage(BufferedImage image, Point3D pivot, Point3D base, Point3D offset, ImagePosition position, int angle){	
		imgList.put(position, new SkeletonImage(image, pivot, base, offset, angle));	
	}

	public void setSkeletonImage(BufferedImage image, ImagePosition position){	
		SkeletonImage si = imgList.get(position);
		if(si != null)
			si.setImage(image);	
		else
			imgList.put(position, new SkeletonImage(image));
	}

	public void removeSkeletonImageByPosition(ImagePosition position){
		if(position != null)
			imgList.remove(position);
	}

	public void updateSkeletonImagePosition(ImagePosition position, Point3D pivot, Point3D base, int angle){
		if(imgList.get(position) == null){
			setSkeletonImage(null, pivot, base, null, position, angle);
		}else{
			imgList.get(position).setBase(base);
			imgList.get(position).setPivot(pivot);
			imgList.get(position).setAngle(angle);
		}

	}

	public void setPlayerImage(BufferedImage playerImage){
		if(playerImage != null)
			this.playerImage = playerImage;
	}

	public void draw(Graphics2D g2d) {
		if (g2d == null)
			return;

		if(playerImage != null)
			g2d.drawImage(playerImage, 0, 0, null);

		for(ImagePosition position : imgList.keySet()){

			SkeletonImage si = imgList.get(position);
			Point3D pivot = si.getPivot();
			Point3D base = si.getBase();
			Point3D offset = si.getOffset();
			BufferedImage image = si.getImage();

			if (pivot != null && base != null && image != null && offset != null){

				AffineTransform origTF = g2d.getTransform();    // store original orientation
				AffineTransform newTF = (AffineTransform)(origTF.clone());

				int z = (int)(pivot.getZ() + offset.getZ());

				double dx = image.getWidth() * SCALEDEFAULT / z; //Width of scaled image
				double dy = image.getHeight() * SCALEDEFAULT / z; //Height of scaled image
				double sx = dx / image.getWidth(); //Scaling factors
				double sy = dy / image.getHeight();

				if(z > 0){
					//Affinetransform works in oposite order								
					newTF.concatenate(AffineTransform.getTranslateInstance(si.getPivot().getX() + si.getOffset().getX(), si.getPivot().getY() + si.getOffset().getY()));
					newTF.concatenate(AffineTransform.getRotateInstance( Math.toRadians(si.getAngle())));
					newTF.concatenate(AffineTransform.getScaleInstance(sx, sy));
					newTF.concatenate(AffineTransform.getTranslateInstance(-image.getWidth()/2, -image.getHeight()/2));
				}	

				g2d.setTransform(newTF);
				g2d.drawImage(image, 0, 0, null);
				g2d.setTransform(origTF);    // reset original orientation
			}
		}
	} 

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


}
