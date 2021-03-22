package io.openems.edge.heater.chp.dachs;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.heater.api.ChpBasic;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.heater.chp.dachs.api.DachsGltInterfaceChannel;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;


/**
 * Dachs GLT interface.
 * This controller communicates with a Dachs CHP via the GLT interface and maps the return message to OpenEMS channels.
 * Read and write is supported.
 * Not all GLT commands have been coded in yet, only those for basic CHP operation.
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "DachsGltInterfaceImpl",
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		immediate = true)
public class DachsGltInterfaceImpl extends AbstractOpenemsComponent implements OpenemsComponent, ChpBasic, DachsGltInterfaceChannel, Controller {

	private final Logger log = LoggerFactory.getLogger(DachsGltInterfaceImpl.class);
	private InputStream is = null;
	private String urlBuilderIP;
	private String basicAuth;
	private int interval;
	private LocalDateTime timestamp;

	// Variables for channel mapping
	private boolean rpmChannelHasData;
	private int rpmValue;


	public DachsGltInterfaceImpl() {
		super(OpenemsComponent.ChannelId.values(),
				DachsGltInterfaceChannel.ChannelId.values(),
				ChpBasic.ChannelId.values(),
				Controller.ChannelId.values());
	}

	@Activate
	public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		interval = config.interval();
		// Limit interval to 9 minutes max. Because on/off command needs to be sent to Dachs at least once every 10 minutes.
		if (interval > 540) {
			interval = 540;
		}
		timestamp = LocalDateTime.now().minusSeconds(interval-2);	//Shift timing a bit, may help to avoid executing at the same time as other demanding controllers.
		urlBuilderIP = config.address();
		String gltpass = config.username() + ":" + config.password();
		basicAuth = "Basic " + new String(Base64.getEncoder().encode(gltpass.getBytes()));
		getSerialAndPartsNumber();

	}

	@Deactivate
	public void deactivate() { super.deactivate(); }


	@Override
	public void run() throws OpenemsError.OpenemsNamedException {
		// Transfer channel data to local variables for better readability
		rpmChannelHasData = this.getRpm().value().isDefined();
		if (rpmChannelHasData) {
			rpmValue = this.getRpm().value().get();
		}


		// How often the Dachs is polled is determined by "interval"
		if (ChronoUnit.SECONDS.between(timestamp, LocalDateTime.now()) >= interval) {
			updateChannels();
			timestamp = LocalDateTime.now();

			// Use the "setOnOff()" channel like a write channel is used with Modbus. value()/setNextValue() is the readout,
			// nextWriteValue is the write command. Keep the two separate.
			// The Dachs does not have an on/off indicator. So instead use RPM readout to tell if Dachs is running or not.
			// If the CHP is running with >1000 RPM, it is on. If not it is off. (regular RPM is ~2400).
			if (rpmChannelHasData) {
				if (rpmValue > 1000) {
					this.setOnOff().setNextValue(true);
				} else {
					this.setOnOff().setNextValue(false);
				}
			}

			// This is supposed to be the on-off switch. There are some things to watch out for:
			// - This is not a hard command, especially the "off" command. The Dachs has a list of reasons to be running
			// 	 (see Dachs-Lauf-Anforderungen), the "external requirement" (this on/off switch) being one of many. If
			// 	 any one of those reasons is true, it is running. Only if all of them are false, it will shut down.
			// 	 Bottom line, only if the Dachs is off because nothing else tells it to run (and it is not shut down
			// 	 because of a limitation) will this switch do anything.
			// - Timing: need to send "on" command at least every 10 minutes for the Dachs to keep running.
			//   "interval" is capped at 9 minutes, so this should be taken care of.
			// - Also: You cannot switch a CHP on/off as you want. There is a limit on how often you can start. Number of
			//   starts should be minimised. Currently the code does not enforce any restrictions in this regard!
			if (this.setOnOff().getNextWriteValue().isPresent()) {
				if (this.setOnOff().getNextWriteValue().get()) {
					activateDachs();
				} else {
					deactivateDachs();
				}
			}
		}
	}


	protected void updateChannels() {
		// "getKeyDachs" is the command to request data from the server. The answer is saved to "serverMessage".
		String serverMessage = getKeyDachs(		//For a description of these arguments, look in DachsGltInterfaceChannel
                        "k=Wartung_Cache.fStehtAn&" +
						"k=Hka_Bd.ulAnzahlStarts&" +
                        "k=Hka_Mw1.usDrehzahl&" +
						"k=Hka_Bd.ulBetriebssekunden&" +
						"k=Hka_Bd.ulArbeitThermKon&" +
                        "k=Hka_Bd.ulArbeitThermHka&" +
						"k=Hka_Bd.ulArbeitElektr&" +
						"k=Hka_Bd.Anforderung.UStromF_Anf.bFlagSF&" +
						"k=Hka_Bd.UHka_Anf.Anforderung.fStrom&" +
						"k=Hka_Bd.UStromF_Frei.bFreigabe&" +
						"k=Hka_Bd.Anforderung.ModulAnzahl&" +
						"k=Hka_Bd.UHka_Anf.usAnforderung&" +
						"k=Hka_Bd.UHka_Frei.usFreigabe&" +
                        "k=Hka_Mw1.Temp.sbRuecklauf&" +
                        "k=Hka_Mw1.Temp.sbVorlauf&" +
						"k=Hka_Mw1.sWirkleistung&" +
						"k=Hka_Bd.bWarnung&" +
                        "k=Hka_Bd.bStoerung");

		// The return message now needs to be processed. The message is queried for a marker, then the value after the
		// marker is read. Then the value is parsed according to the description in the manual. The result is then
		// written into the corresponding channel.

		// Test if a marker can be found in the message to see if the server message is ok. If this marker can not be
		// found, the server message is most likely garbage.
		if (serverMessage.contains("Hka_Bd.bStoerung=")) {

			String stoerung = readEntryAfterString(serverMessage, "Hka_Bd.bStoerung=");
			if (stoerung.equals("0")) {
				this.isError().setNextValue(false);
			} else {
				this.isError().setNextValue(true);
				String returnMessage = "Code " + stoerung + ":";
				if (stoerung.contains("101")) {
					returnMessage = returnMessage + " Abgasfühler HKA-Austritt - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("102")) {
					returnMessage = returnMessage + " Kühlwasserfühler Motor - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("103")) {
					returnMessage = returnMessage + " Kühlwasserfühler Generator - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("104")) {
					returnMessage = returnMessage + " Abgasfühler Motor-Austritt - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("105")) {
					returnMessage = returnMessage + " Vorlauftemperatur - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("106")) {
					returnMessage = returnMessage + " Rücklauftemperatur - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("107")) {
					returnMessage = returnMessage + " Fühler 1 - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("108")) {
					returnMessage = returnMessage + " Fühler 2 - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("109")) {
					returnMessage = returnMessage + " Außenfühler - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("110")) {
					returnMessage = returnMessage + " Kapselfühler - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("111")) {
					returnMessage = returnMessage + " Fühler Regler intern - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("120")) {
					returnMessage = returnMessage + " Abgastemperatur Motor-Austritt - zu hoch;G:>620°C,HR:>520°C,";
				}
				if (stoerung.contains("121")) {
					returnMessage = returnMessage + " Kapseltemperatur - zu hoch; > 120°C,";
				}
				if (stoerung.contains("122")) {
					returnMessage = returnMessage + " Kühlwassertemperatur Motor (Austritt) - zu hoch; > 95°C,";
				}
				if (stoerung.contains("123")) {
					returnMessage = returnMessage + " Abgastemperatur HKA-Austritt - zu hoch; > 210°C,";
				}
				if (stoerung.contains("124")) {
					returnMessage = returnMessage + " Kühlwassertemperatur Generator (Eintritt) - zu hoch; > 77°C,";
				}
				if (stoerung.contains("129")) {
					returnMessage = returnMessage + " Rückleistung - Brennstoffversorgung oder Zündung fehlerhaft,";
				}
				if (stoerung.contains("130")) {
					returnMessage = returnMessage + " Drehzahl nach Anlasser AUS - Drehzahl trotz ausgeschaltetem Anlasser bei Fehlstart,";
				}
				if (stoerung.contains("131")) {
					returnMessage = returnMessage + " HKA-Anlauf < 100 U/min - 1 sek nach Anlasser ein: n < 100 U/min,";
				}
				if (stoerung.contains("133")) {
					returnMessage = returnMessage + " HKA-Lauf < 2300 U/min - n<2300 U/min für 30 sek nach Erreichen 800 U/min,";
				}
				if (stoerung.contains("139")) {
					returnMessage = returnMessage + " Generatorzuschaltung - keine Zuschaltung bei Start Drehzahl > 2600 U/Min,";
				}
				if (stoerung.contains("140")) {
					returnMessage = returnMessage + " Generatorabschaltung - Drehzahl nicht im Drehzahlfenster länger als 1 Sek,";
				}
				if (stoerung.contains("151")) {
					returnMessage = returnMessage + " Startfreigabe - Startfreigabe von Überwachung fehlt,";
				}
				if (stoerung.contains("152")) {
					returnMessage = returnMessage + " NO UC_Daten b. Ini - interner Fehler,";
				}
				if (stoerung.contains("154")) {
					returnMessage = returnMessage + " NO KraftstoffInfo - Kraftstofftyp nicht erkannt,";
				}
				if (stoerung.contains("155")) {
					returnMessage = returnMessage + " Dif. Kraftstofftyp - unterschiedliche Kraftstofftypen erkannt,";
				}
				if (stoerung.contains("159")) {
					returnMessage = returnMessage + " Spannung b. Start - Spannungsfehler vor Start,";
				}
				if (stoerung.contains("160")) {
					returnMessage = returnMessage + " Spannung - Spannungsfehler nach Generatorzuschaltung,";
				}
				if (stoerung.contains("162")) {
					returnMessage = returnMessage + " Leistung zu hoch - Leistung um mehr als 500 Watt zu hoch,";
				}
				if (stoerung.contains("163")) {
					returnMessage = returnMessage + " Leistung zu klein - Leistung um mehr als 500 Watt zuniedrig,";
				}
				if (stoerung.contains("164")) {
					returnMessage = returnMessage + " Leistung im Stand - Mehr als +- 200 Watt bei stehenderAnlage,";
				}
				if (stoerung.contains("167")) {
					returnMessage = returnMessage + " Frequenz bei Start - Frequenzfehler vor Start,";
				}
				if (stoerung.contains("168")) {
					returnMessage = returnMessage + " Frequenz - Frequenzfehler nachGeneratorzuschaltung,";
				}
				if (stoerung.contains("171")) {
					returnMessage = returnMessage + " Öldruckschalter - Öldruckschalter im Stillstand länger als 2.6s geschlossen,";
				}
				if (stoerung.contains("172")) {
					returnMessage = returnMessage + " Ölstand prüfen! - Öldruckschalter während des Laufes länger als 12s offen,";
				}
				if (stoerung.contains("173")) {
					returnMessage = returnMessage + " MV Gas 1 / Hubmagnet - undicht, Abschaltung dauert länger als 5 s,";
				}
				if (stoerung.contains("174")) {
					returnMessage = returnMessage + " MV Gas 2 - undicht, Abschaltung dauert länger als 5 s,";
				}
				if (stoerung.contains("177")) {
					returnMessage = returnMessage + " Wartung notwendig - 1*täglich entstörbar; +300h=>nicht entstörbar (Wartungsbestätigung erf.),";
				}
				if (stoerung.contains("179")) {
					returnMessage = returnMessage + " 4 Starts < 2300 U/min - 4 erfolglose Startversuche Drehzahl < 2300 U/min nach 1 Minute,";
				}
				if (stoerung.contains("180")) {
					returnMessage = returnMessage + " Unterbrechung RF-Abbrand > 4 - nur bei Öl: 5 Abschaltungen bei Russfilterregeneration,";
				}
				if (stoerung.contains("184")) {
					returnMessage = returnMessage + " Drehfeld falsch - Drehfeld prüfen,";
				}
				if (stoerung.contains("185")) {
					returnMessage = returnMessage + " Flüssigkeitsschalter - nur bei Öl: Schalter geöffnet (erkennt Flüssigkeit),";
				}
				if (stoerung.contains("187")) {
					returnMessage = returnMessage + " Überdrehzahl - Drehzahl>3000 U/min,";
				}
				if (stoerung.contains("188")) {
					returnMessage = returnMessage + " 4 Starts 400 - 800 U/min - 4 erfolglose Startversuche 400 U/min < Drehzahl < 800 U/min,";
				}
				if (stoerung.contains("189")) {
					returnMessage = returnMessage + " 4 Starts < 400 U/min - 4 erfolglose Startversuche Drehzahl < 400 U/min,";
				}
				if (stoerung.contains("190")) {
					returnMessage = returnMessage + " Drehzahl > 15 U/min vor Start - Drehzahl vor Start > 15 U/m / Öldruck vor Start,";
				}
				if (stoerung.contains("191")) {
					returnMessage = returnMessage + " Drehzahl > 3500 U/min - Überdrehzahl,";
				}
				if (stoerung.contains("192")) {
					returnMessage = returnMessage + " UC verriegelt - Dachs von Überwachungssoftware verriegelt,";
				}
				if (stoerung.contains("200")) {
					returnMessage = returnMessage + " Fehler Stromnetz - keine genaue Spezifikation möglich,";
				}
				if (stoerung.contains("201")) {
					returnMessage = returnMessage + " Fehler MSR2 intern - keine genaue Spezifikation möglich,";
				}
				if (stoerung.contains("202")) {
					returnMessage = returnMessage + " Synchronisierung - Überwachungscontroller asynchron, Dachs am "
							+ "Motorschutzschalter aus- und einschalten,";
				}
				if (stoerung.contains("203")) {
					returnMessage = returnMessage + " Eeprom defekt - interner Fehler,";
				}
				if (stoerung.contains("204")) {
					returnMessage = returnMessage + " Ergebnis ungleich - interner Fehler,";
				}
				if (stoerung.contains("205")) {
					returnMessage = returnMessage + " Dif auf Messkanal - interner Fehler,";
				}
				if (stoerung.contains("206")) {
					returnMessage = returnMessage + " Multiplexer - interner Fehler,";
				}
				if (stoerung.contains("207")) {
					returnMessage = returnMessage + " Hauptrelais - interner Fehler,";
				}
				if (stoerung.contains("208")) {
					returnMessage = returnMessage + " AD-Wandler - interner Fehler,";
				}
				if (stoerung.contains("209")) {
					returnMessage = returnMessage + " Versorgung MCs - interner Fehler,";
				}
				if (stoerung.contains("210")) {
					returnMessage = returnMessage + " Prog.-laufzeit - 24h Abschaltung durch Überwachung,";
				}
				if (stoerung.contains("212")) {
					returnMessage = returnMessage + " Identifizierung - gegenseitige Identifizierung der Controller fehlerhaft,";
				}
				if (stoerung.contains("213")) {
					returnMessage = returnMessage + " Prog.-durchlauf - interner Fehler,";
				}
				if (stoerung.contains("214")) {
					returnMessage = returnMessage + " Busfehler intern - Störung auf dem internen CAN-Bus,";
				}
				if (stoerung.contains("215")) {
					returnMessage = returnMessage + " Leitungsbruch Gen - Leitungsunterbrechung zwischen Generatorschütz und Generator,";
				}
				if (stoerung.contains("216")) {
					returnMessage = returnMessage + " Spannung > 280V - mindestens eine Spannung > 280 V (>40ms),";
				}
				if (stoerung.contains("217")) {
					returnMessage = returnMessage + " Impedanz- es wurde ein Impedanzsprung > ENS-Grenzwert gemessen,";
				}
				if (stoerung.contains("218")) {
					returnMessage = returnMessage + " U-Si am X22 fehlt - an X22/15 liegt keine Spannung an,";
				}
				if (stoerung.contains("219")) {
					returnMessage = returnMessage + " U-SiKette fehlt - an X5/2 liegt keine Spannung an,";
				}
				if (stoerung.contains("220")) {
					returnMessage = returnMessage + " Gasdruck fehlt - an X22/13 liegt keine Spannung an,";
				}
				if (stoerung.contains("221")) {
					returnMessage = returnMessage + " Rückmeldungen - interner Fehler,";
				}
				if (stoerung.contains("222")) {
					returnMessage = returnMessage + " Rückm Generator - Signal an X21/7,";
				}
				if (stoerung.contains("223")) {
					returnMessage = returnMessage + " Rückm Sanftanlauf - Signal an X21/5,";
				}
				if (stoerung.contains("224")) {
					returnMessage = returnMessage + " Rückm Magnetv. - Sicherung F21 prüfen,";
				}
				if (stoerung.contains("225")) {
					returnMessage = returnMessage + " Rückm Anlasser - Signal an X21/8,";
				}
				if (stoerung.contains("226")) {
					returnMessage = returnMessage + " Rückm Hubmagnet - Sicherung F18 prüfen,";
				}
				if (stoerung.contains("250")) {
					returnMessage = returnMessage + " Vorlauffühler Heizkreis 1 - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("251")) {
					returnMessage = returnMessage + " Vorlauffühler Heizkreis 2 - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("252")) {
					returnMessage = returnMessage + " Warmwasserfühler - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("253")) {
					returnMessage = returnMessage + " Fühler 3 - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("254")) {
					returnMessage = returnMessage + " Fühler 4 - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("255")) {
					returnMessage = returnMessage + " Raumtemp. Fühler 1 - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("256")) {
					returnMessage = returnMessage + " Raumtemp. Fühler 2 - Unterbrechung/Kurzschluss,";
				}
				if (stoerung.contains("270")) {
					returnMessage = returnMessage + " Leitregler mehrfach - nur bei MM und LR: Leitregler mehrfach eingestellt,";
				}
				if (stoerung.contains("271")) {
					returnMessage = returnMessage + " Modul-Nr. mehrfach - nur bei MM und LR: Regler-Adresse mehrfach eingestellt,";
				}
				if (stoerung.contains("350")) {
					returnMessage = returnMessage + " EEP_DatenRP not OK - interner Fehler,";
				}
				if (stoerung.contains("354")) {
					returnMessage = returnMessage + " User Stack > Soll - interner Fehler,";
				}
				if (stoerung.contains("355")) {
					returnMessage = returnMessage + " Int. Stack > Soll - interner Fehler.";
				}
				if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
					returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
				}
				this.getErrorMessages().setNextValue(returnMessage);
			}
			this.logDebug(this.log, "isError: " + this.isError().getNextValue().get().toString());
			this.logDebug(this.log, "getErrorMessages: " + this.getErrorMessages().getNextValue().get());


			String warnung = readEntryAfterString(serverMessage, "Hka_Bd.bWarnung=");
			if (warnung.equals("0")) {
				this.isWarning().setNextValue(false);
			} else {
				this.isWarning().setNextValue(true);
				this.getWarningMessages().setNextValue(warnung);

				// Would put more code here to decipher warning code, but the warning code is not yet in the manual.

			}
			this.logDebug(this.log, "isWarning: " + this.isWarning().getNextValue().get().toString());


			String wirkleistung = "";	// To make sure there is no null exception when parsing.
			wirkleistung = readEntryAfterString(serverMessage, "Hka_Mw1.sWirkleistung=");
			try {
				this.getElectricalPower().setNextValue(Float.parseFloat(wirkleistung));
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse electrical power (Wirkleistung): " + e.getMessage());
				this.getElectricalPower().setNextValue(0);	// To avoid null exception when printing this channel to the log.
			}
			this.logDebug(this.log, "getElectricalPower: " + this.getElectricalPower().getNextValue().get().toString() + " kW");


			String forwardTemp = "";   // To make sure there is no null exception when parsing.
			forwardTemp = forwardTemp + readEntryAfterString(serverMessage, "Hka_Mw1.Temp.sbVorlauf=");
			try {
				this.getForwardTemp().setNextValue(Integer.parseInt(forwardTemp.trim())*10);	// Convert to dezidegree.
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse forward temperature (Vorlauf): " + e.getMessage());
				this.getForwardTemp().setNextValue(0);
			}
			this.logDebug(this.log, "getForwardTemp: " + this.getForwardTemp().getNextValue().get() + " dezidegree");


			String rewindTemp = "";
			rewindTemp = rewindTemp + readEntryAfterString(serverMessage, "Hka_Mw1.Temp.sbRuecklauf=");
			try {
				this.getRewindTemp().setNextValue(Integer.parseInt(rewindTemp.trim())*10);	// Convert to dezidegree.
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse rewind temperature (Ruecklauf): " + e.getMessage());
				this.getRewindTemp().setNextValue(0);
			}
			this.logDebug(this.log, "getRewindTemp: " + this.getRewindTemp().getNextValue().get() + " dezidegree");


			String freigabe = readEntryAfterString(serverMessage, "Hka_Bd.UHka_Frei.usFreigabe=");
			if (freigabe.equals("65535")) {	// This is the int equivalent of hex FFFF. Manual discusses freigabe code in hex.
				this.isReady().setNextValue(true);
				this.getNotReadyCode().setNextValue("Code FFFF: Dachs is ready to run.");
			} else {
				this.isReady().setNextValue(false);
				try {
					int tempInt = Integer.parseInt(freigabe.trim());
					String inHex = Integer.toHexString(tempInt).toUpperCase();
					// The code is a 4 digit hex. Digit 1, 3 and 4 can be from 0 to F, while digit 2 is C to F. The 0 to F
					// digits code for four options. Their 0 or 1 states are concatenated to form a 4 digit binary, that
					// is then represented by a single digit hex. Digit 2 of the hex code represents just two options,
					// that's why it is C to F.
					// The manual discusses the code in hex representation, while the server transmits the hex code as a
					// base 10 integer. When transforming back to a four digit hex, account for the fact that a 0 in the
					// first hex digit would have been lost in translation.
					// Make sure inHex has 4 digits just in case, to prevent string out of bounds error.
					for (int i = inHex.length(); i < 4; i++) {
						inHex = "0" + inHex;
					}
					String returnMessage = "Code " + inHex + ": Clearance not given by";
					String firstDigitInBinary = Integer.toBinaryString(Integer.parseInt(String.valueOf(inHex.charAt(0)),16));
					//Make sure it has 4 digits
					for (int i = firstDigitInBinary.length(); i < 4; i++) {
						firstDigitInBinary = "0" + firstDigitInBinary;
					}
					if (firstDigitInBinary.charAt(0) == '0') {
						returnMessage = returnMessage + " Inbetriebname OK,";
					}
					if (firstDigitInBinary.charAt(1) == '0') {
						returnMessage = returnMessage + " Taste OnOff,";
					}
					if (firstDigitInBinary.charAt(2) == '0') {
						returnMessage = returnMessage + " Interne Freigabe HKA,";
					}
					if (firstDigitInBinary.charAt(3) == '0') {
						returnMessage = returnMessage + " Eingang Modulfreigabe,";
					}
					if (inHex.charAt(1) == 'C') {
						returnMessage = returnMessage + " Startverzögerung, Netz OK,";
					}
					if (inHex.charAt(1) == 'D') {
						returnMessage = returnMessage + " Startverzögerung,";
					}
					if (inHex.charAt(1) == 'E') {
						returnMessage = returnMessage + " Netz OK,";
					}
					String thirdDigitInBinary = Integer.toBinaryString(Integer.parseInt(String.valueOf(inHex.charAt(2)),16));
					for (int i = thirdDigitInBinary.length(); i < 4; i++) {
						thirdDigitInBinary = "0" + thirdDigitInBinary;
					}
					if (thirdDigitInBinary.charAt(0) == '0') {
						returnMessage = returnMessage + " Rueckmeldung SiKette,";
					}
					if (thirdDigitInBinary.charAt(1) == '0') {
						returnMessage = returnMessage + " Max Ruecklauftemperatur,";
					}
					if (thirdDigitInBinary.charAt(2) == '0') {
						returnMessage = returnMessage + " Temperatur,";
					}
					if (thirdDigitInBinary.charAt(3) == '0') {
						returnMessage = returnMessage + " Stoerung,";
					}
					String fourthDigitInBinary = Integer.toBinaryString(Integer.parseInt(String.valueOf(inHex.charAt(3)),16));
					for (int i = fourthDigitInBinary.length(); i < 4; i++) {
						fourthDigitInBinary = "0" + fourthDigitInBinary;
					}
					if (fourthDigitInBinary.charAt(0) == '0') {
						returnMessage = returnMessage + " Lauf24h,";
					}
					if (fourthDigitInBinary.charAt(1) == '0') {
						returnMessage = returnMessage + " Stillstandszeit,";
					}
					if (fourthDigitInBinary.charAt(2) == '0') {
						returnMessage = returnMessage + " Abschaltzeit,";
					}
					if (fourthDigitInBinary.charAt(3) == '0') {
						returnMessage = returnMessage + " Anforderung HKA.";
					}
					if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
						returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
					}
					this.getNotReadyCode().setNextValue(returnMessage);
				} catch (NumberFormatException e) {
					this.logError(this.log, "Error, can't parse NotReadyCode: " + e.getMessage());
					this.getNotReadyCode().setNextValue("Code " + freigabe + ": Error deciphering code.");
				}
			}
			this.logDebug(this.log, "isReady: " + this.isReady().getNextValue().get().toString());
			this.logDebug(this.log, "getNotReadyCode: " + this.getNotReadyCode().getNextValue().get());


			String laufAnforderung = readEntryAfterString(serverMessage, "Hka_Bd.UHka_Anf.usAnforderung=");
			if (laufAnforderung.equals("0")) {
				this.getRunSetting().setNextValue("Code 0: Nothing is requesting the Dachs to run right now.");
			} else {
				try {
					int tempInt = Integer.parseInt(laufAnforderung.trim());
					String returnMessage = "Code " + Integer.toHexString(tempInt).toUpperCase() + ": Running requested by";
					// This code represents 8 options. Their 0 or 1 states are concatenated to a 8 digit binary that the
					// server transmits as a base 10 integer. The Manual discusses the code as a hex number.
					String inBinary = Integer.toBinaryString(tempInt);
					// Make sure the string has 8 digits to avoid string out of bounds error.
					for (int i = inBinary.length(); i < 8; i++) {
						inBinary = "0" + inBinary;
					}
					if (inBinary.charAt(0) == '1') {
						returnMessage = returnMessage + " Mehrmodul,";
					}
					if (inBinary.charAt(1) == '1') {
						returnMessage = returnMessage + " Strom,";
					}
					if (inBinary.charAt(2) == '1') {
						returnMessage = returnMessage + " Manuell,";
					}
					if (inBinary.charAt(3) == '1') {
						returnMessage = returnMessage + " Extern,";
					}
					if (inBinary.charAt(4) == '1') {
						returnMessage = returnMessage + " Hoher Sollwert,";
					}
					if (inBinary.charAt(5) == '1') {
						returnMessage = returnMessage + " BW Bereitung,";
					}
					if (inBinary.charAt(6) == '1') {
						returnMessage = returnMessage + " Waerme,";
					}
					if (inBinary.charAt(7) == '1') {
						returnMessage = returnMessage + " Mindestlaufzeit.";
					}
					if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
						returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
					}
					this.getRunSetting().setNextValue(returnMessage);
				} catch (NumberFormatException e) {
					this.logError(this.log, "Error, can't parse RunSettings: " + e.getMessage());
					this.getRunSetting().setNextValue("Code " + laufAnforderung + ": Error deciphering code.");
				}
			}
			this.logDebug(this.log, "getRunSetting: " + this.getRunSetting().getNextValue().get());


			String modulzahl = "";
			modulzahl = modulzahl + readEntryAfterString(serverMessage, "Hka_Bd.Anforderung.ModulAnzahl=");
			try {
				this.getNumberOfRequestedModules().setNextValue(Integer.parseInt(modulzahl.trim()));
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse NumberOfRequestedModules: " + e.getMessage());
				this.getNumberOfRequestedModules().setNextValue(0);
			}
			this.logDebug(this.log, "getNumberOfRequestedModules: " + this.getNumberOfRequestedModules().getNextValue().get());


			String freigabeStromfuehrung = readEntryAfterString(serverMessage, "Hka_Bd.UStromF_Frei.bFreigabe=");
			if (freigabeStromfuehrung.equals("255")) {
				this.getElecGuidedClearance().setNextValue("Code FF: Dachs is in electric power guided mode.");
			} else {
				try {
					int tempInt = Integer.parseInt(freigabeStromfuehrung.trim());
					String inHex = Integer.toHexString(tempInt).toUpperCase();
					String returnMessage = "Code " + inHex + ": No electric power guided mode because the following is missing -";
					// This code represents 4 options. Their 0 or 1 states are concatenated to a 4 digit binary that is
					// represented by a single digit hex. To this hex, F0 is added to form a two digit hex where only the
					// second digit has meaning. The server then transmits this number as a base 10 integer. The Manual
					// discusses the code as a hex number.
					String secondDigitInBinary = "1111";	// Fallbackvalue in case inHex.length() != 2
					if (inHex.length() == 2) {
						secondDigitInBinary = Integer.toBinaryString(Integer.parseInt(String.valueOf(inHex.charAt(1)),16));
					} else {
						returnMessage = "Code " + inHex + ": Error deciphering code.";
					}
					for (int i = secondDigitInBinary.length(); i < 4; i++) {
						secondDigitInBinary = "0" + secondDigitInBinary;
					}
					if (secondDigitInBinary.charAt(0) == '0') {
						returnMessage = returnMessage + " SoWi,";
					}
					if (secondDigitInBinary.charAt(1) == '0') {
						returnMessage = returnMessage + " HtNt,";
					}
					if (secondDigitInBinary.charAt(2) == '0') {
						returnMessage = returnMessage + " MaxStrom,";
					}
					if (secondDigitInBinary.charAt(3) == '0') {
						returnMessage = returnMessage + " Anforderung Strom.";
					}
					if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
						returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
					}
					this.getElecGuidedClearance().setNextValue(returnMessage);
				} catch (NumberFormatException e) {
					this.logError(this.log, "Error, can't parse ElecGuidedClearance: " + e.getMessage());
					this.getElecGuidedClearance().setNextValue("Code " + freigabeStromfuehrung + ": Error deciphering code.");
				}
			}
			this.logDebug(this.log, "getElecGuidedClearance: " + this.getElecGuidedClearance().getNextValue().get());


			String anforderungStrom = readEntryAfterString(serverMessage, "Hka_Bd.UHka_Anf.Anforderung.fStrom=");
			if (anforderungStrom.equals("false")) {
				this.getElecGuidedRunFlag().setNextValue(false);
			} else {
				this.getElecGuidedRunFlag().setNextValue(true);
			}
			this.logDebug(this.log, "getElecGuidedRunFlag: " + this.getElecGuidedRunFlag().getNextValue().get().toString());


			String stromAnforderung = readEntryAfterString(serverMessage, "Hka_Bd.Anforderung.UStromF_Anf.bFlagSF=");
			if (stromAnforderung.equals("0")) {
				this.getElecGuidedSettings().setNextValue("Code 0: No component is requesting electric power guided mode.");
			} else {
				try {
					int tempInt = Integer.parseInt(stromAnforderung.trim());
					String returnMessage = "Code " + Integer.toHexString(tempInt).toUpperCase() + ": Electric power guided mode requested by";
					// This code represents 5 options. Their 0 or 1 states are concatenated to a 5 digit binary that the
					// server transmits as a base 10 integer. The Manual discusses the code as a hex number.
					String inBinary = Integer.toBinaryString(tempInt);
					// Make sure the string has 5 digits to avoid string out of bounds error.
					for (int i = inBinary.length(); i < 5; i++) {
						inBinary = "0" + inBinary;
					}
					if (inBinary.charAt(0) == '1') {
						returnMessage = returnMessage + " Energie Zaehler 2,";
					}
					if (inBinary.charAt(1) == '1') {
						returnMessage = returnMessage + " Energie Zaehler 1,";
					}
					if (inBinary.charAt(2) == '1') {
						returnMessage = returnMessage + " DigExtern,";
					}
					if (inBinary.charAt(3) == '1') {
						returnMessage = returnMessage + " Uhr intern,";
					}
					if (inBinary.charAt(4) == '1') {
						returnMessage = returnMessage + " Can extern.";
					}
					if (returnMessage.charAt(returnMessage.length() - 1) == ',') {
						returnMessage = returnMessage.substring(0, returnMessage.length() - 1) + ".";
					}
					this.getElecGuidedSettings().setNextValue(returnMessage);
				} catch (NumberFormatException e) {
					this.logError(this.log, "Error, can't parse ElecGuidedSettings: " + e.getMessage());
					this.getElecGuidedSettings().setNextValue("Code " + stromAnforderung + ": Error deciphering code.");
				}
			}
			this.logDebug(this.log, "getElecGuidedSettings: " + this.getElecGuidedSettings().getNextValue().get());


			String arbeitElectr = "";
			arbeitElectr = readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitElektr=");
			try {
				this.getElectricWork().setNextValue(Double.parseDouble(arbeitElectr));
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse electrical work: " + e.getMessage());
				this.getElectricWork().setNextValue(0);
			}
			this.logDebug(this.log, "getElectricWork: " + this.getElectricWork().getNextValue().get().toString() + " kWh");


			String arbeitTherm = "";
			arbeitTherm = readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitThermHka=");
			try {
				this.getThermalWork().setNextValue(Double.parseDouble(arbeitTherm));
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse thermal work: " + e.getMessage());
				this.getThermalWork().setNextValue(0);
			}
			this.logDebug(this.log, "getThermalWork: " + this.getThermalWork().getNextValue().get().toString() + " kWh");


			String arbeitThermKon = "";
			arbeitThermKon = readEntryAfterString(serverMessage, "Hka_Bd.ulArbeitThermKon=");
			try {
				this.getThermalWorkCond().setNextValue(Double.parseDouble(arbeitThermKon));
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse thermal work condenser: " + e.getMessage());
				this.getThermalWorkCond().setNextValue(0);
			}
			this.logDebug(this.log, "getThermalWorkCond: " + this.getThermalWorkCond().getNextValue().get().toString() + " kWh");


			String runtimeSinceRestart = "";
			runtimeSinceRestart = readEntryAfterString(serverMessage, "Hka_Bd.ulBetriebssekunden=");
			try {
				this.getRuntimeSinceRestart().setNextValue(Double.parseDouble(runtimeSinceRestart));
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse runtime since restart: " + e.getMessage());
				this.getRuntimeSinceRestart().setNextValue(0);
			}
			this.logDebug(this.log, "getRuntimeSinceRestart: " + this.getRuntimeSinceRestart().getNextValue().get().toString() + " h");


			String drehzahl = "";
			drehzahl = drehzahl + readEntryAfterString(serverMessage, "Hka_Mw1.usDrehzahl=");
            try {
                this.getRpm().setNextValue(Integer.parseInt(drehzahl.trim()));
            } catch (NumberFormatException e) {
                this.logError(this.log, "Error, can't parse RPM: " + e.getMessage());
				this.getRpm().setNextValue(0);
            }
			this.logDebug(this.log, "getRpm: " + this.getRpm().getNextValue().get());


			String engineStarts = "";
			engineStarts = engineStarts + readEntryAfterString(serverMessage, "Hka_Bd.ulAnzahlStarts=");
			try {
				this.getEngineStarts().setNextValue(Integer.parseInt(engineStarts.trim()));
			} catch (NumberFormatException e) {
				this.logError(this.log, "Error, can't parse engine starts: " + e.getMessage());
				this.getEngineStarts().setNextValue(0);
			}
			this.logDebug(this.log, "getEngineStarts: " + this.getEngineStarts().getNextValue().get());


			String wartungFlag = readEntryAfterString(serverMessage, "Wartung_Cache.fStehtAn=");
			if (wartungFlag.equals("false")) {
				this.getMaintenanceFlag().setNextValue(false);
			} else {
				this.getMaintenanceFlag().setNextValue(true);
			}
			this.logDebug(this.log, "getMaintenanceFlag: " + this.getMaintenanceFlag().getNextValue().get().toString());

		} else {
		    this.logError(this.log, "Error: Couldn't read data from GLT interface.");
		}
	}


	// Separate method for these as they don't change and only need to be requested once.
    protected void getSerialAndPartsNumber() {
        String temp = getKeyDachs("k=Hka_Bd_Stat.uchSeriennummer&k=Hka_Bd_Stat.uchTeilenummer");
        if (temp.contains("Hka_Bd_Stat.uchSeriennummer=")) {
			this.getSerialNumber().setNextValue(readEntryAfterString(temp, "Hka_Bd_Stat.uchSeriennummer="));
            this.logDebug(this.log, "Seriennummer: " + this.getSerialNumber().getNextValue().get());
			this.getPartsNumber().setNextValue(readEntryAfterString(temp, "Hka_Bd_Stat.uchTeilenummer="));
            this.logDebug(this.log, "Teilenummer: " + this.getPartsNumber().getNextValue().get());
        } else {
            this.logInfo(this.log, "Error: Couldn't read data from GLT interface.");
        }
    }

    // Extract a value from the server return message. "stuff" is the return message from the server. "marker" is the value
	// after which you want to read. Reads until the end of the line.
    protected String readEntryAfterString(String stuff, String marker) {
        return stuff.substring(stuff.indexOf(marker) + marker.length(), stuff.indexOf("/n",stuff.indexOf(marker)));
	}


	// Send read request to server.
	protected String getKeyDachs(String key) {
		String message = "";
		try {
            URL url = new URL("http://" + urlBuilderIP + ":8081/getKey?" + key);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", basicAuth);
            is = connection.getInputStream();

            // Read text returned by server
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                this.logInfo(this.log, line);
                message = message + line + "/n";
            }
            reader.close();

		} catch (MalformedURLException e) {
			this.logInfo(this.log, "Malformed URL: " + e.getMessage());
		} catch (IOException e) {
			this.logInfo(this.log, "I/O Error: " + e.getMessage());
            if (e.getMessage().contains("code: 401")) {
                this.logInfo(this.log, "Wrong user/password. Access refused.");
            } else if (e.getMessage().contains("code: 404") || e.getMessage().contains("Connection refused")) {
                this.logInfo(this.log, "No GLT interface at specified address.");
            }
		} finally {
			if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    this.logInfo(this.log, "I/O Error: " + e.getMessage());
                }
            }
		}

		return message;
	}


	// Send write request to server.
	protected String setKeysDachs(String key) {
		String message = "";
		try {
			String body = key;

			URL url = new URL("http://" + urlBuilderIP + ":8081/setKeys");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Authorization", basicAuth);
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Length", String.valueOf(body.length()));

			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(body);
			writer.flush();
			writer.close();

			is = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = reader.readLine()) != null) {
				this.logInfo(this.log, line);
				message = message + line + "/n";
			}
			reader.close();

		} catch (MalformedURLException e) {
			this.logInfo(this.log, "Malformed URL: " + e.getMessage());
		} catch (IOException e) {
			this.logInfo(this.log, "I/O Error: " + e.getMessage());
		} finally {
			if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    this.logInfo(this.log, "I/O Error: " + e.getMessage());
                }
            }
		}

		return message;
	}

	protected void activateDachs() {
		String returnMessage = setKeysDachs("Stromf_Ew.Anforderung_GLT.bAktiv=1");
		this.logDebug(this.log, returnMessage);
		return;
	}

	protected void deactivateDachs() {
		String returnMessage = setKeysDachs("Stromf_Ew.Anforderung_GLT.bAktiv=0");
		this.logDebug(this.log, returnMessage);
		return;
	}

}
