package io.openems.edge.chp.ecpower.ro;

import org.osgi.service.event.EventHandler;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;

public interface XrgiRo extends ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		//ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		STOERUNG(Doc.of(OpenemsType.BOOLEAN)),
		BETRIEB(Doc.of(OpenemsType.BOOLEAN)),
		BHKW_EINSATZBEREIT(Doc.of(OpenemsType.BOOLEAN)),
		BHKW_NICHT_EINSATZBEREIT(Doc.of(OpenemsType.BOOLEAN)),
		MEHRERE_SPEICHER(Doc.of(OpenemsType.BOOLEAN)),
		SPEICHERFUEHLER_REIHENFOLGE_ERKANNT(Doc.of(OpenemsType.BOOLEAN)),

		// Temperatur (°C x100)
		SPEICHERTEMPERATUR_OBEN(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		SPEICHERTEMPERATUR_UNTEN(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		FM_VORLAUFTEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		FM_RUECKLAUFTEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		BHKW_NETZTEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		AUSSENTEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),

		// Leistung / Produktion
		AKTUELLE_ELEKTRO_PRODUKTION(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)), // kW x10
		AKTUELLE_WAERMEPRODUKTION(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		GESAMTE_STROMPRODUKTION(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh
		GESAMTE_WAERMEPRODUKTION(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh
		STROMPRODUKTION_15MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh
		WAERMEPRODUKTION_15MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh

		// Gas
		GESAMT_GASVERBRAUCH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh

		// Betriebsstunden etc.
		GESAMTE_BETRIEBSSTUNDEN(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
		BETRIEBSSTUNDEN_BIS_WARTUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
		LETZTER_FEHLERCODE(Doc.of(OpenemsType.INTEGER)),
		GESAMTE_GENERATOR_STARTS(Doc.of(OpenemsType.INTEGER)),

		// Temperaturen QW/FM (°C x100)
		QW_TMV_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		QW_TMK_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		QW_TLV_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		QW_TLK_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		QW_RUECKLAUF_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		OPERATIVER_SOLLWERT_TMV(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		FM_BYPASSTEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		FM_ZUFUHR_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		FM_SOLLWERT_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		FM_OP_SOLLWERT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),

		// Prozent (%)
		QW_MOTORKREISPUMPE(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		QW_SEKUNDAERPUMPE(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		FM_PUMPENLEISTUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		QW_VENTILSTELLUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		FM_VENTILSTELLUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),

		// Wärmemotor
		AKTUELLE_WAERMEPRODUKTION_MOTOR(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)), // kW x100
		WAERMEUEBERTRAGUNGSWERT(Doc.of(OpenemsType.INTEGER)), // kW/K x10
		TRENN_SCHICHTTEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),

		// Leistung/Leistungsgrenzen
		PU_ANGEFORDERTE_LEISTUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		PU_LEISTUNGSGRENZWERT(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		PU_ZIELLEISTUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		PU_MOTORLEISTUNG_VENTILSTELLUNG(Doc.of(OpenemsType.INTEGER)),
		PU_MAP_DRUCK(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIBAR)),
		PU_BRENNGAS_VENTILSTELLUNG(Doc.of(OpenemsType.INTEGER)),
		PU_ZUENDWINKEL(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		PU_MOTORBLOCKTEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		PU_DREHZAHL(Doc.of(OpenemsType.INTEGER)),

		// Spannung, Frequenz
		L1L2_PHASENSPANNUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
		L2L3_PHASENSPANNUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
		L3L1_PHASENSPANNUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
		NETZFREQUENZ(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ)),
		MELDESTATUS(Doc.of(OpenemsType.INTEGER)), 
		VPP_FREIGABE(Doc.of(OpenemsType.INTEGER)),
		BHKW_LEISTUNGSREGELUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		
		SET_POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_ONLY)),
		
		
		
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	


	
	
}
