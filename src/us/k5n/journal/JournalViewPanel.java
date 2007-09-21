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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.MouseInputAdapter;

import org.apache.commons.codec.binary.Base64;

import us.k5n.ical.Attachment;
import us.k5n.ical.Journal;
import us.k5n.ical.Summary;
import us.k5n.ical.Utils;

public class JournalViewPanel extends JPanel {
	private Journal journal;
	private JTabbedPane tabbedPane;
	private JLabel date;
	private JLabel subject;
	private JLabel categories;
	private JTextArea text;

	public JournalViewPanel() {
		super ();

		journal = null;

		setLayout ( new BorderLayout () );

		JPanel topPanel = new JPanel ();
		topPanel.setLayout ( new GridLayout ( 3, 1 ) );
		topPanel.setBorder ( BorderFactory.createEmptyBorder ( 2, 4, 2, 4 ) );

		JPanel subpanel = new JPanel ();
		subpanel.setLayout ( new BorderLayout () );
		subpanel.add ( new JLabel ( "Date: " ), BorderLayout.WEST );
		date = new JLabel ();
		subpanel.add ( date, BorderLayout.CENTER );
		topPanel.add ( subpanel );

		subpanel = new JPanel ();
		subpanel.setLayout ( new BorderLayout () );
		subpanel.add ( new JLabel ( "Subject: " ), BorderLayout.WEST );
		subject = new JLabel ();
		subpanel.add ( subject, BorderLayout.CENTER );
		topPanel.add ( subpanel );

		subpanel = new JPanel ();
		subpanel.setLayout ( new BorderLayout () );
		subpanel.add ( new JLabel ( "Categories: " ), BorderLayout.WEST );
		categories = new JLabel ();
		subpanel.add ( categories, BorderLayout.CENTER );
		topPanel.add ( subpanel );

		add ( topPanel, BorderLayout.NORTH );

		tabbedPane = new JTabbedPane ();

		text = new JTextArea ();
		text.setLineWrap ( true );
		text.setWrapStyleWord ( true );
		text.setEditable ( false );
		JScrollPane scrollPane = new JScrollPane ( text );
		scrollPane
		    .setVerticalScrollBarPolicy ( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );

		add ( scrollPane, BorderLayout.CENTER );
		tabbedPane.addTab ( "Text", null, scrollPane, "Journal Text" );

		add ( tabbedPane, BorderLayout.CENTER );
	}

	public void clear () {
		date.setText ( "" );
		subject.setText ( "" );
		categories.setText ( "" );
		text.setText ( "" );
		for ( int i = tabbedPane.getTabCount (); i > 1; i-- ) {
			tabbedPane.remove ( i - 1 );
		}
	}

	public void setJournal ( Journal j ) {
		this.journal = j;
		if ( j.getStartDate () != null ) {
			DisplayDate d = new DisplayDate ( j.getStartDate () );
			date.setText ( d.toString () );
		} else {
			date.setText ( "None" );
		}
		Summary s = j.getSummary ();
		if ( s != null ) {
			subject.setText ( s.getValue () );
		} else {
			subject.setText ( "(None)" );
		}
		if ( j.getCategories () != null ) {
			categories.setText ( j.getCategories ().getValue () );
		} else {
			categories.setText ( "(None)" );
		}
		if ( j.getDescription () != null ) {
			text.setText ( j.getDescription ().getValue () );
			text.setCaretPosition ( 0 );
		} else {
			text.setText ( "" );
		}
		for ( int i = tabbedPane.getTabCount (); i > 1; i-- ) {
			tabbedPane.remove ( i - 1 );
		}
		Vector attachments = j.getAttachments ();
		for ( int i = 0; attachments != null && i < attachments.size (); i++ ) {
			Attachment a = (Attachment) attachments.elementAt ( i );
			tabbedPane.addTab ( a.getFilename (), null, createAttachmentTab ( a ),
			    "Attachment" );
		}
	}

