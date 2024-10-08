/*
 * Copyright (C) 2005-2024 Craig Knudsen
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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import us.k5n.ical.Attachment;
import us.k5n.ical.ParseException;

/**
 * Present a dialog window that allows the user to add and remove attachments.
 * The simplest way to use this class is with the static
 * <tt>showAttachmentDialog</tt> method.
 * 
 * TODO: use a table to display file size.
 * 
 * @author Craig Knudsen, craig@k5n.us
 */
public class AttachmentDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	JList list;
	List<Attachment> attachments;
	boolean userAccepted = false;
	JFrame parent;
	private static File lastDirectory = null;
	static ImageIcon okIcon, cancelIcon;

	/**
	 * Show the AttachmentDialog window that allows the user to modify the list of
	 * attachments
	 * 
	 * @param parent
	 * @param attachments
	 *                    Current attachments (or null if none)
	 * @return A List of attachments. This can be size 0. If the user pressed
	 *         "Cancel", then null will be returned.
	 */
	public static List<Attachment> showAttachmentDialog(JFrame parent,
			List<Attachment> attachments) {
		AttachmentDialog ad = new AttachmentDialog(parent, attachments);
		List<Attachment> ret = ad.userAccepted ? ad.attachments : null;
		return ret;
	}

	public AttachmentDialog(JFrame parent, List<Attachment> attachments) {
		super((JFrame) null);
		this.parent = parent;
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setModal(true);
		this.attachments = cloneAttachments(attachments);
		this.getContentPane().setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		JButton cancelButton = new JButton("Cancel");
		if (cancelIcon == null) {
			URL imageURL = this.getClass().getClassLoader().getResource(
					"images/cancel.png");
			if (imageURL == null) {
				System.err.println("Error: could not find cancel.png file");
			} else {
				cancelIcon = new ImageIcon(imageURL);
			}
		}
		if (cancelIcon != null)
			cancelButton.setIcon(cancelIcon);
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				cancel();
			}
		});
		buttonPanel.add(cancelButton);

		JButton okButton = new JButton("Ok");
		if (okIcon == null) {
			URL imageURL = this.getClass().getClassLoader().getResource(
					"images/ok.png");
			if (imageURL == null) {
				System.err.println("Error: could not find ok.png file");
			} else {
				okIcon = new ImageIcon(imageURL);
			}
		}
		if (okIcon != null)
			okButton.setIcon(okIcon);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ok();
			}
		});
		buttonPanel.add(okButton);

		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		JPanel topPanel = createAttachmentSelection();

		this.getContentPane().add(topPanel, BorderLayout.CENTER);

		this.pack();
		this.setVisible(true);
	}

	private List<Attachment> cloneAttachments(List<Attachment> attachments) {
		return (List<Attachment>) (attachments == null ? new ArrayList<Attachment>()
				: new ArrayList<Attachment>(attachments));
	}

	private JPanel createAttachmentSelection() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEtchedBorder());
		p.setLayout(new BorderLayout());

		JPanel addRemovePanel = new JPanel();
		addRemovePanel.setLayout(new FlowLayout());

		JButton addButton = new JButton("Add...");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				addFile();
			}
		});
		addRemovePanel.add(addButton);

		JButton removeButton = new JButton("Remove");
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				removeFile();
			}
		});
		addRemovePanel.add(removeButton);

		p.add(addRemovePanel, BorderLayout.SOUTH);

		this.list = new JList();
		rebuildList();
		this.list.setVisibleRowCount(5);
		this.setPreferredSize(new Dimension(300, 200));
		p.add(this.list, BorderLayout.CENTER);

		p.setBorder(BorderFactory.createEtchedBorder());

		return p;
	}

	private void rebuildList() {
		List<String> items = new ArrayList<String>();
		for (int i = 0; this.attachments != null && i < this.attachments.size(); i++) {
			Attachment a = this.attachments.get(i);
			String filename = a.getFilename();
			items.add(filename == null ? "Unnamed-" + (i + 1) : filename);
		}
		this.list.setListData(items.toArray());
	}

	protected void addFile() {
		JFileChooser fileChooser;
		File file = null;

		if (lastDirectory == null)
			fileChooser = new JFileChooser();
		else
			fileChooser = new JFileChooser(lastDirectory);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setDialogTitle("Select File to Attach");
		fileChooser.setApproveButtonText("Attach");
		fileChooser.setApproveButtonToolTipText("Attach selected file");
		int ret = fileChooser.showOpenDialog(this);
		if (ret == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
		} else {
			// Cancel
			return;
		}
		lastDirectory = file.getParentFile();
		if (!file.exists()) {
			JOptionPane.showMessageDialog(parent, "No such file:\n\n"
					+ file.toString() + "\n\nPlease select a file.", "Save Error",
					JOptionPane.PLAIN_MESSAGE);
			return;
		}
		try {
			Attachment a = new Attachment(file);
			this.attachments.add(a);
			rebuildList();
			System.out.println("File attached: " + file + ", " + a.getFilename());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(parent, "Error attaching file:\n\n"
					+ file.toString() + "\n\nPException:\n\n" + e.toString(),
					"Save Error", JOptionPane.PLAIN_MESSAGE);
			e.printStackTrace();
			return;
		} catch (ParseException e2) {
			e2.printStackTrace();
		}

	}

	// remove selected attachments
	protected void removeFile() {
		int[] sel = this.list.getSelectedIndices();
		if (sel != null && sel.length > 0) {
			// remove from List, starting at end so indices don't change
			// while we are deleting.
			for (int i = sel.length - 1; i >= 0; i--) {
				this.attachments.remove(sel[i]);
			}
			this.rebuildList();
		}
	}

	protected void cancel() {
		this.dispose();
	}

	protected void ok() {
		this.userAccepted = true;
		this.dispose();
	}
}
