package io.openems.edge.kaco.blueplanet.hybrid10;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;

public enum ErrorChannelId implements io.openems.edge.common.channel.ChannelId {
	A001(Doc.of(Level.WARNING).text("Emergency Stop")), //
	A002(Doc.of(Level.WARNING).text("Key Manual Stop")), //
	A003(Doc.of(Level.WARNING).text("Transformer Phase B Temperature Sensor Invalidation")), //
	A004(Doc.of(Level.WARNING).text("SD Memory Card Invalidation")), //
	A005(Doc.of(Level.WARNING).text("Inverter Communication Abnormity")), //
	A010(Doc.of(Level.WARNING).text("Battery Stack Communication Abnormity")), //
	A021(Doc.of(Level.WARNING).text("Multifunctional Ammeter Communication Abnormity")), //
	A022(Doc.of(Level.WARNING).text("Remote Communication Abnormity")), //
	A030(Doc.of(Level.WARNING).text("PVDC1 Communication Abnormity")), //
	A032(Doc.of(Level.WARNING).text("PVDC2 Communication Abnormity")), //
	A040(Doc.of(Level.WARNING).text("Transformer Severe Overtemperature")), //
	A050(Doc.of(Level.WARNING).text("DC Precharge Contactor Close Unsuccessfully")), //
	A060(Doc.of(Level.WARNING).text("AC Precharge Contactor Close Unsuccessfully")), //
	A071(Doc.of(Level.WARNING).text("AC Main Contactor Close Unsuccessfully")), //
	A072(Doc.of(Level.WARNING).text("DC Electrical Breaker1 Close Unsuccessfully")), //
	A100(Doc.of(Level.WARNING).text("DC Main Contactor Close Unsuccessfully")), //
	A110(Doc.of(Level.WARNING).text("AC Breaker Trip")), //
	A200(Doc.of(Level.WARNING).text("AC Main Contactor Open When Running")), //
	A210(Doc.of(Level.WARNING).text("DC Main Contactor Open When Running")), //
	A220(Doc.of(Level.WARNING).text("AC Main Contactor Open Unsuccessfully")), //
	A230(Doc.of(Level.WARNING).text("DC Electrical Breaker1 Open Unsuccessfully")), //

	E001(Doc.of(Level.FAULT).text("DC Main Contactor Open Unsuccessfully")), //
	E002(Doc.of(Level.FAULT).text("Hardware PDP Fault")), //
	E010(Doc.of(Level.FAULT).text("Master Stop Suddenly")), //
	E021(Doc.of(Level.FAULT).text("DCShortCircuitProtection")), //
	E022(Doc.of(Level.FAULT).text("DCOvervoltageProtection")), //
	E030(Doc.of(Level.FAULT).text("DCUndervoltageProtection")), //
	E041(Doc.of(Level.FAULT).text("DCInverseNoConnectionProtection")), //
	E042(Doc.of(Level.FAULT).text("DCDisconnectionProtection")), //
	E050(Doc.of(Level.FAULT).text("CommutingVoltageAbnormityProtection")), //
	E060(Doc.of(Level.FAULT).text("DCOvercurrentProtection")), //
	E070(Doc.of(Level.FAULT).text("Phase1PeakCurrentOverLimitProtection")), //
	E080(Doc.of(Level.FAULT).text("Phase2PeakCurrentOverLimitProtection")), //
	E101(Doc.of(Level.FAULT).text("Phase3PeakCurrentOverLimitProtection")), //
	E102(Doc.of(Level.FAULT).text("Phase1GridVoltageSamplingInvalidation")), //
	E103(Doc.of(Level.FAULT).text("Phase2VirtualCurrentOverLimitProtection")), //
	E104(Doc.of(Level.FAULT).text("Phase3VirtualCurrentOverLimitProtection")), //
	E110(Doc.of(Level.FAULT).text("Phase1GridVoltageSamplingInvalidation2")), //
	E120(Doc.of(Level.FAULT).text("Phase2ridVoltageSamplingInvalidation")), //
	E140(Doc.of(Level.FAULT).text("Phase3GridVoltageSamplingInvalidation")), //
	E150(Doc.of(Level.FAULT).text("Phase1InvertVoltageSamplingInvalidation")), //
	E160(Doc.of(Level.FAULT).text("Phase2InvertVoltageSamplingInvalidation")), //
	E170(Doc.of(Level.FAULT).text("Phase3InvertVoltageSamplingInvalidation")), //
	E180(Doc.of(Level.FAULT).text("ACCurrentSamplingInvalidation")); //

	private final Doc doc;

	private ErrorChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}

	public String getErrorCode() {
		return this.name();
	}
}