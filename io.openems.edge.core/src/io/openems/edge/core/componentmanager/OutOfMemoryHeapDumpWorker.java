package io.openems.edge.core.componentmanager;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.ComponentManager;

/**
 * This Worker constantly checks for heap-dump files in /usr/lib/openems
 * directory. Those get created on OutOfMemory-Errors. All but the latest
 * heap-dump file are deleted and the
 * {@link ComponentManagerImpl.ChannelId#WAS_OUT_OF_MEMORY} StateChannel is set.
 */
public class OutOfMemoryHeapDumpWorker extends AbstractWorker {

	private final static int CYCLE_TIME = 300_000; // in ms

	private final Logger log = LoggerFactory.getLogger(OutOfMemoryHeapDumpWorker.class);

	private final ComponentManagerImpl parent;

	public OutOfMemoryHeapDumpWorker(ComponentManagerImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() {
		boolean foundhprof = false;

		File currentWorkingDir = Paths.get("").toAbsolutePath().toFile();
		File[] files = currentWorkingDir.listFiles();
		// From the docs: 'files' is null if this abstract pathname does not denote a
		// directory, or if an I/O error occurs.
		if (files != null) {

			Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

			for (File file : files) {
				String filename = file.getName();

				// delete 'core' files
				if (filename.equals("core")) {
					// delete core files
					this.delete(file);
					continue;
				}

				// delete all but the first *.hprof files
				if (filename.endsWith(".hprof")) {
					if (!foundhprof) {
						foundhprof = true;
					} else {
						this.delete(file);
					}
				}

				// delete all *.log files
				if (filename.endsWith(".log")) {
					this.delete(file);
				}
			}
		}

		this.parent.channel(ComponentManager.ChannelId.WAS_OUT_OF_MEMORY).setNextValue(foundhprof);
	}

	private void delete(File file) {
		this.log.info("Deleting file [" + file.getAbsolutePath() + "]");
		file.delete();
	}

	@Override
	protected int getCycleTime() {
		return CYCLE_TIME;
	}

}
