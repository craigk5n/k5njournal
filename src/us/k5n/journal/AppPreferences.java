package us.k5n.journal;

/**
 * A convenience class for using the java Preferences class. All application
 * preferences will be stored using the java.util.prefs.Preferences class. This
 * class abstracts out the details of where the info saved (Windows registry,
 * etc.)
 * 
 * @version $Id: AppPreferences.java,v 1.3 2007-05-02 14:06:59 cknudsen Exp $
 * @author Craig Knudsen, craig
 * @k5n.us
 */
public class AppPreferences {
	java.util.prefs.Preferences prefs = null;
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

	/**
	 * Get height of main window
	 * 
	 * @return
	 */
	public int getMainWindowHeight () {
		return prefs.getInt ( MAIN_WINDOW_WIDTH, 600 );
	}

	/**
	 * Set height of main window
	 * 
	 * @param mainWindowHeight
	 *          height of main window (pixels)
	 */
	public void setMainWindowHeight ( int mainWindowHeight ) {
		prefs.putInt ( MAIN_WINDOW_HEIGHT, mainWindowHeight );
	}

	/**
	 * Get divider location for horizontally divided JSplitPane. This value is in
	 * pixels.
	 * 
	 * @return
	 */
	public int getMainWindowHorizontalSplitPosition () {
		return prefs.getInt ( MAIN_WINDOW_HORIZONTAL_SPLIT_POSITION, 185 );
	}

	/**
	 * Set divider location for horizontally divided JSplitPane. This value is in
	 * pixels.
	 * 
	 * @param mainWindowHorizontalSplitPosition
	 *          The new divider location (in pixels)
	 * @return
	 */
	public void setMainWindowHorizontalSplitPosition (
	    int mainWindowHorizontalSplitPosition ) {
		prefs.putInt ( MAIN_WINDOW_HORIZONTAL_SPLIT_POSITION,
		    mainWindowHorizontalSplitPosition );
	}

	/**
	 * Get divider location for vertically divided JSplitPane. This value is in
	 * pixels.
	 * 
	 * @return
	 */
	public int getMainWindowVerticalSplitPosition () {
		return prefs.getInt ( MAIN_WINDOW_VERTICAL_SPLIT_POSITION, 200 );
	}

	/**
	 * Set divider location for vertically divided JSplitPane. This value is in
	 * pixels.
	 * 
	 * @param mainWindowVerticalSplitPosition
	 *          The new divider location (in pixels)
	 * @return
	 */
	public void setMainWindowVerticalSplitPosition (
	    int mainWindowVerticalSplitPosition ) {
		prefs.putInt ( MAIN_WINDOW_VERTICAL_SPLIT_POSITION,
		    mainWindowVerticalSplitPosition );
	}

	/**
	 * Get main window width
	 * 
	 * @return
	 */
	public int getMainWindowWidth () {
		return prefs.getInt ( MAIN_WINDOW_WIDTH, 600 );
	}

	/**
	 * Set main window width
	 * 
	 * @param mainWindowWidth
	 *          width of main window (in pixels)
	 */
	public void setMainWindowWidth ( int mainWindowWidth ) {
		prefs.putInt ( MAIN_WINDOW_WIDTH, mainWindowWidth );
	}

	/**
	 * Get the main window X position
	 * 
	 * @return
	 */
	public int getMainWindowX () {
		return prefs.getInt ( MAIN_WINDOW_X, 15 );
	}

	/**
	 * Set the main window X position
	 * 
	 * @param mainWindowX
	 *          The X position of the main window
	 */
	public void setMainWindowX ( int mainWindowX ) {
		prefs.putInt ( MAIN_WINDOW_X, mainWindowX );
	}

	/**
	 * Get the main window Y position
	 * 
	 * @return
	 */
	public int getMainWindowY () {
		return prefs.getInt ( MAIN_WINDOW_Y, 15 );
	}

	/**
	 * Set the main window Y position
	 * 
	 * @param mainWindowY
	 *          The main window Y position
	 */
	public void setMainWindowY ( int mainWindowY ) {
		prefs.putInt ( MAIN_WINDOW_Y, mainWindowY );
	}

}
