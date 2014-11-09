package no.uio.kulu;

import java.awt.image.BufferedImage;

import org.OpenNI.Point3D;

/**
 * Object with an image, name and position properties. Used to draw images on given positions with the 
 * OpenNI Skeleton framework.
 * @author Magnus Lien
 */
public class SkeletonImage {

	private BufferedImage image;
	private Point3D pivot;
	private Point3D base;
	private Point3D offset;
	private int angle;
	
	
	/**
	 * 
	 * Create a new SkeletonImage
	 * 
	 * @param image Image to store.
	 * @param pivot Center point of the image. Additionally the image will pivot around this joint when the player moves.
	 * @param base Base joint for calculating pivot angle.
	 * @return Returns a SkeletonImage object.
	 */
	public SkeletonImage (BufferedImage image, Point3D pivot, Point3D base, Point3D offset, int angle){
		this.image = image;
		this.pivot = pivot;
		this.base = base;
		this.angle = angle;
		this.offset = offset;
	}
	
	public SkeletonImage (BufferedImage image){
		this.image = image;
		this.pivot = null;
		this.base = null;
		this.angle = 0;
		this.offset = null;
	}

	public Point3D getOffset() {
		return offset;
	}

	public void setOffset(Point3D offset) {
		this.offset = offset;
	}
	
	public BufferedImage getImage() {
		return image;
	}


	public void setImage(BufferedImage image) {
		this.image = image;
	}


	public Point3D getPivot() {
		return pivot;
	}


	public void setPivot(Point3D pivot) {
		this.pivot = pivot;
	}


	public Point3D getBase() {
		return base;
	}


	public void setBase(Point3D base) {
		this.base = base;
	}


	public int getAngle() {
		return angle;
	}


	public void setAngle(int angle) {
		this.angle = angle;
	}
	
	
}
