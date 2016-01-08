package us.k5n.journal;

/*
 * Copyright (C) 2005-2011 Craig Knudsen
 *
 * k5nJournal is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * A copy of the GNU Lesser General Public License can be found at www.gnu.org. 
 * To receive a hard copy, you can write to:
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA.
 */

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Class for handling popup messages (warning, info, ec.)
 * 
 * @author Craig Knudsen, craig@k5n.us
 * @version $Id: MessageHandler.java,v 1.3 2016-01-08 13:23:53 cknudsen Exp $
 */
public class MessageHandler {
	JFrame parent;

	public MessageHandler(JFrame parent) {
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
				JOptionPane.ERROR_MESSAGE );
		System.exit ( 1 );
	}
}
