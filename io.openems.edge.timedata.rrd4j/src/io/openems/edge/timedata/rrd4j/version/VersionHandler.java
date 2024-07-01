package io.openems.edge.timedata.rrd4j.version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.rrd4j.core.RrdDb;

import io.openems.edge.timedata.rrd4j.Rrd4jConstants;

@Component(//
		scope = ServiceScope.SINGLETON, //
		service = { VersionHandler.class } //
)
public class VersionHandler {

	/**
	 * Gets the version of the given {@link RrdDb}.
	 * 
	 * @param db the database to get the version from
	 * @return the version
	 * @throws IOException on I/O-Error
	 */
	public static int getVersion(RrdDb db) throws IOException {
		if (db.getArcCount() > 2 || db.getRrdDef().getStep() == 60) {
			return 1;
		}

		final var ds = db.getDatasource(0);
		final var name = ds.getName();
		if (name.length() > Rrd4jConstants.DEFAULT_DATASOURCE_NAME.length()) {
			final var versionNumberString = name.substring(name.lastIndexOf('_') + 1);
			return Integer.parseInt(versionNumberString);
		}
		return 2;
	}

	private final List<Version> versions = new ArrayList<>();

	/**
	 * Binds a {@link Version} to this current handler.
	 * 
	 * @param version the {@link Version} to bind
	 */
	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			bind = "bindVersion", unbind = "unbindVersion" //
	)
	public void bindVersion(Version version) {
		// make sure the versions list is sorted by the version number ascending
		final var insertIndex = Collections.binarySearch(this.versions, version, Version.numberComparator());
		if (insertIndex < 0) {
			this.versions.add(-insertIndex - 1, version);
		}
	}

	/**
	 * Unbinds a {@link Version} from this current handler.
	 * 
	 * @param version the {@link Version} to unbind
	 */
	public void unbindVersion(Version version) {
		this.versions.remove(version);
	}

	@Activate
	public VersionHandler() {
	}

	/**
	 * Gets the latest version of the found {@link Version Versions}.
	 * 
	 * <p>
	 * Note: In order to automatically add a version, the {@link Version} needs to
	 * have the annotation:
	 * 
	 * <pre>
	 * &#64;Component(//
	 *   scope = ServiceScope.SINGLETON, //
	 *   property = { "version:Integer=(version number)" }, //
	 *   immediate = true //
	 * )
	 * </pre>
	 * 
	 * @return the latest version
	 */
	public Version getLatestVersion() {
		return this.versions.get(this.versions.size() - 1);
	}

	/**
	 * Gets the latest version number.
	 * 
	 * @return the number of the latest version
	 * @see Version#getVersion()
	 */
	public int getLatestVersionNumber() {
		return this.getLatestVersion().getVersion();
	}

	/**
	 * Checks if the database matches the current version.
	 * 
	 * @param db the database to check
	 * @return true if the database is on the latest version
	 * @throws IOException on I/O-Error
	 */
	public boolean isUpToDate(RrdDb db) throws IOException {
		return getVersion(db) == this.getLatestVersionNumber();
	}

	/**
	 * Gets all versions. Ordered by the version number ascending.
	 * 
	 * @return a unmodifiable list of the versions
	 */
	public List<Version> getVersions() {
		return Collections.unmodifiableList(this.versions);
	}

}