	protected JPanel createAttachmentTab ( Attachment a ) {
		String filename = a.getFilename ();
		String type = a.getFormatType ();
		JPanel panel = new JPanel ();
		panel.setLayout ( new BorderLayout () );

		if ( type == null && filename != null ) {
			type = Utils.getMimeTypeForExtension ( filename );
		}
		if ( type == null ) {
			panel.add ( new JLabel ( "No viewer (unknown format type)" ) );
		} else if ( type.equalsIgnoreCase ( "text/plain" ) ) {
			String val = a.getValue ();
			byte[] bytes = new byte[val.length ()];
			char[] chars = val.toCharArray ();
			for ( int i = 0; i < chars.length; i++ ) {
				bytes[i] = (byte) chars[i];
			}
			byte[] decoded = Base64.decodeBase64 ( bytes );
			String decStr = new String ( decoded );
			JTextArea text = new JTextArea ( decStr );
			text.setEditable ( false );
			JScrollPane scroll = new JScrollPane ( text );
			panel.add ( scroll, BorderLayout.CENTER );
		} else if ( type.equalsIgnoreCase ( "image/jpeg" )
		    || type.equalsIgnoreCase ( "image/gif" ) ) {
			// Image viewer for JPEG/GIF images
			String val = a.getValue ();
			byte[] bytes = new byte[val.length ()];
			char[] chars = val.toCharArray ();
			for ( int i = 0; i < chars.length; i++ ) {
				bytes[i] = (byte) chars[i];
			}
			byte[] decoded = Base64.decodeBase64 ( bytes );
			IV image = new IV ( decoded );
			JScrollPane scroll = new JScrollPane ( image );
			panel.add ( image.getUIPanel (), BorderLayout.NORTH );
			panel.add ( scroll, BorderLayout.CENTER );
		} else {
			panel.add ( new JLabel ( "No viewer for format " + type ) );
		}

		return panel;
	}
}

class IV extends JPanel {
	BufferedImage image;
	Rectangle r;
	double scale, inc, min;

	public IV(byte[] imageData) {
		loadImage ( imageData );
		scale = 1.0;
		inc = 0.01;
		min = 0.25;
		ImageMover mover = new ImageMover ( this );
		addMouseListener ( mover );
		addMouseMotionListener ( mover );
	}

	protected void paintComponent ( Graphics g ) {
		super.paintComponent ( g );
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint ( RenderingHints.KEY_INTERPOLATION,
		    RenderingHints.VALUE_INTERPOLATION_BICUBIC );
		if ( r == null )
			init ();
		AffineTransform at = AffineTransform.getTranslateInstance ( r.x, r.y );
		at.scale ( scale, scale );
		g2.drawRenderedImage ( image, at );
	}

	private void init () {
		int w = getWidth ();
		int h = getHeight ();
		int imageWidth = image.getWidth ();
		int imageHeight = image.getHeight ();
		r = new Rectangle ( imageWidth, imageHeight );
		r.x = ( w - imageWidth ) / 2;
		r.y = ( h - imageHeight ) / 2;
	}

	private void loadImage ( byte[] imageData ) {
		ByteArrayInputStream stream = new ByteArrayInputStream ( imageData );
		try {
			image = ImageIO.read ( stream );
		} catch ( MalformedURLException mue ) {
			System.err.println ( "url: " + mue.getMessage () );
		} catch ( IOException ioe ) {
			System.err.println ( "read: " + ioe.getMessage () );
		}
	}

	protected JPanel getUIPanel () {
		final JButton larger = new JButton ( "+" ), smaller = new JButton (
		    "-" );
		ActionListener l = new ActionListener () {
			public void actionPerformed ( ActionEvent e ) {
				JButton button = (JButton) e.getSource ();
				if ( button == larger )
					scale += inc;
				if ( button == smaller )
					scale -= scale - inc > min ? inc : 0;
				repaint ();
			}
		};
		larger.addActionListener ( l );
		smaller.addActionListener ( l );
		JPanel panel = new JPanel ();
		panel.add ( larger );
		panel.add ( smaller );
		return panel;
	}
}

class ImageMover extends MouseInputAdapter {
	IV iv;
	Point offset;
	boolean dragging;

	public ImageMover(IV iv) {
		this.iv = iv;
		offset = new Point ();
		dragging = false;
	}

	public void mousePressed ( MouseEvent e ) {
		Point p = e.getPoint ();
		if ( iv.r.contains ( p ) ) {
			offset.x = p.x - iv.r.x;
			offset.y = p.y - iv.r.y;
			dragging = true;
		}
	}

	public void mouseReleased ( MouseEvent e ) {
		dragging = false;
	}

	public void mouseDragged ( MouseEvent e ) {
		if ( dragging ) {
			iv.r.x = e.getX () - offset.x;
			iv.r.y = e.getY () - offset.y;
			iv.repaint ();
		}
	}
}
