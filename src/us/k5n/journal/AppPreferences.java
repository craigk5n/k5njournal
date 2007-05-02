package us.k5n.journal;

/**
 * A convenience class for using the java Preferences class.
 * 
 * @version $Id: AppPreferences.java,v 1.1 2007-05-02 13:59:41 cknudsen Exp $
 * @author Craig Knudsen, craig
 * @k5n.us
 */
public class AppPreferences {
	java.util.prefs.Preferences prefs;
	static final String MAIN_WINDOW_HEIGHT = "MainWindow.height";
	static final String MAIN_WINDOW_WIDTH = "MainWindow.width";
	static final String MAIN_WINDOW_X = "MainWindow.x";
	static final String MAIN_WINDOW_Y = "MainWindow.y";
	static final String MAIN_WINDOW_VERTICAL_SPLIT_POSITION = "MainWindow.vSplitPanePosition";
	static final String MAIN_WINDOW_HORIZONTAL_SPLIT_POSITION = "MainWindow.hSplitPanePosition";
	private static AppPreferences instance = null;

	public AppPreferences() {
		this.prefs = java.util.prefs.Preferences.userNodeForPackage ( this
		    .getClass () );
	}

	public static AppPreferences getInstance () {
		if ( instance == null )
			instance = new AppPreferences ();
		return instance;
	}

	public int getMainWindowHeight () {
		return prefs.getInt ( MAIN_WINDOW_WIDTH, 600 );
	}

	public void setMainWindowHeight ( int mainWindowHeight ) {
		prefs.putInt ( MAIN_WINDOW_HEIGHT, mainWindowHeight );
	}

	public int getMainWindowHorizontalSplitPosition () {
		return prefs.getInt ( MAIN_WINDOW_HORIZONTAL_SPLIT_POSITION, 185 );
	}

	public void setMainWindowHorizontalSplitPosition (
	    int mainWindowHorizontalSplitPosition ) {
		prefs.putInt ( MAIN_WINDOW_HORIZONTAL_SPLIT_POSITION,
		    mainWindowHorizontalSplitPosition );
	}

	public int getMainWindowVerticalSplitPosition () {
		return prefs.getInt ( MAIN_WINDOW_VERTICAL_SPLIT_POSITION, 200 );
	}

	public void setMainWindowVerticalSplitPosition (
	    int mainWindowVerticalSplitPosition ) {
		prefs.putInt ( MAIN_WINDOW_VERTICAL_SPLIT_POSITION,
		    mainWindowVerticalSplitPosition );
	}

	public int getMainWindowWidth () {
		return prefs.getInt ( MAIN_WINDOW_WIDTH, 600 );
	}

	public void setMainWindowWidth ( int mainWindowWidth ) {
		prefs.putInt ( MAIN_WINDOW_WIDTH, mainWindowWidth );
	}

	public int getMainWindowX () {
		return prefs.getInt ( MAIN_WINDOW_X, 15 );
	}

	public void setMainWindowX ( int mainWindowX ) {
		prefs.putInt ( MAIN_WINDOW_X, mainWindowX );
	}

	public int getMainWindowY () {
		return prefs.getInt ( MAIN_WINDOW_Y, 15 );
	}

	public void setMainWindowY ( int mainWindowY ) {
		prefs.putInt ( MAIN_WINDOW_Y, mainWindowY );
	}

}
