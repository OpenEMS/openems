package io.openems.edge.common.meta.types;

import static io.openems.edge.common.meta.types.CountryCode.AT;
import static io.openems.edge.common.meta.types.CountryCode.CH;
import static io.openems.edge.common.meta.types.CountryCode.CZ;
import static io.openems.edge.common.meta.types.CountryCode.DE;
import static io.openems.edge.common.meta.types.CountryCode.ES;
import static io.openems.edge.common.meta.types.CountryCode.GR;
import static io.openems.edge.common.meta.types.CountryCode.LT;
import static io.openems.edge.common.meta.types.CountryCode.NL;
import static io.openems.edge.common.meta.types.CountryCode.RO;
import static io.openems.edge.common.meta.types.CountryCode.SE;

/**
 * Enum representing ISO 3166-2 subdivision codes for various countries. Each
 * constant corresponds to a specific administrative region identified by its
 * ISO code and associated with a {@link CountryCode}.
 */
public enum SubdivisionCode {

	/** Unknown or undefined subdivision. */
	UNDEFINED(CountryCode.UNDEFINED, "Undefined"), //

	/** Germany. */
	DE_BW(DE, "Baden-Württemberg"), //
	DE_BY(DE, "Bavaria"), //
	DE_BE(DE, "Berlin"), //
	DE_BB(DE, "Brandenburg"), //
	DE_HB(DE, "Bremen"), //
	DE_HH(DE, "Hamburg"), //
	DE_HE(DE, "Hesse"), //
	DE_MV(DE, "Mecklenburg-Vorpommern"), //
	DE_NI(DE, "Lower Saxony"), //
	DE_NW(DE, "North Rhine-Westphalia"), //
	DE_RP(DE, "Rhineland-Palatinate"), //
	DE_SL(DE, "Saarland"), //
	DE_SN(DE, "Saxony"), //
	DE_ST(DE, "Saxony-Anhalt"), //
	DE_SH(DE, "Schleswig-Holstein"), //
	DE_TH(DE, "Thuringia"), //

	/** Austria. */
	AT_1(AT, "Burgenland"), //
	AT_2(AT, "Carinthia"), //
	AT_3(AT, "Lower Austria"), //
	AT_4(AT, "Upper Austria"), //
	AT_5(AT, "Salzburg"), //
	AT_6(AT, "Styria"), //
	AT_7(AT, "Tyrol"), //
	AT_8(AT, "Vorarlberg"), //
	AT_9(AT, "Vienna"), //

	/** Switzerland. */
	CH_AG(CH, "Aargau"), //
	CH_AR(CH, "Appenzell Ausserrhoden"), //
	CH_AI(CH, "Appenzell Innerrhoden"), //
	CH_BL(CH, "Basel-Landschaft"), //
	CH_BS(CH, "Basel-Stadt"), //
	CH_BE(CH, "Bern"), //
	CH_FR(CH, "Fribourg"), //
	CH_GE(CH, "Geneva"), //
	CH_GL(CH, "Glarus"), //
	CH_GR(CH, "Graubünden"), //
	CH_JU(CH, "Jura"), //
	CH_LU(CH, "Lucerne"), //
	CH_NE(CH, "Neuchâtel"), //
	CH_NW(CH, "Nidwalden"), //
	CH_OW(CH, "Obwalden"), //
	CH_SG(CH, "St. Gallen"), //
	CH_SH(CH, "Schaffhausen"), //
	CH_SZ(CH, "Schwyz"), //
	CH_SO(CH, "Solothurn"), //
	CH_TG(CH, "Thurgau"), //
	CH_TI(CH, "Ticino"), //
	CH_UR(CH, "Uri"), //
	CH_VS(CH, "Valais"), //
	CH_VD(CH, "Vaud"), //
	CH_ZG(CH, "Zug"), //
	CH_ZH(CH, "Zürich"), //

