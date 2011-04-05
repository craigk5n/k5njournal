package us.k5n.journal;

import java.io.File;
import java.io.IOException;

import org.jasypt.util.text.BasicTextEncryptor;

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

/**
 * The Security object holds the passphrase used to encode and decode all the
 * data files. We implement encryption by using two passwords. One use a
 * system-generated password to encrypt all the data files. We then store that
 * password in a data file that is encrypted with the user-specified password.
 * This allows the user to change their password without having to decrypt and
 * re-encrypt every data file.
 */
public class Security {
	// TODO: just getting started here.... nowhere near complete.
	private static Security instance = null;
	private static File datafile = null;
	private static String filename = "security.dat";
	private String password = null;

	public Security(File baseDirectory) {
		if ( instance != null )
			throw new IllegalStateException (
			    "Only once instance of Passphrase is allowed" );
		instance = this;
	}

	public static Security getInstance ( File baseDirectory ) {
		if ( instance == null )
			instance = new Security ( baseDirectory );
		return instance;
	}

	public void setNewPassphrase ( String newPassphrase ) {
	}

	public String getEncryptionKey () {
		return null;
	}

	public boolean passwordMatches ( String password ) {
		return true;
	}

	private void read () throws IOException {
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor ();
		textEncryptor.setPassword ( password );
	}

	private void write () throws IOException {
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor ();
		textEncryptor.setPassword ( password );
	}
}
