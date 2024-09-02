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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import us.k5n.ical.Attachment;
import us.k5n.ical.Categories;
import us.k5n.ical.Date;
import us.k5n.ical.Description;
import us.k5n.ical.Journal;
import us.k5n.ical.Sequence;
import us.k5n.ical.Summary;

/**
 * Create a Journal entry edit window.
 * 
 * @author Craig Knudsen, craig@k5n.us
 */
public class EditWindow extends JDialog implements ComponentListener {
	private static final long serialVersionUID = 1L;
	Repository repo;
	Journal journal;
	Sequence seq = null;
	JFrame parent;
	JTextField subject;
	JTextField categories;
	JTextField attachmentsText;
	List<Attachment> attachments;
	JLabel startDate;
	JTextArea description;
	AppPreferences prefs;
	static ImageIcon saveIcon, cancelIcon;

	public EditWindow(JFrame parent, Repository repo, Journal journal) {
		super(parent);
		prefs = AppPreferences.getInstance();
		super.setSize(prefs.getEditWindowWidth(), prefs.getEditWindowHeight());
		super.setLocation(prefs.getEditWindowX(), prefs.getEditWindowY());
		// TODO: don't make this modal once we add code to check
		// things like deleting this entry in the main window, etc.
		// super.setModal ( true );
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		this.parent = parent;
		this.repo = repo;
		this.journal = journal;

		if (this.journal == null) {
			this.journal = new Journal("", "", Date.getCurrentDateTime("DTSTART"));
		} else {
			// Create an updated sequence number for use only if we save
			// (So don't put it in the original Journal object yet)
			if (this.journal.getSequence() == null)
				seq = new Sequence(1);
			else
				seq = new Sequence(this.journal.getSequence().getNum() + 1);
		}
		// Make sure there is a Summary and Description
		if (this.journal.getSummary() == null)
			this.journal.setSummary(new Summary());
		if (this.journal.getDescription() == null)
			this.journal.setDescription(new Description());
		if (this.journal.getCategories() == null)
			this.journal.setCategories(new Categories());

		if (getAttachments() != null)
			this.attachments = getAttachments();

		createWindow();
		setVisible(true);
		this.addComponentListener(this);
	}

	private List<Attachment> getAttachments() {
		return this.journal.getAttachments();
	}

