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

import us.k5n.ical.Journal;

/**
 * Interface for receiving updates from Repository.
 * 
 * @author Craig Knudsen, craig@k5n.us
 */
public interface RepositoryChangeListener {

	public abstract void journalAdded ( Journal journal );

	public abstract void journalUpdated ( Journal journal );

	public abstract void journalDeleted ( Journal journal );
}