	/** Spain. */
	ES_AN(ES, "Andalusia"), //
	ES_AR(ES, "Aragon"), //
	ES_AS(ES, "Principality of Asturias"), //
	ES_CN(ES, "Canary Islands"), //
	ES_CB(ES, "Cantabria"), //
	ES_CL(ES, "Castile and León"), //
	ES_CM(ES, "Castile-La Mancha"), //
	ES_CT(ES, "Catalonia"), //
	ES_CE(ES, "Ceuta"), //
	ES_EX(ES, "Extremadura"), //
	ES_GA(ES, "Galicia"), //
	ES_IB(ES, "Balearic Islands"), //
	ES_RI(ES, "La Rioja"), //
	ES_MD(ES, "Community of Madrid"), //
	ES_ML(ES, "Melilla"), //
	ES_MC(ES, "Region of Murcia"), //
	ES_NC(ES, "Chartered Community of Navarre"), //
	ES_PV(ES, "Basque Country"), //
	ES_VC(ES, "Valencian Community"), //

	/** Greece. */
	GR_69(GR, "Mount Athos (self-governed part)"), //
	GR_A(GR, "Eastern Macedonia and Thrace"), //
	GR_I(GR, "Attica"), //
	GR_G(GR, "Western Greece"), //
	GR_C(GR, "Western Macedonia"), //
	GR_F(GR, "Ionian Islands"), //
	GR_D(GR, "Epirus"), //
	GR_B(GR, "Central Macedonia"), //
	GR_M(GR, "Crete"), //
	GR_L(GR, "Southern Aegean"), //
	GR_J(GR, "Peloponnese"), //
	GR_H(GR, "Central Greece"), //
	GR_E(GR, "Thessaly"), //
	GR_K(GR, "Northern Aegean"), //

	/** Netherlands. */
	NL_DR(NL, "Drenthe"), //
	NL_FL(NL, "Flevoland"), //
	NL_FR(NL, "Friesland"), //
	NL_GE(NL, "Gelderland"), //
	NL_GR(NL, "Groningen"), //
	NL_LI(NL, "Limburg"), //
	NL_NB(NL, "North Brabant"), //
	NL_NH(NL, "North Holland"), //
	NL_OV(NL, "Overijssel"), //
	NL_UT(NL, "Utrecht"), //
	NL_ZE(NL, "Zeeland"), //
	NL_ZH(NL, "South Holland"), //

	/** Romania. */
	RO_AB(RO, "Alba"), //
	RO_AR(RO, "Arad"), //
	RO_AG(RO, "Arges"), //
	RO_BC(RO, "Bacau"), //
	RO_BH(RO, "Bihor"), //
	RO_BN(RO, "Bistrita-Nasaud"), //
	RO_BT(RO, "Botosani"), //
	RO_BV(RO, "Brasov"), //
	RO_BR(RO, "Braila"), //
	RO_B(RO, "Bucharest"), //
	RO_BZ(RO, "Buzau"), //
	RO_CS(RO, "Caras-Severin"), //
	RO_CL(RO, "Calarasi"), //
	RO_CJ(RO, "Cluj"), //
	RO_CT(RO, "Constanta"), //
	RO_CV(RO, "Covasna"), //
	RO_DB(RO, "Dambovita"), //
	RO_DJ(RO, "Dolj"), //
	RO_GL(RO, "Galati"), //
	RO_GR(RO, "Giurgiu"), //
	RO_GJ(RO, "Gorj"), //
	RO_HR(RO, "Harghita"), //
	RO_HD(RO, "Hunedoara"), //
	RO_IL(RO, "Ialomita"), //
	RO_IS(RO, "Iasi"), //
	RO_IF(RO, "Ilfov"), //
	RO_MM(RO, "Maramures"), //
	RO_MH(RO, "Mehedinti"), //
	RO_MS(RO, "Mures"), //
	RO_NT(RO, "Neamt"), //
	RO_OT(RO, "Olt"), //
	RO_PH(RO, "Prahova"), //
	RO_SM(RO, "Satu Mare"), //
	RO_SJ(RO, "Salaj"), //
	RO_SB(RO, "Sibiu"), //
	RO_SV(RO, "Suceava"), //
	RO_TR(RO, "Teleorman"), //
	RO_TM(RO, "Timis"), //
	RO_TL(RO, "Tulcea"), //
	RO_VS(RO, "Vaslui"), //
	RO_VL(RO, "Valcea"), //
	RO_VN(RO, "Vrancea"), //

