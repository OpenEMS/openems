package io.openems.edge.controller.api.websocket;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import io.openems.common.KacoConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.UpdateDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;

public class KacoUpdateHandler {

	static long uiCount = 0;
	static final int UI_DOWNLOAD_STEP = 1;
	static final int UI_BACKUP_STEP = 2;
	static final int UI_INSTALL_STEP = 3;
	static final int EDGE_DOWNLOAD_STEP = 4;
	static final int EDGE_INSTALL_STEP = 5;
	static final int UI_RESTORE_STEP = 6;

	public static void updateUI(WebsocketApi parent, WebSocket ws)
			throws IOException, IllegalArgumentException, OpenemsNamedException {
		File uiDirectory = new File(KacoConstants.UI_FOLDER);
		File uiBackup = new File(KacoConstants.UI_FOLDER_BACKUP);
		sendProgressNotification(0, UI_DOWNLOAD_STEP, ws, parent);
		downloadFile(KacoConstants.UPDATE_URL + KacoConstants.UI_FILE,
				KacoConstants.LOCAL_FOLDER + KacoConstants.UI_FILE, parent, ws, UI_DOWNLOAD_STEP);
		sendProgressNotification(0, UI_BACKUP_STEP, ws, parent);
		makeUiBackup(uiDirectory, uiBackup, ws, parent, UI_BACKUP_STEP);
		sendProgressNotification(0, UI_INSTALL_STEP, ws, parent);
		unzipUI(parent, ws);
		sendProgressNotification(100, 0, ws, parent);
	}

	public static void uiRestore(WebsocketApi parent, WebSocket ws) throws IOException {
		sendProgressNotification(0, UI_RESTORE_STEP, ws, parent);
		File uiDirectory = new File(KacoConstants.UI_FOLDER);
		File uiBackup = new File(KacoConstants.UI_FOLDER_BACKUP);
		FileUtils.cleanDirectory(uiDirectory);
		makeUiBackup(uiBackup, uiDirectory, ws, parent, UI_RESTORE_STEP);
		sendProgressNotification(100, 0, ws, parent);
	}

	public static void updateEdge(WebSocket ws, WebsocketApi parent)
			throws IOException, IllegalArgumentException, OpenemsNamedException {
		sendProgressNotification(0, EDGE_DOWNLOAD_STEP, ws, parent);
		downloadFile(KacoConstants.UPDATE_URL + KacoConstants.EDGE_FILE,
				KacoConstants.TMP_FOLDER + KacoConstants.EDGE_FILE, parent, ws, EDGE_DOWNLOAD_STEP);
		sendProgressNotification(0, EDGE_INSTALL_STEP, ws, parent); // backup Edge;
		backupEdge(KacoConstants.LOCAL_FOLDER + KacoConstants.EDGE_FILE,
				KacoConstants.LOCAL_FOLDER + KacoConstants.EDGE_BACKUP);
		copyEdgeFile(KacoConstants.TMP_FOLDER + KacoConstants.EDGE_FILE,
				KacoConstants.LOCAL_FOLDER + KacoConstants.EDGE_FILE, EDGE_INSTALL_STEP, ws, parent);
		sendProgressNotification(100, 0, ws, parent);

	}

	private static void backupEdge(String from, String to) {
		File old = new File(from);
		File backup = new File(to);
		FileUtils.deleteQuietly(backup);
		old.renameTo(backup);

	}

	public static void restoreEdge() {
		backupEdge(KacoConstants.LOCAL_FOLDER + KacoConstants.EDGE_BACKUP,
				KacoConstants.LOCAL_FOLDER + KacoConstants.EDGE_FILE);
	}

	private static void downloadFile(String source, String dest, WebsocketApi parent, WebSocket ws, int step)
			throws IOException, IllegalArgumentException, OpenemsNamedException {

		URL sourceUrl = new URL(source);
		URLConnection con = sourceUrl.openConnection();
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		con.connect();
		int fileSize = con.getContentLength();
		InputStream iStream = con.getInputStream();
		File destination = new File(dest);
		copyInputStreamToFileNew(iStream, destination, fileSize, parent, ws, step);

	}

