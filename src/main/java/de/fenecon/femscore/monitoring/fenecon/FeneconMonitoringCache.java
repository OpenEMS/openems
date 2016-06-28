package de.fenecon.femscore.monitoring.fenecon;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeneconMonitoringCache {
	public static final String CACHE_DB_PATH = "/opt/fems/cache.db";

	private Logger logger = LoggerFactory.getLogger(FeneconMonitoringCache.class);
	private DB db = null; // only use after calling getMapDB()
	BlockingQueue<TimedElementValue> cache = null; // only use after calling
													// getMapDB()
	private final FeneconMonitoringWorker worker;

	public FeneconMonitoringCache(FeneconMonitoringWorker worker) {
		this.worker = worker;
	}

	@SuppressWarnings("deprecation")
	private void getMapDB() throws Exception {
		if (cache == null) {
			File cacheDbFile = new File(CACHE_DB_PATH);
			try {
				logger.info("Opening cache database");
				if (db == null) {
					db = DBMaker.fileDB(cacheDbFile).fileLockDisable().serializerRegisterClass(TimedElementValue.class)
							.closeOnJvmShutdown().make();
				}
				cache = db.getQueue("fems");
				logger.info("Opening cache database: finished");

			} catch (Exception e) {
				logger.error("Error opening cache database; delete and try again");
				worker.offer(new TimedElementValue(FeneconMonitoringWorker.FEMS_SYSTEMMESSAGE,
						"ERROR opening cache database: " + e.getMessage()));
				e.printStackTrace();
				Files.delete(cacheDbFile.toPath());
				try {
					if (db == null) {
						db = DBMaker.fileDB(cacheDbFile).fileLockDisable()
								.serializerRegisterClass(TimedElementValue.class).closeOnJvmShutdown().make();
					}
					cache = db.getQueue("fems");
				} catch (Exception e1) {
					worker.offer(new TimedElementValue(FeneconMonitoringWorker.FEMS_SYSTEMMESSAGE,
							"REPEATED ERROR opening cache database: " + e.getMessage()));
					e.printStackTrace();
					db = null;
					throw e;
				}
			}
		}
	}

	public void dispose() {
		logger.info("Closing cache database");
		try {
			getMapDB();
			db.commit();
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<TimedElementValue> pollMany(int count) throws Exception {
		ArrayList<TimedElementValue> returnList = new ArrayList<>(count);
		getMapDB();
		for (int i = 0; i < count && !cache.isEmpty(); i++) {
			TimedElementValue tev = cache.poll();
			if (tev == null)
				break;
			returnList.add(tev);
		}
		db.commit();
		return returnList;
	}

	public void addAll(Collection<TimedElementValue> c) throws Exception {
		getMapDB();
		for (TimedElementValue tev : c) {
			cache.add(tev);
		}
		db.commit();
	}

	public String isEmpty() {
		try {
			getMapDB();
			if (cache.isEmpty()) {
				return "empty";
			} else {
				return "filled";
			}
		} catch (Exception e) {
			return "not available";
		}
	}
}
