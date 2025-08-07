package io.openems.edge.io.shelly.common;

import java.util.Set;

/**
 * Defines the expected device models for each Shelly component type. These
 * model IDs are retrieved from the /shelly endpoint.
 */
public class ShellyDeviceModels {

	private ShellyDeviceModels() {
	}

	// Generation 1
	public static final Set<String> SHELLY1 = Set.of("SHSW-1");
	public static final Set<String> SHELLY1L = Set.of("SHSW-L");
	public static final Set<String> SHELLY1PM = Set.of("SHSW-PM");
	public static final Set<String> SHELLY2_RELAY = Set.of("SHSW-21");
	public static final Set<String> SHELLY2_ROLLER = Set.of("SHSW-21");
	public static final Set<String> SHELLY25_RELAY = Set.of("SHSW-25");
	public static final Set<String> SHELLY25_ROLLER = Set.of("SHSW-25");
	public static final Set<String> SHELLY4PRO = Set.of("SHSW-44");
	public static final Set<String> SHELLYDIMMER = Set.of("SHDM-1");
	public static final Set<String> SHELLYDIMMER2 = Set.of("SHDM-2");
	public static final Set<String> SHELLYIX3 = Set.of("SHIX3-1");
	public static final Set<String> SHELLYUNI = Set.of("SHUNI-1");
	public static final Set<String> SHELLYPLUG = Set.of("SHPLG2-1");
	public static final Set<String> SHELLYPLUGS = Set.of("SHPLG-S");
	public static final Set<String> SHELLYEM = Set.of("SHEM");
	public static final Set<String> SHELLYEM3 = Set.of("SHEM-3");
	public static final Set<String> SHELLYRGBW2_COLOR = Set.of("SHRGBW2");
	public static final Set<String> SHELLYRGBW2_WHITE = Set.of("SHRGBW2");
	public static final Set<String> SHELLYBULB_COLOR = Set.of("SHBLB-1");
	public static final Set<String> SHELLYBULB_WHITE = Set.of("SHBLB-1");
	public static final Set<String> SHELLYBULBDUO = Set.of("SHBDUO-1");
	public static final Set<String> SHELLYCOLORBULB = Set.of("SHCB-1");
	public static final Set<String> SHELLYVINTAGE = Set.of("SHVIN-1");
	public static final Set<String> SHELLYHT = Set.of("SHHT-1");
	public static final Set<String> SHELLYFLOOD = Set.of("SHWT-1");
	public static final Set<String> SHELLYSMOKE = Set.of("SHSM-1");
	public static final Set<String> SHELLYMOTION = Set.of("SHMOS-01");
	public static final Set<String> SHELLYMOTION2 = Set.of("SHMOS-02");
	public static final Set<String> SHELLYGAS = Set.of("SHGS-1");
	public static final Set<String> SHELLYDW = Set.of("SHDW-1");
	public static final Set<String> SHELLYDW2 = Set.of("SHDW-2");
	public static final Set<String> SHELLYBUTTON1 = Set.of("SHBTN-1");
	public static final Set<String> SHELLYBUTTON2 = Set.of("SHBTN-2");
	public static final Set<String> SHELLYSENSE = Set.of("SHSEN-1");
	public static final Set<String> SHELLYTRV = Set.of("SHTRV-01");

