/*
 * Copyright (C) 2005-2007 Craig Knudsen
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

package us.k5n.journal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import us.k5n.ical.Attachment;

public class AttachmentDialog extends JDialog {
	JList list;
	Vector attachments;
	boolean userAccepted = false;
	private static File lastDirectory = null;

	public static Vector showAttachmentDialog ( JFrame parent, Vector attachments ) {
		AttachmentDialog ad = new AttachmentDialog ( parent, attachments );
		Vector ret = ad.userAccepted ? ad.attachments : null;
		return ret;
	}

	public AttachmentDialog(JFrame parent, Vector attachments) {
		super ( (JFrame) null );
		setDefaultCloseOperation ( JDialog.DISPOSE_ON_CLOSE );
		this.setModal ( true );
		this.attachments = attachments == null ? new Vector ()
		    : (Vector) attachments.clone ();
		this.getContentPane ().setLayout ( new BorderLayout () );
		JPanel buttonPanel = new JPanel ();
		buttonPanel.setLayout ( new FlowLayout () );
		JButton cancelButton = new JButton ( "Cancel" );
		cancelButton.addActionListener ( new ActionListener () {
			public void actionPerformed ( ActionEvent event ) {
				cancel ();
			}
		} );
		buttonPanel.add ( cancelButton );

		JButton okButton = new JButton ( "Ok" );
		okButton.addActionListener ( new ActionListener () {
			public void actionPerformed ( ActionEvent event ) {
				ok ();
			}
		} );
		buttonPanel.add ( okButton );

		this.getContentPane ().add ( buttonPanel, BorderLayout.SOUTH );

		JPanel topPanel = createAttachmentSelection ();

		this.getContentPane ().add ( topPanel, BorderLayout.CENTER );

		this.pack ();
		this.setVisible ( true );
	}

	private JPanel createAttachmentSelection () {
		JPanel p = new JPanel ();
		p.setBorder ( BorderFactory.createEtchedBorder () );
		p.setLayout ( new BorderLayout () );
		
		JPanel addRemovePanel = new JPanel ();
		addRemovePanel.setLayout ( new FlowLayout () );
		
		JButton addButton = new JButton ( "Add..." );
		addRemovePanel.add ( addButton );
		
		JButton removeButton = new JButton ( "Remove" );
		addRemovePanel.add ( removeButton );
		
		p.add ( addRemovePanel, BorderLayout.SOUTH );
		
		Vector items = new Vector ();
		for ( int i = 0; this.attachments != null && i < this.attachments.size (); i++ ) {
			Attachment a = (Attachment) this.attachments.elementAt ( i );
			String filename = a.getFilename ();
			items.addElement ( filename == null ? "Unnamed-" + ( i + 1 ) : filename );
		}
		this.list = new JList ( items );
		this.list.setVisibleRowCount ( 5 );
		this.setPreferredSize ( new Dimension ( 300, 200 ) );
		p.add ( this.list, BorderLayout.CENTER );

		p.setBorder ( BorderFactory.createEtchedBorder () );

		return p;
	}
	
	protected void addFile ()
	{
		JFileChooser fileChooser;
		File file = null;

		if ( lastDirectory == null )
			fileChooser = new JFileChooser ();
		else
			fileChooser = new JFileChooser ( lastDirectory );
		fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );
		fileChooser.setFileFilter ( new ICSFileChooserFilter () );
		fileChooser.setDialogTitle ( "Select File to Attach" );
		fileChooser.setApproveButtonText ( "Attach" );
		fileChooser
		    .setApproveButtonToolTipText ( "Attach selected file" );
		int ret = fileChooser.showOpenDialog ( this );
		if ( ret == JFileChooser.APPROVE_OPTION ) {
			file = fileChooser.getSelectedFile ();
		} else {
			// Cancel
			return;
		}
		System.out.println ( "Selected File: " + file.toString () );
		lastDirectory = file.getParentFile ();
		if ( ! file.exists ()  ) {
			JOptionPane.showMessageDialog ( parent,
			    "No such file:\n\n"
			        + file.toString () + "\n\nPlease select a file.",
			    "Save Error", JOptionPane.PLAIN_MESSAGE );
			return;
		}
		Attachment a = new Attachment ( file );
		this.attachments.addElement ( a );
		
	}

	protected void cancel () {
		this.dispose ();
	}

	protected void ok () {
		this.userAccepted = true;
		this.dispose ();
	}
}
