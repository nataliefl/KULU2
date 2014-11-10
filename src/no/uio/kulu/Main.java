package no.uio.kulu;

/*
 * Based on work of Andrew Davison, ad@fivedots.psu.ac.th
 */

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.WindowConstants;

import org.OpenNI.Context;
import org.OpenNI.GeneralException;
import org.OpenNI.StatusException;

import clicktracking.no.uio.kulu.GGUIsManager;
import clicktracking.no.uio.kulu.GestureGUI;


public class Main extends JFrame implements Runnable, ComponentListener 
{
	//OpenNI
	private Context context;

	private TrackerPanel trackPanel; 
	private GestureGUI gestureGUI;
	private JLayeredPane container; //Container for overlapping components


	private volatile boolean isRunning;

	//OpenNI
	public Main()
	{
		super("KULU Spillet");		

		setLayout( new BorderLayout() );   

		try {
			context = new Context();
		} catch (GeneralException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		setSize(640,480);
		container = new JLayeredPane();
		
		//Player panel
		trackPanel = new TrackerPanel(context, "files/Dictotor1.jpg");
		gestureGUI = new GestureGUI(context);
		gestureGUI.setBounds(0, 0, 640, 480);
		
		container.add(trackPanel, new Integer(10));
		container.add(gestureGUI, new Integer(20));

		add(container, BorderLayout.CENTER);

		//Listen for keystrokes
		initKeyListener();

		//Exit with 'X'
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		setVisible(true);
		setLocationRelativeTo(null);

		this.addComponentListener(this);
		
		new Thread(this).start();
	} // end of GorillasTracker()

	public void closeDown()
	{  isRunning = false;  } 

	// --------------------Keyboard Listener--------------------

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



	public static void main( String args[] )
	{  new Main();  }

	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			try {
				context.waitAnyUpdateAll();
			}
			catch(StatusException e)
			{  System.out.println(e); 
			System.exit(1);
			}
			trackPanel.run();

		}
		// close down
		try {
			context.stopGeneratingAll();
		}
		catch (StatusException e) {}
		context.release();
		System.exit(0);

	}

	@Override
	public void componentResized(ComponentEvent e) {
		Dimension size = this.getSize();
		trackPanel.setSize(size);
		gestureGUI.setSize(size);
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentShown(ComponentEvent e) {
		Dimension size = this.getSize();
		trackPanel.setSize(size);
		gestureGUI.setSize(size);
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub

	}


} // end of GorillasTracker class
