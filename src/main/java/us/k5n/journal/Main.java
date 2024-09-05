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
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import us.k5n.ical.Categories;
import us.k5n.ical.Constants;
import us.k5n.ical.Description;
import us.k5n.ical.Journal;
import us.k5n.ical.Summary;

/**
 * Main class for k5njournal application. This application makes use of the k5n
 * iCalendar library (part of Java Calendar Tools). Its primary use is as a
 * personal diary. However, future plans include the ability to post entries to
 * blog sites using the APIs for Blogger, MetaWeblog and Moveable Type.
 * 
 * @author Craig Knudsen, craig@k5n.us
 */
public class Main extends JFrame implements Constants, ComponentListener,
		PropertyChangeListener, RepositoryChangeListener, PasswordAcceptedListener {
	private static final long serialVersionUID = 1L;
	public static final String DEFAULT_DIR_NAME = "k5njournal";
	public static final String VERSION = "0.3.3 (02 Sep 2024)";

	JFrame parent;
	final MessageHandler messageHandler;
	JLabel messageArea;
	Security security;
	Repository dataRepository;
	JTree dateTree;
	DefaultMutableTreeNode dateTreeAllNode;
	ReadOnlyTable journalListTable;
	ReadOnlyTabelModel journalListTableModel;
	ImageIcon clipIcon = null;
	JournalViewPanel journalView = null;
	// filteredJournalEntries is the List of Journal objects filtered
	// by dates selected by the user. (Not yet filtered by search text.)
	List<Journal> filteredJournalEntries;
	// filteredSearchedJournalEntries is filtered by both date selection
	// and text search.
	List<Journal> filteredSearchedJournalEntries;
	final static String[] journalListTableHeader = { "", "Date", "Subject" };
	final static String[] monthNames = { "", "January", "February", "March",
			"April", "May", "June", "July", "August", "September", "October",
			"November", "December" };
	JButton newButton, editButton, deleteButton;
	JMenuItem exportSelected;
	JTextField searchTextField;
	JSplitPane verticalSplit = null, horizontalSplit = null;
	String searchText = null;
	AppPreferences prefs;

	class DateFilterTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 1L;
		public int year, month, day;
		public String label;

		public DateFilterTreeNode(String l, int y, int m, int d, int count) {
			super(l);
			this.year = y;
			this.month = m;
			this.day = d;
			this.label = l + " (" + count + ")";
		}

		public String toString() {
			return label;
		}
	}

	public Main() {
		super("k5njournal");
		setWindowsLAF();
		this.parent = this;

		// TODO: save user's preferred size on exit and set here
		prefs = AppPreferences.getInstance();

		setSize(prefs.getMainWindowWidth(), prefs.getMainWindowHeight());
		this.setLocation(prefs.getMainWindowX(), prefs.getMainWindowY());

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container contentPane = getContentPane();

		this.messageHandler = new MessageHandler(this);

		// Setup security (encryption/decryption, etc.)
		try {
			this.security = new Security(getDataDirectory());
		} catch (IOException e) {
			System.err.println("Error initializing password system: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		/*
		 * // Load data dataRepository = new Repository ( getDataDirectory (), false
		 * ); // Ask to be notified when the repository changes (user adds/edits //
		 * an entry) dataRepository.addChangeListener ( this );
		 */

		// Create a menu bar
		setJMenuBar(createMenu());

		contentPane.setLayout(new BorderLayout());

		// Add message/status bar at bottom
		JPanel messagePanel = new JPanel();
		messagePanel.setLayout(new BorderLayout());
		messagePanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		messageArea = new JLabel("Welcome to k5njournal...");
		messagePanel.add(messageArea, BorderLayout.CENTER);
		contentPane.add(messagePanel, BorderLayout.SOUTH);

		contentPane.add(createToolBar(), BorderLayout.NORTH);

		JPanel navArea = createJournalSelectionPanel();
		journalView = new JournalViewPanel();

		verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, navArea,
				journalView);
		verticalSplit.setOneTouchExpandable(true);
		verticalSplit.setResizeWeight(0.5);
		verticalSplit.setDividerLocation(prefs
				.getMainWindowVerticalSplitPosition());
		verticalSplit.addPropertyChangeListener(this);
		// verticalSplit.addComponentListener ( this );
		contentPane.add(verticalSplit, BorderLayout.CENTER);

		this.addComponentListener(this);
		this.setVisible(true);

		// See if user is still using default password.
		if (security.usingDefaultPassword()) {
			// Load data now
			loadData();
			// warn user about default password.
			messageHandler.showMessage("You are using the default password.\n"
					+ "If you would like to secure your journal entries,\n"
					+ "then you should set a new password.\n"
					+ "To do so, select \"Change Password\" from the\nFile menu.");
		} else {
			// Data will be loaded after password is entered.
			promptForPassword();
		}
	}

	private void promptForPassword() {
		boolean done = false;
		while (!done) {
			final JPasswordField jpf = new JPasswordField();
			JOptionPane jop = new JOptionPane(jpf, JOptionPane.QUESTION_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION);
			JDialog dialog = jop.createDialog("Password:");
			/*
			 * dialog.addComponentListener ( new ComponentAdapter () {
			 * 
			 * @Override public void componentShown ( ComponentEvent e ) {
			 * SwingUtilities.invokeLater ( new Runnable () { public void run () {
			 * jpf.requestFocusInWindow (); } } ); } } );
			 */
			dialog.setVisible(true);
			int result = (Integer) jop.getValue();
			dialog.dispose();
			String password = null;
			if (result == JOptionPane.OK_OPTION) {
				password = new String(jpf.getPassword());
			} else {
				System.exit(0);
			}
			try {
				if (security.passwordIsCorrect(password)) {
					System.out.println("Password correct!");
					done = true;
				} else {
					messageHandler.showError("Invalid password");
					System.out.println("Password incorrect");
				}
			} catch (IOException e) {
				messageHandler.showError("Error checking password: " + e);
				e.printStackTrace();
			}
		}
		loadData();
	}

	// Load data here once the user has entered a password.
	public void loadData() {
		// Load data
		dataRepository = new Repository(getDataDirectory(), false);
		// Ask to be notified when the repository changes (user adds/edits
		// an entry)
		dataRepository.addChangeListener(this);
		// Populate Date JTree
		updateDateTree();
		handleDateFilterSelection(0, null);
		// filteredJournalEntries = dataRepository.getAllEntries ();
		// updateFilteredJournalList ();
	}

	JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar();
		newButton = makeNavigationButton("new.png", "new",
				"Add new Journal entry", "New...");
		newButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new EditWindow(parent, dataRepository, null);
			}
		});
		toolbar.add(newButton);

		editButton = makeNavigationButton("edit.png", "edit",
				"Edit Journal entry", "Edit...");
		toolbar.add(editButton);
		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// Get selected item and open edit window
				int ind = journalListTable.getSelectedRow();
				if (ind >= 0 && filteredSearchedJournalEntries != null
						&& ind < filteredSearchedJournalEntries.size()) {
					DisplayDate dd = (DisplayDate) journalListTable.getValueAt(ind, 1);
					Journal j = (Journal) dd.getUserData();
					new EditWindow(parent, dataRepository, j);
				}
			}
		});

		deleteButton = makeNavigationButton("delete.png", "delete",
				"Delete Journal entry", "Delete");
		toolbar.add(deleteButton);
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// Get selected item and open edit window
				int ind = journalListTable.getSelectedRow();
				if (ind >= 0 && filteredSearchedJournalEntries != null
						&& ind < filteredSearchedJournalEntries.size()) {
					Journal j = filteredSearchedJournalEntries.get(ind);
					if (JOptionPane.showConfirmDialog(parent,
							"Are you sure you want\nto delete this entry?", "Confirm Delete",
							JOptionPane.YES_NO_OPTION) == 0) {
						try {
							dataRepository.deleteJournal(j);
						} catch (IOException e1) {
							messageHandler.showError("Error deleting.");
							e1.printStackTrace();
						}
					}
				} else {
					System.err.println("Index out of range: " + ind);
				}
			}
		});

		return toolbar;
	}

	void updateToolbar(int numSelected) {
		editButton.setEnabled(numSelected == 1);
		deleteButton.setEnabled(numSelected == 1);
		exportSelected.setEnabled(numSelected >= 1);
	}

	public void setMessage(String msg) {
		this.messageArea.setText(msg);
	}

	public JMenuBar createMenu() {
		JMenuItem item;

		JMenuBar bar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");

		JMenu exportMenu = new JMenu("Export");
		// exportMenu.setMnemonic ( 'X' );
		fileMenu.add(exportMenu);

		item = new JMenuItem("All");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Exporter.exportAll(parent, dataRepository, messageHandler);
			}
		});
		exportMenu.add(item);
		item = new JMenuItem("Visible");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Exporter
						.exportVisible(parent, filteredJournalEntries, messageHandler);
			}
		});
		exportMenu.add(item);
		item = new JMenuItem("Selected");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Exporter.exportSelected(parent, journalListTable, messageHandler);
			}
		});
		exportMenu.add(item);
		exportSelected = item;

		fileMenu.addSeparator();

		item = new JMenuItem("Change Password");
		item.setAccelerator(KeyStroke.getKeyStroke('P', Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				changePassword();
			}
		});
		fileMenu.add(item);

		fileMenu.addSeparator();

		item = new JMenuItem("Exit");
		item.setAccelerator(KeyStroke.getKeyStroke('Q', Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// TODO: check for unsaved changes
				// TODO: save current size of main window for use next time
				System.exit(0);
			}
		});
		fileMenu.add(item);

		bar.add(fileMenu);

		/*
		 * commented out because of bug in JDK that causes NullPointerException when
		 * we update the UI L&F. JMenu settingsMenu = new JMenu ( "Settings" );
		 * 
		 * item = new JMenuItem ( "Look & Feel" ); item.addActionListener ( new
		 * ActionListener () { public void actionPerformed ( ActionEvent event ) {
		 * selectLookAndFeel ( parent, parent ); } } ); settingsMenu.add ( item );
		 * bar.add ( settingsMenu );
		 */

		// Add help bar to right end of menubar
		bar.add(Box.createHorizontalGlue());

		JMenu helpMenu = new JMenu("Help");

		item = new JMenuItem("About...");
		item.setAccelerator(KeyStroke.getKeyStroke('A', Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// TODO: add logo, etc...
				JOptionPane.showMessageDialog(parent, "k5njournal\nVersion "
						+ VERSION + "\n\nDeveloped by k5n.us\n\n"
						+ "Go to www.k5n.us for more info.");
			}
		});
		helpMenu.add(item);

		bar.add(helpMenu);

		return bar;
	}

	/**
	 * Create the file selection area on the top side of the window. This will
	 * include a split pane where the left will allow navigation and selection of
	 * dates and the right will allow the selection of a specific entry.
	 * 
	 * @return
	 */
	protected JPanel createJournalSelectionPanel() {
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());

		JTabbedPane tabbedPane = new JTabbedPane();

		JPanel byDate = new JPanel();
		byDate.setLayout(new BorderLayout());
		tabbedPane.addTab("Date", byDate);
		dateTreeAllNode = new DefaultMutableTreeNode("All");
		dateTree = new JTree(dateTreeAllNode);

		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = dateTree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = dateTree.getPathForLocation(e.getX(), e.getY());
				if (selRow != -1) {
					if (e.getClickCount() == 1) {
						handleDateFilterSelection(selRow, selPath);
					} else if (e.getClickCount() == 2) {
						// Do something for double-click???
					}
				}
			}
		};
		dateTree.addMouseListener(ml);

		JScrollPane scrollPane = new JScrollPane(dateTree);
		byDate.add(scrollPane, BorderLayout.CENTER);

		// TODO: add category tab filter
		// JPanel byCategory = new JPanel ();
		// tabbedPane.addTab ( "Category", byCategory );

		JPanel journalListPane = new JPanel();
		journalListPane.setLayout(new BorderLayout());

		JPanel searchPanel = new JPanel();
		searchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		searchPanel.setLayout(new BorderLayout());
		URL imageURL = getImage("search.png");
		ImageIcon icon = imageURL == null ? null : new ImageIcon(imageURL);
		JLabel searchLabel = new JLabel();
		searchLabel.setIcon(icon);
		searchPanel.add(searchLabel, BorderLayout.WEST);
		searchTextField = new SearchTextField();
		searchTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				searchUpdated();
			}
		});
		searchPanel.add(searchTextField, BorderLayout.CENTER);
		journalListPane.add(searchPanel, BorderLayout.NORTH);

		journalListTableModel = new ReadOnlyTabelModel(journalListTableHeader, 0,
				3);

		TableSorter sorter = new TableSorter(journalListTableModel);
		journalListTable = new ReadOnlyTable(sorter);
		sorter.setTableHeader(journalListTable.getTableHeader());
		// journalListTable.setAutoResizeMode ( JTable.AUTO_RESIZE_OFF );
		journalListTable.setRowSelectionAllowed(true);
		TableColumn tc = journalListTable.getColumnModel().getColumn(0);
		tc.setWidth(15);
		journalListTable.setColumnFixedWidth(0, 20);

		imageURL = getImage("clip.png");
		clipIcon = new ImageIcon(imageURL);

		// Set the text and icon values on the second column for the icon render
		// journalListTable.getColumnModel ().getColumn ( 0 ).setHeaderValue (
		// "Attachments" );
		journalListTable.getColumnModel().getColumn(0).setHeaderValue(" ");
		journalListTable.getColumnModel().getColumn(0)
				.setCellRenderer(new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;

					public Component getTableCellRendererComponent(JTable tblDataTable,
							Object value, boolean isSelected, boolean hasFocus,
							int markedRow, int col) {
						JLabel ret = (JLabel) super.getTableCellRendererComponent(
								tblDataTable, value, isSelected, hasFocus, markedRow, col);
						if (value instanceof Integer
								&& ((Integer) value).intValue() > 0) {
							ret.setIcon(clipIcon);
							// ret.setText ( null );
							ret.setHorizontalTextPosition(JLabel.LEFT);
						} else {
							ret.setIcon(null);
						}
						ret.setText(null);
						ret.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
						return ret;
					}
				});

		// Add selection listener to table
		journalListTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent event) {
						if (journalView != null) {
							int ind = journalListTable.getSelectedRow();
							int numSel = journalListTable.getSelectedRowCount();
							updateToolbar(numSel);
							if (numSel == 0) {
								journalView.clear();
							} else if (!event.getValueIsAdjusting() && ind >= 0
									&& filteredSearchedJournalEntries != null
									&& ind < filteredSearchedJournalEntries.size()) {
								int[] selRows = journalListTable.getSelectedRows();
								// The call below might actually belong in ReadOnlyTable.
								// However, we would need to add a MouseListener to
								// ReadOnlyTable
								// and make sure that one got called before this one.
								journalListTable.setHighlightedRows(selRows);
								if (selRows != null && selRows.length == 1) {
									DisplayDate dd = (DisplayDate) journalListTable.getValueAt(
											ind, 1);
									Journal journal = (Journal) dd.getUserData();
									if (journal != null)
										journalView.setJournal(journal);
								} else {
									// more than one selected
									journalView.clear();
								}
							} else {
								journalListTable.clearHighlightedRows();
							}
						}
					}
				});

		JScrollPane journalListTableScroll = new JScrollPane(journalListTable);
		journalListPane.add(journalListTableScroll, BorderLayout.CENTER);

		horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane,
				journalListPane);
		horizontalSplit.setOneTouchExpandable(true);
		horizontalSplit.setDividerLocation(prefs
				.getMainWindowHorizontalSplitPosition());
		// horizontalSplit.addComponentListener ( this );
		horizontalSplit.addPropertyChangeListener(this);

		topPanel.add(horizontalSplit, BorderLayout.CENTER);

		return topPanel;
	}

	void handleDateFilterSelection(int row, TreePath path) {
		int year = -1;
		int month = -1;
		if (path == null || path.getPathCount() < 2) {
			// "All"
		} else {
			DateFilterTreeNode dateFilter = (DateFilterTreeNode) path
					.getLastPathComponent();
			// System.out.println ( "Showing entries for " + dateFilter.year + "/"
			// + dateFilter.month );
			year = dateFilter.year;
			if (dateFilter.month > 0)
				month = dateFilter.month;
		}
		if (year < 0) {
			filteredJournalEntries = dataRepository.getAllEntries();
		} else if (month < 0) {
			filteredJournalEntries = dataRepository.getEntriesByYear(year);
		} else {
			filteredJournalEntries = dataRepository.getEntriesByMonth(year, month);
		}
		this.updateFilteredJournalList();
	}

	// Rebuild the Date JTree.
	// TODO: What we should really be doing is updating the JTree so that
	// we can preserve what year nodes were open and what objects were
	// selected.
	void updateDateTree() {
		dateTree.setShowsRootHandles(true);
		// Remove all old entries
		dateTreeAllNode.removeAllChildren();
		// Get entries, starting with years
		int[] years = dataRepository.getYears();
		if (years != null) {
			for (int i = years.length - 1; i >= 0; i--) {
				List<Journal> yearEntries = dataRepository
						.getEntriesByYear(years[i]);
				DateFilterTreeNode yearNode = new DateFilterTreeNode("" + years[i],
						years[i], 0, 0, yearEntries == null ? 0 : yearEntries.size());
				dateTreeAllNode.add(yearNode);
				int[] months = dataRepository.getMonthsForYear(years[i]);
				for (int j = 0; months != null && j < months.length; j++) {
					List<Journal> monthEntries = dataRepository.getEntriesByMonth(
							years[i], months[j]);
					DateFilterTreeNode monthNode = new DateFilterTreeNode(
							monthNames[months[j]], years[i], months[j], 0,
							monthEntries == null ? 0 : monthEntries.size());
					yearNode.add(monthNode);
				}
			}
		}
		DefaultTreeModel dtm = (DefaultTreeModel) dateTree.getModel();
		dtm.nodeStructureChanged(dateTreeAllNode);
		dateTree.expandRow(0);
		// Select "All" by default
		dateTree.setSelectionRow(0);
		dateTree.repaint();
		updateToolbar(0);
	}

	/**
	 * User pressed the Enter key in the search text.
	 */
	void searchUpdated() {
		searchText = searchTextField.getText();
		if (searchText != null && searchText.trim().length() == 0)
			searchText = null;
		updateFilteredJournalList();
	}

	// Filter the specified List of Journal objects by
	// the searchText using a regular expression.
	private List<Journal> filterSearchText(List<Journal> entries) {
		List<Journal> ret;
		Pattern pat;
		Matcher m;

		if (searchText == null || searchText.trim().length() == 0)
			return entries;

		// remove any characters that are not regular expression safe
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < searchText.length(); i++) {
			char ch = searchText.charAt(i);
			if (ch >= 'a' || ch <= 'Z' || ch >= 'A' || ch <= 'Z' || ch >= '0'
					|| ch <= '9' || ch == ' ') {
				sb.append(ch);
			}
		}
		if (sb.length() == 0)
			return entries;

		ret = new ArrayList<Journal>();
		pat = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
		// System.out.println ( "Pattern: " + pat );
		for (int i = 0; i < entries.size(); i++) {
			Journal j = entries.get(i);
			Description d = j.getDescription();
			boolean matches = false;
			// Search summary, categories, and description
			Summary summary = j.getSummary();
			if (summary != null) {
				m = pat.matcher(summary.getValue());
				if (m.find()) {
					matches = true;
				}
			}
			if (!matches) {
				Categories cats = j.getCategories();
				if (cats != null) {
					m = pat.matcher(cats.getValue());
					if (m.find()) {
						matches = true;
					}
				}
			}
			if (!matches) {
				if (d != null) {
					m = pat.matcher(d.getValue());
					if (m.find()) {
						matches = true;
					}
				}
			}
			if (matches) {
				ret.add(j);
			}
		}
		return ret;
	}

	/**
	 * Update the JTable of Journal entries based on the Journal objects in the
	 * filteredJournalEntries List.
	 */
	void updateFilteredJournalList() {
		filteredSearchedJournalEntries = filterSearchText(filteredJournalEntries);
		// Sort by date...
		filteredSearchedJournalEntries = SortableJournal
				.sortJournals(filteredSearchedJournalEntries);
		journalListTableModel
				.setRowCount(filteredSearchedJournalEntries == null ? 0
						: filteredSearchedJournalEntries.size());
		journalListTable.clearHighlightedRows();
		for (int i = 0; filteredSearchedJournalEntries != null
				&& i < filteredSearchedJournalEntries.size(); i++) {
			Journal entry = filteredSearchedJournalEntries.get(i);
			// Set attachment count
			if (entry.getAttachments() != null
					&& entry.getAttachments().size() > 0)
				journalListTable.setValueAt(new Integer(entry.getAttachments()
						.size()), i, 0);
			else
				journalListTable.setValueAt(new Integer(0), i, 0);
			if (entry.getStartDate() != null) {
				journalListTable.setValueAt(new DisplayDate(entry.getStartDate(),
						entry), i, 1);
			} else {
				journalListTable.setValueAt(new DisplayDate(null, entry), i, 1);
			}
			Summary summary = entry.getSummary();
			journalListTable.setValueAt(
					summary == null ? "-" : summary.getValue(), i, 2);
		}
		this.showStatusMessage(""
				+ (filteredSearchedJournalEntries == null ? "No"
						: ""
								+ filteredSearchedJournalEntries.size())
				+ " entries "
				+ (searchText == null ? "" : "matched '" + searchText + "'"));

		journalListTable.repaint();
	}

	/**
	 * Get the data directory that data files for this application will be stored
	 * in.
	 * 
	 * @return
	 */
	// TODO: allow user preferences to override this setting
	File getDataDirectory() {
		String s = (String) System.getProperty("user.home");
		if (s == null) {
			System.err.println("Could not find user.home setting.");
			System.err.println("Using current directory instead.");
			s = ".";
		}
		File f = new File(s);
		if (f == null)
			messageHandler.fatalError("Invalid user.home value '" + s + "'");
		if (!f.exists())
			messageHandler.fatalError("Home directory '" + f + "' does not exist.");
		if (!f.isDirectory())
			messageHandler.fatalError("Home directory '" + f
					+ "'is not a directory");
		// Use the home directory as the base. Data files will
		// be stored in a subdirectory.
		File dir = new File(f, DEFAULT_DIR_NAME);
		if (!dir.exists()) {
			if (!dir.mkdirs())
				messageHandler.fatalError("Unable to create data directory: " + dir);
			messageHandler.showMessage("The following directory was created\n"
					+ "to store data files:\n\n" + dir);
		}
		if (!dir.isDirectory())
			messageHandler.fatalError("Not a directory: " + dir);
		return dir;
	}

	void showStatusMessage(String string) {
		this.messageArea.setText(string);
	}

	private URL getImage(String imageName) {
		URL ret = null;
		if (imageName != null) {
			ret = this.getClass().getClassLoader()
					.getResource("images/" + imageName);
		}
		if (ret == null) {
			// try without "images/" (which might happen when running inside an IDE
			// like Eclipse)
			ret = this.getClass().getClassLoader().getResource(imageName);
		}
		return ret;
	}

	protected JButton makeNavigationButton(String imageName,
			String actionCommand, String toolTipText, String altText) {
		JButton button;
		String imgLocation = null;
		URL imageURL = null;

		imageURL = getImage(imageName);

		if (imageURL != null) { // image found
			button = new JButton(altText);
			button.setIcon(new ImageIcon(imageURL, altText));
		} else {
			// no image found
			button = new JButton(altText);
			if (imageName != null)
				System.err.println("Resource not found: " + imgLocation);
		}

		button.setVerticalTextPosition(JButton.BOTTOM);
		button.setHorizontalTextPosition(JButton.CENTER);
		button.setActionCommand(actionCommand);
		button.setToolTipText(toolTipText);

		// Decrease font size by 2 if we have an icon
		if (imageURL != null) {
			Font f = button.getFont();
			Font newFont = new Font(f.getFamily(), Font.PLAIN, f.getSize() - 2);
			button.setFont(newFont);
		}

		return button;
	}

	/**
	 * Set the Look and Feel to be Windows.
	 */
	public static void setWindowsLAF() {
		try {
			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			} else {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (Exception e) {
			System.out.println("Unable to load Windows UI: " + e.toString());
		}
	}

	public void selectLookAndFeel(Component toplevel, Frame dialogParent) {
		LookAndFeel lafCurrent = UIManager.getLookAndFeel();
		System.out.println("Current L&F: " + lafCurrent);
		UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
		String[] choices = new String[info.length];
		int sel = 0;
		for (int i = 0; i < info.length; i++) {
			System.out.println("  " + info[i].toString());
			choices[i] = info[i].getClassName();
			if (info[i].getClassName().equals(lafCurrent.getClass().getName()))
				sel = i;
		}
		Object uiSelection = JOptionPane.showInputDialog(dialogParent,
				"Select Look and Feel", "Look and Feel",
				JOptionPane.INFORMATION_MESSAGE, null, choices, choices[sel]);
		UIManager.LookAndFeelInfo selectedLAFInfo = null;
		for (int i = 0; i < info.length; i++) {
			if (uiSelection.equals(choices[i]))
				selectedLAFInfo = info[i];
		}
		if (selectedLAFInfo != null) {
			try {
				System.out.println("Changing L&F: " + selectedLAFInfo);
				UIManager.setLookAndFeel(selectedLAFInfo.getClassName());
				// SwingUtilities.updateComponentTreeUI ( parent );
				// parent.pack ();
			} catch (Exception e) {
				System.err.println("Unabled to load L&F: " + e.toString());
			}
		} else {
			System.err.println("No L&F selected");
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

	public void propertyChange(PropertyChangeEvent pce) {
		// System.out.println ( "property Change: " + pce );
		if (pce.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
			saveWindowPreferences();
		}
	}

	/**
	 * Save current window width, height so we can restore on next run.
	 */
	public void saveWindowPreferences() {
		prefs.setMainWindowX(this.getX());
		prefs.setMainWindowY(this.getY());
		prefs.setMainWindowWidth(this.getWidth());
		prefs.setMainWindowHeight(this.getHeight());
		prefs.setMainWindowVerticalSplitPosition(verticalSplit
				.getDividerLocation());
		prefs.setMainWindowHorizontalSplitPosition(horizontalSplit
				.getDividerLocation());
	}

	public void journalAdded(Journal journal) {
		this.updateDateTree();
		handleDateFilterSelection(0, null);
	}

	public void journalUpdated(Journal journal) {
		this.updateDateTree();
		handleDateFilterSelection(0, null);
	}

	public void journalDeleted(Journal journal) {
		this.updateDateTree();
		handleDateFilterSelection(0, null);
	}

	void changePassword() {
		boolean done = false;

		while (!done) {
			JPasswordField password1 = new JPasswordField();
			JPasswordField password2 = new JPasswordField();

			final JComponent[] inputs = new JComponent[] { new JLabel("Password"),
					password1, new JLabel("Password (again)"), password2 };
			JOptionPane.showMessageDialog(this, inputs, "Change Password",
					JOptionPane.PLAIN_MESSAGE);
			String p1 = new String(password1.getPassword());
			String p2 = new String(password2.getPassword());
			if (p1 == null || p1.length() == 0) {
				// cancel
				done = true;
			} else if (!p1.equals(p2)) {
				messageHandler.showError("Passwords do not match.");
			} else {
				done = true;
				try {
					security.setNewPassword(p1);
				} catch (IOException e) {
					messageHandler.showError("Error saving new password:\n" + e);
					e.printStackTrace();
				}
				// System.out.println ( "new password: " + p1 );
			}
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Main();
	}

}

/**
 * Create a class to use as a file filter for exporting to ics.
 */

class ICSFileChooserFilter extends javax.swing.filechooser.FileFilter {
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		String name = f.getName();
		if (name.toLowerCase().endsWith(".ics"))
			return true;
		return false;
	}

	public String getDescription() {
		return "*.ics (iCalendar Files)";
	}
}

class SortableJournal implements Comparable {
	Journal journal;

	public SortableJournal(Journal j) {
		this.journal = j;
	}

	public int compareTo(Object o) {
		SortableJournal j = (SortableJournal) o;
		return j.journal.getDtstamp().compareTo(this.journal.getDtstamp());
	}

	public static List<Journal> sortJournals(List<Journal> journals) {
		List<SortableJournal> sjs = new ArrayList<SortableJournal>();
		for (int i = 0; journals != null && i < journals.size(); i++) {
			Journal j = journals.get(i);
			SortableJournal sj = new SortableJournal(j);
			sjs.add(sj);
		}
		Collections.sort(sjs);
		List<Journal> ret = new ArrayList<Journal>();
		for (int i = 0; i < sjs.size(); i++) {
			SortableJournal sj = sjs.get(i);
			ret.add(sj.journal);
		}
		return ret;

	}
}
