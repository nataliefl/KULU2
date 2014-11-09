package no.uio.kulu;

/*
 * Based on work of Andrew Davison, ad@fivedots.psu.ac.th
 */

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;


public class Main extends JFrame 
{
	private TrackerPanel trackPanel; 


	public Main()
	{
		super("KULU Spillet");

		Container c = getContentPane();
		c.setLayout( new BorderLayout() );   

		trackPanel = new TrackerPanel("files/Dictotor1.jpg");
		c.add( trackPanel, BorderLayout.CENTER);

		addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{ trackPanel.closeDown();  }
		});

		pack();  
		setLocationRelativeTo(null);
		setVisible(true);
		//    asetExtendedState(this.MAXIMIZED_BOTH);  
		// setUndecorated(true);

	} // end of GorillasTracker()


	// -------------------------------------------------------

	public static void main( String args[] )
	{  new Main();  }

} // end of GorillasTracker class
