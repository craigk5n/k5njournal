                           k5nJournal
****************************************************************************

Version:      0.3.0
URL:          http://k5njournal.sourceforge.net
Author:       Craig Knudsen, craig [< at >] k5n.us
License:      GNU GPL
Requires:     Java 1.5 or later

---------------------------------------------------------------------------
                         BUILDING
---------------------------------------------------------------------------
To build the source, you will need to use ant with the provided build.xml
file.  (Ant 1.6 or later is required.)

To build with ant:

ant

This build process will create the following jar file (Where N.N.N is the
version number):

	dist/k5njournal-N.N.N.jar

---------------------------------------------------------------------------
                         RUNNING THE APP
---------------------------------------------------------------------------

To run the k5njournal application, you can double-click on the file
in your file browser (Windows Explorer, Mac OS X Finder, etc.), or you
can start it from the command line:

java -jar k5njournal-N.N.N.jar

(where N.N.N is the version)

---------------------------------------------------------------------------
                         LICENSE
---------------------------------------------------------------------------

This application and all associated tools and applications are licensed under
the GNU General Public License.

For information about this license:

	http://www.gnu.org/licenses/gpl.html
	
This tool makes use of and include Joda Time:

	http://www.joda.org/
	
Joda Time is licensed under the Apache License:

	http://joda-time.sourceforge.net/license.html
