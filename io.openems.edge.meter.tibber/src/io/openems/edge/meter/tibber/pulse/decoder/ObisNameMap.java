package io.openems.edge.meter.tibber.pulse.decoder;

import java.util.Map;
import java.util.TreeMap;

/**
 * https://www.bundesnetzagentur.de/DE/Beschlusskammern/BK06/BK6_83_Zug_Mess/835_mitteilungen_datenformate/Mitteilung_24/2_EDIFACT-Konsultationsdokumente/Codeliste%20der%20OBIS-Kennzahlen%20und%20Medien%202.4.pdf?__blob=publicationFile&v=1.
 * https://www.promotic.eu/en/pmdoc/Subsystems/Comm/PmDrivers/IEC62056_OBIS.htm
 * https://www.dzg.de/fileadmin/dzg/content/downloads/produkte-zaehler/dvze/DZG_DVZE_Handbuch_201211.pdf
 */
public class ObisNameMap {

	protected static final Map<String, String> obisNameMap = new TreeMap<>();

	static {
		obisNameMap.put("1-0:1.8.0*255", "energyImportTotal");
		obisNameMap.put("1-0:1.8.1*255", "energyImportTariff1");
		obisNameMap.put("1-0:1.8.2*255", "energyImportTariff2");
		obisNameMap.put("1-0:2.8.0*255", "energyExportTotal");
		obisNameMap.put("1-0:2.8.1*255", "energyExportTariff1");
		obisNameMap.put("1-0:2.8.2*255", "energyExportTariff2");
		obisNameMap.put("1-0:16.7.0*255", "powerTotal");
		obisNameMap.put("1-0:36.7.0*255", "powerL1");
		obisNameMap.put("1-0:56.7.0*255", "powerL2");
		obisNameMap.put("1-0:76.7.0*255", "powerL3");
		obisNameMap.put("1-0:32.7.0*255", "voltageL1");
		obisNameMap.put("1-0:52.7.0*255", "voltageL2");
		obisNameMap.put("1-0:72.7.0*255", "voltageL3");
		obisNameMap.put("1-0:31.7.0*255", "currentL1");
		obisNameMap.put("1-0:51.7.0*255", "currentL2");
		obisNameMap.put("1-0:71.7.0*255", "currentL3");
		obisNameMap.put("1-0:81.7.1*255", "phaseAngleUL2toUL1");
		obisNameMap.put("1-0:81.7.2*255", "phaseAngleUL3toUL1");
		obisNameMap.put("1-0:81.7.4*255", "phaseAngleIL1toUL1");
		obisNameMap.put("1-0:81.7.15*255", "phaseAngleIL2toUL2");
		obisNameMap.put("1-0:81.7.26*255", "phaseAngleIL3toUL3");
		obisNameMap.put("1-0:14.7.0*255", "networkFrequency");

	}

	/**
	 * Retrieves the descriptive name corresponding to a given OBIS code from a
	 * predefined mapping.
	 *
	 * <p>
	 * This method looks up the human-readable name associated with an OBIS (Object
	 * Identification System) code, which is used to uniquely identify data items in
	 * energy metering. If the OBIS code exists in the mapping, the corresponding
	 * name is returned; otherwise, null is returned.
	 * </p>
	 *
	 * @param obisCode The OBIS code for which the descriptive name is requested.
	 * @return The descriptive name associated with the given OBIS code, or null if
	 *         the code is not found in the mapping.
	 */

	public static String get(String obisCode) {
		return obisNameMap.get(obisCode);
	}

}