	/** Sweden. */
	SE_K(SE, "Blekinge"), //
	SE_W(SE, "Dalarna"), //
	SE_I(SE, "Gotland"), //
	SE_X(SE, "Gavleborg"), //
	SE_N(SE, "Halland"), //
	SE_Z(SE, "Jamtland"), //
	SE_F(SE, "Jonkoping"), //
	SE_H(SE, "Kalmar"), //
	SE_G(SE, "Kronoberg"), //
	SE_BD(SE, "Norrbotten"), //
	SE_M(SE, "Scania"), //
	SE_AB(SE, "Stockholm"), //
	SE_D(SE, "Sodermanland"), //
	SE_C(SE, "Uppsala"), //
	SE_S(SE, "Varmland"), //
	SE_AC(SE, "Vasterbotten"), //
	SE_Y(SE, "Western Northland"), //
	SE_U(SE, "Vastmanland"), //
	SE_O(SE, "Vastra Gotaland"), //
	SE_T(SE, "Orebro"), //
	SE_E(SE, "Ostergotland"), //

	/** Lithuania. */
	LT_AL(LT, "Alytus County"), //
	LT_KU(LT, "Kaunas County"), //
	LT_KL(LT, "Klaipeda County"), //
	LT_MR(LT, "Marijampole County"), //
	LT_PN(LT, "Panevezys County"), //
	LT_SA(LT, "Siauliai County"), //
	LT_TA(LT, "Taurage County"), //
	LT_TE(LT, "Telsiai County"), //
	LT_UT(LT, "Utena County"), //
	LT_VL(LT, "Vilnius County"), //

	/** Czech Republic. */
	CZ_31(CZ, "South Bohemia"), //
	CZ_64(CZ, "South Moravia"), //
	CZ_41(CZ, "Karlovy Vary"), //
	CZ_52(CZ, "Hradec Kralove"), //
	CZ_51(CZ, "Liberec"), //
	CZ_80(CZ, "Moravia-Silesia"), //
	CZ_71(CZ, "Olomouc"), //
	CZ_53(CZ, "Pardubice"), //
	CZ_32(CZ, "Plzen"), //
	CZ_10(CZ, "Prague Capital"), //
	CZ_20(CZ, "Central Bohemia"), //
	CZ_42(CZ, "Usti nad Labem"), //
	CZ_63(CZ, "Vysocina"), //
	CZ_72(CZ, "Zlin"), //

	;

	private final CountryCode countryCode;
	private final String subdivisionName;

	private SubdivisionCode(CountryCode countryCode, String subdivisionName) {
		this.countryCode = countryCode;
		this.subdivisionName = subdivisionName;
	}

	/**
	 * Gets the ISO 3166-1 alpha-2 country code.
	 * 
	 * @return {@link CountryCode}
	 */
	public CountryCode getCountryCode() {
		return this.countryCode;
	}

	/**
	 * Gets the subdivision name.
	 * 
	 * <p>
	 * English translation if available. Local name otherwise. Avoid special
	 * characters.
	 * 
	 * @return subdivision name
	 */
	public String getSubdivisionName() {
		return this.subdivisionName;
	}

	/**
	 * Gets the ISO 3166-2 subdivision code.
	 * 
	 * @return ISO 3166-2 subdivision code
	 */
	public String getCode() {
		return this.name().replace('_', '-');
	}

	@Override
	public String toString() {
		return this.getCode();
	}

	/**
	 * Returns the subdivision part of this code. For example, for "DE-BY" returns
	 * "BY".
	 *
	 * @return the subdivision part
	 */
	public String getSubdivisionPart() {
		String name = this.name();
		int index = name.indexOf('_');
		if (index > 0 && index < name.length() - 1) {
			return name.substring(index + 1);
		}
		return null;
	}

	/**
	 * Converts an ISO 3166-2 code (e.g. "DE-BY") to the corresponding
	 * {@link SubdivisionCode} enum constant. Returns {@link #UNDEFINED} if the
	 * input is null, blank, or not recognized.
	 *
	 * @param code the subdivision code string
	 * @return the matching {@link SubdivisionCode} or {@link #UNDEFINED}
	 */
	public static SubdivisionCode fromCode(String code) {
		if (code == null || code.isBlank()) {
			return UNDEFINED;
		}
		String enumName = code.trim().replace('-', '_').toUpperCase();
		try {
			return valueOf(enumName);
		} catch (IllegalArgumentException e) {
			return UNDEFINED;
		}
	}
}