	// Shelly Plus series (Generation 2+3+4)
	public static final Set<String> SHELLYPLUS1 = Set.of("SNSW-001X16EU", "S3SW-001X16EU", "S4SW-001X16EU");
	public static final Set<String> SHELLYPLUS1L = Set.of("S3SW-0A1X1EUL");
	public static final Set<String> SHELLYPLUS1PM = Set.of("SNSW-001P16EU", "S3SW-001P16EU", "S4SW-001P16EU");
	public static final Set<String> SHELLYPLUS2L = Set.of("S3SW-0A2X4EUL");
	public static final Set<String> SHELLYPLUS2PM_RELAY = Set.of("SNSW-002P16EU", "SNSW-102P16EU", "S3SW-002P16EU",
			"SNSW-002P15UL", "S4SW-002P16EU");
	public static final Set<String> SHELLYPLUS2PM_ROLLER = Set.of("SNSW-002P16EU", "SNSW-102P16EU", "S3SW-002P16EU",
			"SNSW-002P15UL", "S4SW-002P16EU");
	public static final Set<String> SHELLYPLUSSHUTTER = Set.of("S3SH-0A2P4EU");
	public static final Set<String> SHELLYPLUSEM = Set.of("S3EM-002CXCEU");
	public static final Set<String> SHELLYPLUS3EM63 = Set.of("S3EM-002CXCEU");
	public static final Set<String> SHELLYPLUSPLUG = Set.of("SNPL-00112EU", "SNPL-10112EU", "S3PL-00112EU",
			"SNPL-00110IT", "SNPL-00112UK", "S3PL-20112EU");
	public static final Set<String> SHELLYPLUSPLUGUS = Set.of("SNPL-00116US");
	public static final Set<String> SHELLYPLUSSTRIP = Set.of("S4PL-00416EU");
	public static final Set<String> SHELLYPLUSI4 = Set.of("SNSN-0024X", "S3SN-0024X");
	public static final Set<String> SHELLYPLUSI4DC = Set.of("SNSN-0D24X");
	public static final Set<String> SHELLYPLUS10V = Set.of("SNDM-00100WW", "S3DM-0010WW");
	public static final Set<String> SHELLYPLUSUNI = Set.of("SNSN-0043X");
	public static final Set<String> SHELLYPLUSHT = Set.of("SNSN-0013A", "S3SN-0U12A");
	public static final Set<String> SHELLYPLUSSMOKE = Set.of("SNSN-0031Z");
	public static final Set<String> SHELLYPLUSWDUS = Set.of("SNDM-0013US");
	public static final Set<String> SHELLYPLUSRGBWPM = Set.of("SNDC-0D4P10WW");
	public static final Set<String> SHELLYWALLDISPLAY = Set.of("SAWD-0A1XX10EU1");

	// Shelly Plus Mini series (Generation 2+3+4)
	public static final Set<String> SHELLY1MINI = Set.of("SNSW-001X8EU", "S3SW-001X8EU", "S4SW-001X8EU");
	public static final Set<String> SHELLY1PMMINI = Set.of("SNSW-001P8EU", "S3SW-001P8EU", "S4SW-001P8EU");
	public static final Set<String> SHELLYPMMINI = Set.of("SNPM-001PCEU16", "S3PM-001PCEU16");
	public static final Set<String> SHELLYEMMINI = Set.of("S4EM-001PXCEU16");

	// Shelly Pro Series (Generation 2+3)
	public static final Set<String> SHELLYPRO1 = Set.of("SPSW-001XE16EU", "SPSW-101XE16EU", "SPSW-201XE16EU");
	public static final Set<String> SHELLYPRO1PM = Set.of("SPSW-001PE16EU", "SPSW-101PE16EU", "SPSW-201PE16EU");
	public static final Set<String> SHELLYPRO1CB = Set.of("SPCB-01VENEU");
	public static final Set<String> SHELLYPRO2_RELAY = Set.of("SPSW-002XE16EU", "SPSW-102XE16EU", "SPSW-202XE16EU");
	public static final Set<String> SHELLYPRO2PM_RELAY = Set.of("SPSW-002PE16EU", "SPSW-102PE16EU", "SPSW-202PE16EU");
	public static final Set<String> SHELLYPRO2PM_ROLLER = Set.of("SPSW-002PE16EU", "SPSW-102PE16EU", "SPSW-202PE16EU");
	public static final Set<String> SHELLYPRO3 = Set.of("SPSW-003XE16EU");
	public static final Set<String> SHELLYPRO3EM = Set.of("SPEM-003CEBEU");
	public static final Set<String> SHELLYPROEM50 = Set.of("SPEM-002CEBEU50");
	public static final Set<String> SHELLYPRO4PM = Set.of("SHPSW04P", "SPSW-004PE16EU", "SPSW-104PE16EU");

	// Shelly BLU
	public static final Set<String> SHELLYBLUBUTTON = Set.of("SBBT");
	public static final Set<String> SHELLYBLUDW = Set.of("SBDW");
	public static final Set<String> SHELLYBLUMOTION = Set.of("SBMO");
	public static final Set<String> SHELLYBLUHT = Set.of("SBMO");
	public static final Set<String> SHELLYBLUGW = Set.of("SNGW-BT01");
	public static final Set<String> SHELLYBLUGW3 = Set.of("S3GW-1DBT001");
}