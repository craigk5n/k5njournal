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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.BadLocationException;

import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

import us.k5n.ical.Attachment;
import us.k5n.ical.Categories;
import us.k5n.ical.Date;
import us.k5n.ical.Description;
import us.k5n.ical.Journal;
import us.k5n.ical.ParseException;
import us.k5n.ical.Sequence;
import us.k5n.ical.Summary;
import us.k5n.ical.Uid;

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
	private String originalDescriptionText;
	AppPreferences prefs;
	static ImageIcon saveIcon, cancelIcon;
	private JLanguageTool langTool;
	private Map<Integer, RuleMatch> highlightMap = new HashMap<>();
	private Timer spellCheckTimer; // Timer for periodic spell checking
	private boolean textChanged = false; // Flag to track if text has changed
	RuleMatch currentMatch;
	private int lastEditStartPos = -1; // Start position of the last edit
	private int lastEditEndPos = -1; // End position of the last edit
	private long lastEditTime = -1; // Timestamp of the last edit
	private Timer editTimer; // Timer to track 5 seconds since last edit

	/**
	 * Constructs an EditWindow dialog for editing a Journal entry.
	 *
	 * @param parent  the parent JFrame for this dialog
	 * @param repo    the Repository instance
	 * @param journal the Journal instance to edit, or null to create a new one
	 */
	public EditWindow(JFrame parent, Repository repo, Journal journal) {
		super(parent);
		prefs = AppPreferences.getInstance();
		super.setSize(prefs.getEditWindowWidth(), prefs.getEditWindowHeight());
		super.setLocation(prefs.getEditWindowX(), prefs.getEditWindowY());
		// Create LanguageTool instance for US English
		langTool = new JLanguageTool(new AmericanEnglish());
		// TODO: don't make this modal once we add code to check
		// things like deleting this entry in the main window, etc.
		// super.setModal ( true );
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		this.parent = parent;
		this.repo = repo;
		this.journal = journal;

		// Check the spelling every 2 seconds. Any more than that can make the UI
		// feel a bit sluggish.
		spellCheckTimer = new Timer(2000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (textChanged) {
					checkSpelling();
					textChanged = false;
				}
			}
		});
		spellCheckTimer.start();

		editTimer = new Timer(5000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Reset the last edit positions after 5 seconds of inactivity
				lastEditStartPos = -1;
				lastEditEndPos = -1;
				lastEditTime = -1;
			}
		});
		editTimer.setRepeats(false); // We only want it to trigger once per period of inactivity
		editTimer.start();

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
		if (this.journal.getUid() == null) {
			try {
				journal.setUid(new Uid(UIDGenerator.generateVJournalUID()));
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
		}

		if (getAttachments() != null)
			this.attachments = getAttachments();
		originalDescriptionText = this.journal.getDescription() != null ? this.journal.getDescription().getValue() : "";

		createWindow();
		setVisible(true);
		checkSpelling();
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
		// Add a DocumentListener to track text changes
		description.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateLastEditPosition(e);
				textChanged = true;
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateLastEditPosition(e);
				textChanged = true;
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				textChanged = true;
			}

			private void updateLastEditPosition(DocumentEvent e) {
				// Store the positions of the edit for later reference
				lastEditStartPos = e.getOffset();
				lastEditEndPos = e.getOffset() + e.getLength();
				lastEditTime = System.currentTimeMillis(); // Store the current timestamp of the edit
				editTimer.restart(); // Reset the 5-second timer for this edit
			}
		});
		description.addMouseMotionListener(new MouseInputAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int pos = description.viewToModel2D(e.getPoint());
				if (isPositionInHighlight(pos)) {
					String tooltipText = getTooltipTextForPosition(pos);
					description.setToolTipText(tooltipText); // Set the tooltip with suggestions
				} else {
					description.setToolTipText(null); // No tooltip if not hovering over a highlight
				}
			}
		});
		description.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)
						|| (SwingUtilities.isLeftMouseButton(e) && e.isControlDown())) {
					int pos = description.viewToModel2D(e.getPoint());
					if (isPositionInHighlight(pos)) {
						showPopupMenu(e, pos); // Show popup menu with suggestions
					}
				}
			}
		});
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		if (journal != null && journal.getDescription() != null)
			description.setText(journal.getDescription().getValue());
		description.setCaretPosition(0);
		JScrollPane scrollPane = new JScrollPane(description);
		descrPanel.add(scrollPane, BorderLayout.CENTER);
		description.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO: implement
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}
		});
		// Caret listener to track cursor position
		description.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				int caretPosition = e.getDot();
				if (isPositionInHighlight(caretPosition)) {
					showSuggestionsForPosition(caretPosition);
				}
			}
		});
		description.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				int caretPosition = e.getDot();
				if (caretPosition < lastEditStartPos || caretPosition > lastEditEndPos) {
					// If the caret has moved outside of the edited word, reset the last edit
					// tracking
					lastEditStartPos = -1;
					lastEditEndPos = -1;
				}
			}
		});

		allButButtons.add(descrPanel, BorderLayout.CENTER);

		getContentPane().add(allButButtons, BorderLayout.CENTER);
	}

	private String getTooltipTextForPosition(int position) {
		if (currentMatch != null) {
			StringBuilder tooltip = new StringBuilder();
			// Add the message for the rule (usually a description of the issue)
			tooltip.append(currentMatch.getMessage());
			// If there are suggested replacements, append them to the tooltip
			List<String> suggestions = currentMatch.getSuggestedReplacements();
			if (!suggestions.isEmpty()) {
				tooltip.append(" Suggestions: ");
				tooltip.append(String.join(", ", suggestions));
			}
			return tooltip.toString();
		}
		return null;
	}

	private boolean isPositionInHighlight(int position) {
		// Iterate through the highlight map to see if the position falls within any
		// highlight range
		for (Map.Entry<Integer, RuleMatch> entry : highlightMap.entrySet()) {
			int startPos = entry.getKey();
			RuleMatch match = entry.getValue();
			int endPos = match.getToPos();

			if (position >= startPos && position <= endPos) {
				currentMatch = match; // Store the current match for suggestions
				// System.out.println("Position " + position + " is in highlight.");
				// System.out.println("Current match: " + currentMatch);
				return true;
			}
		}
		// System.out.println("Position " + position + " is not in highlight.");
		return false;
	}

	private void showSuggestionsForPosition(int position) {
		if (currentMatch != null) {
			List<String> suggestions = currentMatch.getSuggestedReplacements();
			if (!suggestions.isEmpty()) {
				// Show suggestions as tooltips or handle them as needed
				description.setToolTipText(String.join(", ", suggestions));
			}
		}
	}

	private void showPopupMenu(MouseEvent e, int position) {
		if (currentMatch != null) {
			List<String> suggestions = currentMatch.getSuggestedReplacements();
			if (!suggestions.isEmpty()) {
				JPopupMenu popupMenu = new JPopupMenu();
				for (String suggestion : suggestions) {
					JMenuItem menuItem = new JMenuItem(suggestion);
					menuItem.addActionListener(event -> replaceText(position, suggestion)); // Replace text on click
					popupMenu.add(menuItem);
				}
				popupMenu.show(description, e.getX(), e.getY()); // Show the popup menu at the mouse click position
			}
		}
	}

	private void replaceText(int position, String replacement) {
		try {
			// Replace the misspelled word with the selected suggestion
			int startPos = currentMatch.getFromPos();
			int endPos = currentMatch.getToPos();
			description.getDocument().remove(startPos, endPos - startPos);
			description.getDocument().insertString(startPos, replacement, null);
			// Clear the highlight after replacement
			description.getHighlighter().removeAllHighlights();
			checkSpelling(); // Re-check spelling after the replacement
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
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

	private void checkSpelling() {
		try {
			String text = description.getText();
			description.getHighlighter().removeAllHighlights();
			highlightMap.clear();
			List<RuleMatch> matches = langTool.check(text);
			for (RuleMatch match : matches) {
				int startPos = match.getFromPos();
				int endPos = match.getToPos();

				// Skip this word if it overlaps with the last edit position and the edit was
				// within 1 second
				if ((startPos >= lastEditStartPos && startPos <= lastEditEndPos) ||
						(endPos >= lastEditStartPos && endPos <= lastEditEndPos)) {
					// Only skip if the last edit was less than 5 seconds ago
					if (System.currentTimeMillis() - lastEditTime < 5000) {
						continue;
					}
				}

				// Highlight the misspelled word in pink
				description.getHighlighter().addHighlight(startPos, endPos,
						new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.PINK));
				// Store the RuleMatch for future reference (tooltip, replacement suggestions)
				highlightMap.put(startPos, match);
			}
		} catch (IOException | javax.swing.text.BadLocationException ex) {
			ex.printStackTrace();
		}
	}

	void close() {
		String currentDescriptionText = description.getText().trim();
		boolean hasUnsavedChanges = !currentDescriptionText.equals(originalDescriptionText);
		if (hasUnsavedChanges) {
			int option = JOptionPane.showConfirmDialog(
					this,
					"There are unsaved changes. Do you want to save before closing?",
					"Unsaved Changes",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);

			if (option == JOptionPane.YES_OPTION) {
				save();
			} else if (option == JOptionPane.NO_OPTION) {
				dispose();
			} else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
				return;
			}
		} else {
			dispose();
		}
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