	private void createWindow() {
		this.getContentPane().setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		JButton saveButton = new JButton("Save");
		if (saveIcon == null) {
			URL imageURL = this.getClass().getClassLoader().getResource(
					"images/save.png");
			if (imageURL == null) {
				System.err.println("Error: could not find save.png file");
			} else {
				saveIcon = new ImageIcon(imageURL);
			}
		}
		if (saveIcon != null)
			saveButton.setIcon(saveIcon);
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// Save (write file)
				save();
			}
		});
		buttonPanel.add(saveButton);
		JButton closeButton = new JButton("Close");
		if (cancelIcon == null) {
			URL imageURL = this.getClass().getClassLoader().getResource(
					"images/cancel.png");
			if (imageURL == null) {
				System.err.println("Error: could not find cancel file");
			} else {
				cancelIcon = new ImageIcon(imageURL);
			}
		}
		if (cancelIcon != null)
			closeButton.setIcon(cancelIcon);
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				close();
			}
		});
		buttonPanel.add(closeButton);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		JPanel allButButtons = new JPanel();
		allButButtons.setLayout(new BorderLayout());
		allButButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JPanel upperPanel = new JPanel();
		upperPanel.setBorder(BorderFactory.createEtchedBorder());
		int[] vproportions = { 25, 25, 25, 25 };
		upperPanel.setLayout(new ProportionalLayout(vproportions,
				ProportionalLayout.VERTICAL_LAYOUT));

		int[] proportions = { 20, 80 };

		JPanel subjectPanel = new JPanel();
		subjectPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		subjectPanel.setLayout(new ProportionalLayout(proportions,
				ProportionalLayout.HORIZONTAL_LAYOUT));
		JLabel prompt = new JLabel("Subject: ");
		prompt.setHorizontalAlignment(SwingConstants.RIGHT);
		subjectPanel.add(prompt);
		subject = new JTextField();
		if (journal != null && journal.getSummary() != null)
			subject.setText(journal.getSummary().getValue());
		subjectPanel.add(subject);
		upperPanel.add(subjectPanel);

		JPanel datePanel = new JPanel();
		datePanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		datePanel.setLayout(new ProportionalLayout(proportions,
				ProportionalLayout.HORIZONTAL_LAYOUT));
		prompt = new JLabel("Date: ");
		prompt.setHorizontalAlignment(SwingConstants.RIGHT);
		datePanel.add(prompt);
		JPanel subDatePanel = new JPanel();
		FlowLayout flow = new FlowLayout();
		flow.setAlignment(FlowLayout.LEFT);
		subDatePanel.setLayout(flow);
		startDate = new JLabel();
		DisplayDate d = new DisplayDate(journal.getStartDate());
		startDate.setText(d.toString());
		subDatePanel.add(startDate);
		JButton dateSel = new JButton("...");
		dateSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Date newDate = DateTimeSelectionDialog.showDateTimeSelectionDialog(
						parent, journal.getStartDate());
				if (newDate != null) {
					journal.setStartDate(newDate);
					DisplayDate d = new DisplayDate(journal.getStartDate());
					startDate.setText(d.toString());
				}
			}
		});
		subDatePanel.add(dateSel);
		datePanel.add(subDatePanel);
		upperPanel.add(datePanel);

		JPanel catPanel = new JPanel();
		catPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		catPanel.setLayout(new ProportionalLayout(proportions,
				ProportionalLayout.HORIZONTAL_LAYOUT));
		prompt = new JLabel("Categories: ");
		prompt.setHorizontalAlignment(SwingConstants.RIGHT);
		catPanel.add(prompt);
		categories = new JTextField();
		if (journal != null && journal.getCategories() != null)
			categories.setText(journal.getCategories().getValue());
		catPanel.add(categories);
		upperPanel.add(catPanel);

		JPanel attachmentPanel = new JPanel();
		attachmentPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		attachmentPanel.setLayout(new ProportionalLayout(proportions,
				ProportionalLayout.HORIZONTAL_LAYOUT));
		prompt = new JLabel("Attachments: ");
		prompt.setHorizontalAlignment(SwingConstants.RIGHT);
		attachmentPanel.add(prompt);
		JPanel subAttachmentPanel = new JPanel();
		subAttachmentPanel.setLayout(new BorderLayout());
		JPanel subsub = new JPanel();
		subsub.setLayout(new FlowLayout());
		JButton attachmentSel = new JButton("...");
		attachmentSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				chooseAttachments();
			}
		});
		subsub.add(attachmentSel);
		subAttachmentPanel.add(subsub, BorderLayout.EAST);
		attachmentsText = new JTextField();
		attachmentsText.setEditable(false);
		attachmentsText.setText(getAttachmentsLabel(this.attachments));
		subAttachmentPanel.add(attachmentsText, BorderLayout.CENTER);
		attachmentPanel.add(subAttachmentPanel);
		upperPanel.add(attachmentPanel);

		allButButtons.add(upperPanel, BorderLayout.NORTH);

		// TODO: eventually add some edit buttons/icons here when
		// we support more than plain text.
		JPanel descrPanel = new JPanel();
		descrPanel.setLayout(new BorderLayout());
		description = new JTextArea();
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		if (journal != null && journal.getDescription() != null)
			description.setText(journal.getDescription().getValue());
		description.setCaretPosition(0);
		JScrollPane scrollPane = new JScrollPane(description);
		descrPanel.add(scrollPane, BorderLayout.CENTER);
		allButButtons.add(descrPanel, BorderLayout.CENTER);

		getContentPane().add(allButButtons, BorderLayout.CENTER);
	}

	String getAttachmentsLabel(List<Attachment> list) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; list != null && i < list.size(); i++) {
			Attachment a = list.get(i);
			String filename = a.getFilename();
			if (i > 0)
				sb.append(", ");
			sb.append(filename == null ? "Unnamed-" + (i + 1) : filename);
		}
		return sb.toString();
	}

	void save() {
		// Note: LAST-MODIFIED gets updated by call to saveJournal
		if (seq != null) {
			journal.setSequence(seq);
			seq = null;
		}
		try {
			this.journal.getDescription().setValue(description.getText());
			this.journal.getSummary().setValue(subject.getText().trim());
			this.journal.getCategories().setValue(categories.getText().trim());
			this.journal.setAttachments(this.attachments);
			repo.saveJournal(this.journal);
		} catch (IOException e2) {
			// TODO: add error handler that pops up a window here
			e2.printStackTrace();
		}
		this.dispose();
	}

	void chooseAttachments() {
		List<Attachment> newAttachments = AttachmentDialog.showAttachmentDialog(
				parent, this.attachments);
		if (newAttachments != null) {
			this.attachments = newAttachments;
			// update display to show new attachments
			attachmentsText.setText(getAttachmentsLabel(this.attachments));
		}
	}

	void close() {
		// TODO: check for unsaved changes
		this.dispose();
	}

	public void componentHidden(ComponentEvent ce) {
	}

	public void componentShown(ComponentEvent ce) {
	}

	// Handle moving of main window
	public void componentMoved(ComponentEvent ce) {
		saveWindowPreferences();
	}

	public void componentResized(ComponentEvent ce) {
		saveWindowPreferences();
	}

	/**
	 * Save current window width, height so we can restore on next run.
	 */
	public void saveWindowPreferences() {
		prefs.setEditWindowWidth(this.getWidth());
		prefs.setEditWindowHeight(this.getHeight());
		prefs.setEditWindowX(this.getX());
		prefs.setEditWindowY(this.getY());
	}

}
