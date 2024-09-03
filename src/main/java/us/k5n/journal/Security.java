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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.text.BasicTextEncryptor;

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
	private static Security instance;
	private static File systemPasswordFile;
	private final static String systemPasswordFileName = "security.dat";
	private static File userPasswordDigestFile;
	private final static String userPasswordDigestFileName = "userpassword.dat";
	// This is the user-defined password. It is used to encrypt the password data
	// file that contains the system-generated password.
	private String password = null;
	// This is the system-defined password. On the first run, this password will
	// be generated and stored in the password data file.
	private String key = null;

	private boolean userValidated = false;

	// The default user-defined password which will be used until the user sets
	// the password.
	private final String DEFAULT_USER_PASSWORD = "No user-supplied password yet";

	public Security(File baseDirectory) throws IOException {
		if (instance != null)
			throw new IllegalStateException(
					"Only once instance of Passphrase is allowed");
		instance = this;
		systemPasswordFile = new File(baseDirectory, systemPasswordFileName);
		userPasswordDigestFile = new File(baseDirectory,
				userPasswordDigestFileName);
		if (!keyExists()) {
			userValidated = true;
			initialize();
		} else {
			// auto-validate with default password
			if (!passwordIsCorrect(DEFAULT_USER_PASSWORD)) {
				throw new IllegalStateException(
						"Error authenticating with default password");
			}
		}
	}

	public static Security getInstance() throws IOException {
		if (instance == null)
			throw new IllegalStateException(
					"Cannot invoke getInstance before calling constructor");
		return instance;
	}

	public void setNewPassword(String newPassword) throws IOException {
		if (!userValidated)
			throw new IllegalStateException(
					"Cannot set new password until user is authenticated");
		this.password = newPassword;
		writeUserPasswordDigestFile();
		writePasswordFile();
	}

	/**
	 * Is the user using the default password. This will be true when your app is
	 * first run. Once setNewPassword is called, this will be false.
	 * 
	 * @return
	 */
	public boolean usingDefaultPassword() {
		return password != null && password.equals(DEFAULT_USER_PASSWORD);
	}

	/**
	 * Encrypt some text
	 * 
	 * @param instr
	 *              The text to encrypt
	 * @return the encrypted text
	 */
	public String encrypt(String instr) {
		if (!userValidated)
			throw new IllegalStateException("User has not been validated");
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(key);
		String encryptedText = textEncryptor.encrypt(instr);
		return encryptedText;
	}

	/**
	 * Decrypt some text.
	 * 
	 * @param instr
	 *              The text to descrypt
	 * @return the unencrypted text
	 */
	public String decrypt(String instr) {
		if (!userValidated)
			throw new IllegalStateException("User has not been validated");
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(key);
		String decryptedText = textEncryptor.decrypt(instr);
		return decryptedText;
	}

	public String getEncryptionKey() {
		return key;
	}

	/**
	 * Does the system-generated password exist? Until the first run of the app,
	 * it will not exist.
	 * 
	 * @return
	 */
	private boolean keyExists() {
		return systemPasswordFile.exists();
	}

	private void initialize() throws IOException {
		password = DEFAULT_USER_PASSWORD;

		// Write out the user password digest file. This will initially be based on
		// our default user password.
		writeUserPasswordDigestFile();

		// Generate our system key. This will remain unchanged even if the user
		// changes their password.
		this.key = generateKey();
		// Now encrypt this using the user password (which is currently the default
		// password).
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		// Set password to be user-defined password
		textEncryptor.setPassword(password);

		writePasswordFile();

		userValidated = true;
	}

	/**
	 * Use Java 1.5's UUID generation function to generate a unique system
	 * password.
	 * 
	 * @return
	 */
	private static String generateKey() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Validate the specified user password. Will return true if the password is
	 * correct.
	 * 
	 * @param testPassword
	 * @return true if correct password, false otherwise
	 * @throws IOException
	 */
	public boolean passwordIsCorrect(String testPassword) throws IOException {
		userValidated = false;
		BasicPasswordEncryptor passwordEncryptor = new BasicPasswordEncryptor();
		FileReader fr = new FileReader(userPasswordDigestFile);
		BufferedReader br = new BufferedReader(fr);
		String passwordDigest = br.readLine();
		if (passwordDigest == null)
			throw new NullPointerException("Empty user password digest file");
		br.close();
		fr.close();
		userValidated = passwordEncryptor.checkPassword(testPassword,
				passwordDigest);
		if (userValidated) {
			this.password = testPassword;
			// If correct, then load the key
			readPasswordFile();
		}
		return userValidated;
	}

	/**
	 * Write the current user password as a password digest to our user password
	 * digest file. Note that we do not store the user password in clear text
	 * anywhere, so there is no way to recover a lost password. More importantly,
	 * there is no way for a malicious user to steal your password.
	 * 
	 * @throws IOException
	 */
	private void writeUserPasswordDigestFile() throws IOException {
		BasicPasswordEncryptor passwordEncryptor = new BasicPasswordEncryptor();
		String encryptedPassword = passwordEncryptor.encryptPassword(password);
		FileWriter fw = new FileWriter(userPasswordDigestFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(encryptedPassword + "\n");
		bw.close();
		fw.close();
		System.out.println("Wrote password: " + password);
	}

	/**
	 * Read an encrypted version of the system generated password (this.key).
	 * 
	 * @throws IOException
	 */
	private void readPasswordFile() throws IOException {
		if (!userValidated)
			throw new IllegalStateException("User is not validated");
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(password);

		FileReader fr = new FileReader(systemPasswordFile);
		BufferedReader br = new BufferedReader(fr);
		String encrypted = br.readLine();
		br.close();
		fr.close();

		key = textEncryptor.decrypt(encrypted);
	}

	/**
	 * Write an encrypted version of the system generated password (this.key).
	 * 
	 * @throws IOException
	 */
	private void writePasswordFile() throws IOException {
		if (!userValidated)
			throw new IllegalStateException("User has not been validated");
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(password);
		String encryptedPassword = textEncryptor.encrypt(key);

		FileWriter fw = new FileWriter(systemPasswordFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(encryptedPassword + "\n");
		bw.close();
		fw.close();
	}

}
