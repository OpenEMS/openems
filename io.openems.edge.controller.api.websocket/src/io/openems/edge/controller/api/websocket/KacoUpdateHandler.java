package io.openems.edge.controller.api.websocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import io.openems.common.KacoConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.CurrentDataNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.edge.common.channel.Channel;

public class KacoUpdateHandler {

	public static void updateUI(WebsocketApi parent)
			throws IOException, IllegalArgumentException, OpenemsNamedException {
		File uiDirectory = new File(KacoConstants.UI_FOLDER);
		File uiBackup = new File(KacoConstants.UI_FOLDER_BACKUP);
		downloadFile(KacoConstants.UPDATE_URL + KacoConstants.UI_FILE,
				KacoConstants.LOCAL_FOLDER + KacoConstants.UI_FILE, parent, null);
		FileUtils.copyDirectory(uiDirectory, uiBackup);
		FileUtils.cleanDirectory(uiDirectory);
		unzipUI();

	}

	public static void uiRestore() throws IOException {
		File uiDirectory = new File(KacoConstants.UI_FOLDER);
		File uiBackup = new File(KacoConstants.UI_FOLDER_BACKUP);
		FileUtils.cleanDirectory(uiDirectory);
		FileUtils.copyDirectory(uiBackup, uiDirectory);
	}

	public static void updateEdge(WsData wsData, WebsocketApi parent)
			throws IOException, IllegalArgumentException, OpenemsNamedException {

		downloadFile(KacoConstants.UPDATE_URL + KacoConstants.EDGE_FILE,
				KacoConstants.LOCAL_FOLDER + KacoConstants.EDGE_FILE, parent, wsData);

	}

	private static void downloadFile(String source, String dest, WebsocketApi parent, WsData wsData)
			throws IOException, IllegalArgumentException, OpenemsNamedException {
		URL sourceUrl = new URL(source);
		URLConnection con = sourceUrl.openConnection();
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		con.connect();
		int fileSize = con.getContentLength();
		InputStream iStream = con.getInputStream();
		File destination = new File(dest);
		copyInputStreamToFileNew(iStream, destination, fileSize, parent, wsData);

	}

	private static void unzipUI() throws IOException {
		String fileZip = KacoConstants.LOCAL_FOLDER + KacoConstants.UI_FILE;
		File destDir = new File(KacoConstants.UI_FOLDER);
		byte[] buffer = new byte[1024];
		ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
		ZipEntry zipEntry = zis.getNextEntry();
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

			zipEntry = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
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
			WebsocketApi parent, WsData wsData) throws IOException, IllegalArgumentException, OpenemsNamedException {
		final int EOF = -1;
		final int DEFAULT_BUFFER_SIZE = 1024 * 4;

		Channel<Integer> progressChannel = parent.componentManager.getComponent("_kacoUpdate").channel("Progress");

		WebSocket ws = null;
		if (wsData != null) {
			ws = wsData.getWebsocket();
		}

		try {

			final FileOutputStream output = FileUtils.openOutputStream(destination);
			try {

				final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				long count = 0;
				int n = 0;
				int progress = 0;
				int tmpProgress = -1;
				Gson gson = new Gson();

				while (EOF != (n = source.read(buffer))) {
					output.write(buffer, 0, n);
					count += n;
					// progressChannel.setNextValue(count * 100/fileSize);
					progress = (int) (count * 100 / fileSize);
					if (ws != null && progress != tmpProgress) {

						CurrentDataNotification progressNotification = new CurrentDataNotification();
						progressNotification.add(progressChannel.address(), gson.toJsonTree(progress));
						EdgeRpcNotification noti = new EdgeRpcNotification(WebsocketApi.EDGE_ID, progressNotification);

						parent.server.sendMessage(ws, noti);
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
	}

}