	private static void unzipUI(WebsocketApi parent, WebSocket ws)
			throws IOException, IllegalArgumentException, OpenemsNamedException {
		String fileZip = KacoConstants.LOCAL_FOLDER + KacoConstants.UI_FILE;
		File destDir = new File(KacoConstants.UI_FOLDER);
		byte[] buffer = new byte[1024];

		ZipFile zf = new ZipFile(fileZip);
		int entrycount = zf.size();
		zf.close();

		ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));

		ZipEntry zipEntry = zis.getNextEntry();

		int tmpProgress = -1;
		int progress = 0;
		int count = 0;

		Channel<Integer> progressChannel = parent.componentManager.getComponent("_kacoUpdate").channel("Progress");

		while (zipEntry != null) {

			File newFile = newFile(destDir, zipEntry);

			if (zipEntry.isDirectory()) {
				newFile.mkdirs();
			} else {
				newFile.getParentFile().mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();

			}

			count++;
			progress = (int) (count * 100 / entrycount);

			if (ws != null && progress != tmpProgress) {
				sendProgressNotification(progress, UI_INSTALL_STEP, ws, parent);
			}
			tmpProgress = progress;

			zipEntry = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
		progressChannel.setNextValue(-1);
	}

	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	private static void copyInputStreamToFileNew(final InputStream source, final File destination, int fileSize,
			WebsocketApi parent, WebSocket ws, int step)
			throws IOException, IllegalArgumentException, OpenemsNamedException {
		final int EOF = -1;
		final int DEFAULT_BUFFER_SIZE = 1024 * 4;

		Channel<Integer> progressChannel = parent.componentManager.getComponent("_kacoUpdate").channel("Progress");

		try {

			final FileOutputStream output = FileUtils.openOutputStream(destination);
			try {

				final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				long count = 0;
				int n = 0;
				int progress = 0;
				int tmpProgress = -1;

				while (EOF != (n = source.read(buffer))) {
					output.write(buffer, 0, n);
					count += n;
					
					progress = (int) (count * 100 / fileSize);
					if (ws != null && progress != tmpProgress) {

						sendProgressNotification(progress, step, ws, parent);
					}
					tmpProgress = progress;

				}

				output.close(); // don't swallow close Exception if copy completes normally
			} finally {
				IOUtils.closeQuietly(output);
			}

		} finally {
			IOUtils.closeQuietly(source);
		}
		progressChannel.setNextValue(-1);
	}

	private static void sendProgressNotification(int progress, int step, WebSocket ws, WebsocketApi parent) {
		Gson gson = new Gson();
		UpdateDataNotification progressNotification = new UpdateDataNotification();
		progressNotification.add(new ChannelAddress("_kacoUpdate", "Progress"), gson.toJsonTree(progress));
		progressNotification.add(new ChannelAddress("_kacoUpdate", "UpdateStep"), gson.toJsonTree(step));
		EdgeRpcNotification noti = new EdgeRpcNotification(WebsocketApi.EDGE_ID, progressNotification);

		parent.server.sendMessage(ws, noti);
	}

	private static void makeUiBackup(File uiDirectory, File uiBackup, WebSocket ws, WebsocketApi parent, int step)
			throws IOException {

		long uiSize = FileUtils.sizeOfDirectory(uiDirectory);

		FileFilter pseudoFilter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return true;
				}
				long fileSize = FileUtils.sizeOf(pathname);
				uiCount += fileSize;
				int progress = (int) (uiCount * 100 / uiSize);
				if (progress == 100) {
					progress = 99;
				}
				sendProgressNotification(progress, step, ws, parent);
				return true;
			}

		};
		FileUtils.copyDirectory(uiDirectory, uiBackup, pseudoFilter);
		FileUtils.cleanDirectory(uiDirectory);
		uiCount = 0;

	}

	private static void copyEdgeFile(String srcPath, String destPath, int step, WebSocket ws, WebsocketApi parent)
			throws IllegalArgumentException, IOException, OpenemsNamedException {
		File src = new File(srcPath);
		InputStream iStream = new FileInputStream(src);
		int size = (int) src.length();

		File dest = new File(destPath);
		copyInputStreamToFileNew(iStream, dest, size, parent, ws, step);

	}

}
