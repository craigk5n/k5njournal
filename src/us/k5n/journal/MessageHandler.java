package us.k5n.journal;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class MessageHandler {
	JFrame parent;
	
	public MessageHandler ( JFrame parent )
	{
		this.parent = parent;
	}
	
	void showMessage ( String message ) {
		JOptionPane.showMessageDialog ( parent, message, "Notice",
		    JOptionPane.INFORMATION_MESSAGE );
	}

	void showError ( String message ) {
		System.err.println ( "Error: " + message );
		JOptionPane.showMessageDialog ( parent, message, "Error",
		    JOptionPane.ERROR_MESSAGE );
	}

	void fatalError ( String message ) {
		System.err.println ( "Fatal error: " + message );
		JOptionPane.showMessageDialog ( parent, message, "Fatal Error",
		    JOptionPane.ERROR );
		System.exit ( 1 );
	}
}
