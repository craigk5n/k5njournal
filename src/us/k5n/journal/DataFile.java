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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import org.jasypt.util.text.BasicTextEncryptor;

import us.k5n.ical.Constants;
import us.k5n.ical.DataStore;
import us.k5n.ical.ICalendarParser;
import us.k5n.ical.Journal;
import us.k5n.ical.ParseError;
import us.k5n.ical.ParseErrorListener;

/**
 * Extend the File class to include iCalendar data created from parsing the
 * file. Normally, the application will just store a single Journal entry in
 * each file. However, if a user copies an ICS file into their directory, we
 * don't want to loose track of the original filename to avoid creating
 * duplicates.
 * 
 * @author Craig Knudsen, craig@k5n.us
 * @version $Id: DataFile.java,v 1.9 2011-04-10 03:09:18 cknudsen Exp $
 */
public class DataFile extends File implements Constants, ParseErrorListener {
	private static final long serialVersionUID = 1L;
	ICalendarParser parser;
	DataStore dataStore;
	private boolean isEncrypted = false;

	// public DataFile(String filename) {
	// this ( filename, false, false );
	// }

	/**
	 * Create a DataFile object. If the specified filename exists, then it will be
	 * parsed and all entries loaded into the default DataStore. If the filename
	 * does not exists, then no parsing/loading will take place.
	 * 
	 * @param filename
	 *          The filename (YYYYMMDD.ics as in "19991231.ics")
	 * @param strictParsing
	 */
	public DataFile(String filename, boolean strictParsing, boolean encrypted) {
		super ( filename );
		this.isEncrypted = encrypted;
		parser = new ICalendarParser ( strictParsing ? PARSE_STRICT : PARSE_LOOSE );
		parser.addParseErrorListener ( this );
		if ( this.exists () ) {
			if ( encrypted ) {
				StringBuilder sb = new StringBuilder ( (int) this.length () );
				try {
					// Read encrypted text into StringBuffer. Create StringReader from
					// input to mimic file reading since ICalParser wants a java.io.Reader
					// object.
					BufferedReader reader = new BufferedReader ( new FileReader ( this ) );
					String line;
					while ( ( line = reader.readLine () ) != null ) {
						sb.append ( line );
						sb.append ( "\n" );
					}
					String encryptedStr = sb.toString ();
					String decryptedStr = Security.getInstance ().decrypt ( encryptedStr );
					StringReader sr = new StringReader ( decryptedStr );
					parser.parse ( sr );
					reader.close ();
					sr.close ();
				} catch ( IOException e ) {
					System.err.println ( "Error opening " + toString () + ": " + e );
					e.printStackTrace ();
				}
			} else {
				BufferedReader reader = null;
				try {
					reader = new BufferedReader ( new FileReader ( this ) );
					parser.parse ( reader );
					reader.close ();
				} catch ( IOException e ) {
					System.err.println ( "Error opening " + toString () + ": " + e );
					e.printStackTrace ();
				}
			}
		}
		dataStore = parser.getDataStoreAt ( 0 );
		// Store this DataFile object in the user data object of each
		// Journal entry so we can get back to this object if the user
		// edits and saves a Journal entry.
		for ( int i = 0; i < getJournalCount (); i++ ) {
			Journal j = journalEntryAt ( i );
			j.setUserData ( this );
		}
	}

	public void addJournal ( Journal journal ) {
		journal.setUserData ( this );
		dataStore.storeJournal ( journal );
	}

	private DataFile(ICalendarParser parser, String filename) {
		super ( filename );
		dataStore = parser.getDataStoreAt ( 0 );
		// Store this DataFile object in the user data object of each
		// Journal entry so we can get back to this object if the user
		// edits and saves a Journal entry.
		for ( int i = 0; i < getJournalCount (); i++ ) {
			Journal j = journalEntryAt ( i );
			j.setUserData ( this );
		}
	}

	/**
	 * Return the number of journal entries in this file.
	 * 
	 * @return
	 */
	public int getJournalCount () {
		return dataStore.getAllJournals ().size ();
	}

	/**
	 * Get the Journal entry at the specified location.
	 * 
	 * @param ind
	 *          The index number (0 is first)
	 * @return
	 */
	public Journal journalEntryAt ( int ind ) {
		return (Journal) dataStore.getAllJournals ().elementAt ( ind );
	}

	/**
	 * Remove the Journal object at the specified location in the Vector of
	 * entries.
	 * 
	 * @param ind
	 * @return true if found and deleted
	 */
	public boolean removeJournal ( Journal journal ) {
		return dataStore.getAllJournals ().remove ( journal );
	}

	/**
	 * Get the number of parse errors found in the file.
	 * 
	 * @return
	 */
	public int getParseErrorCount () {
		return parser.getAllErrors ().size ();
	}

	/**
	 * Get the parse error at the specified location
	 * 
	 * @param ind
	 * @return
	 */
	public ParseError getParseErrorAt ( int ind ) {
		return (ParseError) parser.getAllErrors ().elementAt ( ind );
	}

	/**
	 * Write this DataFile object.
	 * 
	 * @throws IOException
	 */
	public void write () throws IOException {
		if ( !isEncrypted ) {
			FileWriter writer = null;
			writer = new FileWriter ( this );
			writer.write ( parser.toICalendar () );
			writer.close ();
		} else {
			// Now write encrypted file
			BasicTextEncryptor textEncryptor = new BasicTextEncryptor ();
			textEncryptor.setPassword ( Security.getInstance ().getEncryptionKey () );

			File encFile = null;
			if ( this.toString ().endsWith ( ".enc" ) )
				encFile = this;
			else
				encFile = new File ( this + ".enc" );
			// System.out.println ( "Writing file: " + encFile.getAbsolutePath () );
			FileWriter ewriter = new FileWriter ( encFile );
			ewriter.write ( textEncryptor.encrypt ( parser.toICalendar () ) );
			ewriter.close ();
		}
	}

	public void reportParseError ( ParseError error ) {
		System.err.println ( "ICalendar Parse Error: line no. " + error.lineNo
		    + ", data=" + error.inputData );
	}
}
