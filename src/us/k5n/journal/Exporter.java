package us.k5n.journal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import us.k5n.ical.DataStore;
import us.k5n.ical.ICalendarParser;
import us.k5n.ical.Journal;

public class Exporter {
	static File lastExportDirectory;

	protected static void exportAll ( JFrame parent, Repository dataRepository,
	    MessageHandler messageHandler ) {
		export ( parent, "Export All", dataRepository.getAllEntries (),
		    messageHandler );
	}

	protected static void exportVisible ( JFrame parent,
	    Vector<Journal> filteredJournalEntries, MessageHandler messageHandler ) {
		export ( parent, "Export Visible", filteredJournalEntries, messageHandler );
	}

	protected static void exportSelected ( JFrame parent,
	    ReadOnlyTable journalListTable, MessageHandler messageHandler ) {
		Vector<Journal> selected = new Vector<Journal> ();
		int[] sel = journalListTable.getSelectedRows ();
		if ( sel == null || sel.length == 0 ) {
			messageHandler.showError ( "You have not selected any entries" );
			return;
		}
		for ( int i = 0; i < sel.length; i++ ) {
			DisplayDate dd = (DisplayDate) journalListTable.getValueAt ( i, 0 );
			Journal journal = (Journal) dd.getUserData ();
			selected.addElement ( journal );
		}
		export ( parent, "Export Selected", selected, messageHandler );
	}

	private static void export ( JFrame parent, String title,
	    Vector<Journal> journalEntries, MessageHandler messageHandler ) {
		JFileChooser fileChooser;
		File outFile = null;

		if ( lastExportDirectory == null )
			fileChooser = new JFileChooser ();
		else
			fileChooser = new JFileChooser ( lastExportDirectory );
		fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );
		fileChooser.setFileFilter ( new ICSFileChooserFilter () );
		fileChooser.setDialogTitle ( "Select Output File for " + title );
		fileChooser.setApproveButtonText ( "Save as ICS File" );
		fileChooser
		    .setApproveButtonToolTipText ( "Export entries to iCalendar file" );
		int ret = fileChooser.showOpenDialog ( parent );
		if ( ret == JFileChooser.APPROVE_OPTION ) {
			outFile = fileChooser.getSelectedFile ();
		} else {
			// Cancel
			return;
		}
		// If no file extension provided, use ".ics
		String basename = outFile.getName ();
		if ( basename.indexOf ( '.' ) < 0 ) {
			// No filename extension provided, so add ".csv" to it
			outFile = new File ( outFile.getParent (), basename + ".ics" );
		}
		System.out.println ( "Selected File: " + outFile.toString () );
		lastExportDirectory = outFile.getParentFile ();
		if ( outFile.exists () && !outFile.canWrite () ) {
			JOptionPane.showMessageDialog ( parent,
			    "You do not have the proper\npermissions to write to:\n\n"
			        + outFile.toString () + "\n\nPlease select another file.",
			    "Save Error", JOptionPane.PLAIN_MESSAGE );
			return;
		}
		if ( outFile.exists () ) {
			if ( JOptionPane.showConfirmDialog ( parent,
			    "Overwrite existing file?\n\n" + outFile.toString (),
			    "Overwrite Confirm", JOptionPane.YES_NO_OPTION ) != 0 ) {
				JOptionPane.showMessageDialog ( parent, "Export canceled.",
				    "Export canceled", JOptionPane.PLAIN_MESSAGE );
				return;
			}
		}
		try {
			PrintWriter writer = new PrintWriter ( new FileWriter ( outFile ) );
			// Now write!
			ICalendarParser p = new ICalendarParser ( ICalendarParser.PARSE_LOOSE );
			DataStore dataStore = p.getDataStoreAt ( 0 );
			for ( int i = 0; i < journalEntries.size (); i++ ) {
				Journal j = journalEntries.elementAt ( i );
				dataStore.storeJournal ( j );
			}
			writer.write ( p.toICalendar () );
			writer.close ();
			JOptionPane.showMessageDialog ( parent, "Exported to:\n\n"
			    + outFile.toString (), "Export", JOptionPane.PLAIN_MESSAGE );
		} catch ( IOException e ) {
			JOptionPane.showMessageDialog ( parent,
			    "An error was encountered\nwriting to the file:\n\n"
			        + e.getMessage (), "Save Error", JOptionPane.PLAIN_MESSAGE );
			e.printStackTrace ();
		}
	}

}
