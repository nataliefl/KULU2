package clicktracking.no.uio.kulu;


// CameraPanel.java
// Andrew Davison, December 2011, ad@fivedots.psu.ac.th

/* Render the latest Kinect camera image, scaled so that it spans the
   screen's width, and write a message at the top-left of the screen.

   The message can be changed by calling setMessage().
*/

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.swing.JPanel;

import org.OpenNI.GeneralException;
import org.OpenNI.ImageGenerator;



public class CameraPanel extends JPanel
{
  // image vars
  private BufferedImage image = null;
  private int imWidth, imHeight;
  private int scaledWidth, scaledHeight;

  // for messages
  private Font font;
  private String message = null;


  public CameraPanel(int scrWidth, int scrHeight, 
                             int iw, int ih, double scaleFactor)
  {
    imWidth = iw; imHeight = ih;

    setBounds(0, 0, scrWidth, scrHeight);
    setPreferredSize( new Dimension(scrWidth, scrHeight));

    // set up message font
    font = new Font("SansSerif", Font.BOLD, 36);
    message = "Click/Wave to start.";

    // calculate scaled image dimensions, so image width spans the screen
    scaledWidth = scrWidth;
    scaledHeight = (int) (imHeight * scaleFactor);    
           // this may mean some of the bottom of the image is lost
  } // end of CameraPanel()



  public void setMessage(String msg)
  {  message = msg;  }



  public void update(ImageGenerator imageGen)
  // get the latest Kinect camera image, and redraw
  {
    try {
      ByteBuffer imageBB = imageGen.getImageMap().createByteBuffer();
      image = bufToImage(imageBB);
      repaint();
    }
    catch (GeneralException e) {
      System.out.println(e);
    }

  }  // end of update()




  private BufferedImage bufToImage(ByteBuffer pixelsRGB)
  /* Transform the ByteBuffer of pixel data into a BufferedImage
     Converts RGB bytes to ARGB ints with no transparency.  */
  {
    int[] pixelInts = new int[imWidth * imHeight];
 
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
	      pixelInts[i++] = 
	         0xFF000000 | ((pixR & 0xFF) << 16) | 
	         ((pixG & 0xFF) << 8) | (pixB & 0xFF);
	    }
      rowStart += rowLen;   // move to next row
    }

    // create a BufferedImage from the pixel data
    BufferedImage im = 
	     new BufferedImage( imWidth, imHeight, BufferedImage.TYPE_INT_ARGB);
    im.setRGB( 0, 0, imWidth, imHeight, pixelInts, 0, imWidth );
    return im;
  }  // end of bufToImage()




  public void paintComponent(Graphics g)
  // draw the scaled camera image and the message at the top-left
  { 
    super.paintComponent(g);
    if (image != null)
      g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);

    // draw message
    g.setFont(font);
    g.setColor(Color.YELLOW);
    if (message != null)
      g.drawString(message, 10, 35);    // top-left
  } // end of paintComponent()


} // end of CameraPanel class

