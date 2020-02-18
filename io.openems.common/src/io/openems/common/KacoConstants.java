package io.openems.common;

public class KacoConstants {

	// public final static String UPDATE_URL = "https://www.energydepot.de/primus/update/";
	public final static String UPDATE_URL = "https://www.energydepot.de/primus-dev/update/";
	public final static String LOCAL_FOLDER = "/usr/lib/hy-control/";
	//public final static String LOCAL_FOLDER = "C:\\Users\\hummelsberger\\Documents\\";
	public final static String EDGE_FILE = "hy-control.jar";
	public final static String UI_FILE = "edge.zip";
	public final static String UI_FOLDER = "/var/www/html/";
	public final static String UI_FOLDER_BACKUP = LOCAL_FOLDER + "ui-backup/";
	public final static String RESTART_CMD = "sudo systemctl restart hy-control.service";
	public final static String USERNAME = "pi";
	public final static String PASSWORD = "raspberry";
}
