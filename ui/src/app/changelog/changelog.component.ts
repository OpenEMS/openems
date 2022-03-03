import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';
import { Service } from '../shared/shared';
import { Role } from '../shared/type/role';
import { Changelog, Library, Product } from './changelog.constants';

@Component({
  selector: 'changelog',
  templateUrl: './changelog.component.html'
})
export class ChangelogComponent {

  public environment = environment;

  constructor(
    public translate: TranslateService,
    public service: Service,
    private route: ActivatedRoute,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Menu.changelog'), this.route);
  }

  public readonly roleIsAtLeast = Role.isAtLeast;
  public numberToRole(role: number): string {
    return Role[role].toLowerCase();
  }

  public readonly changelogs: {
    version: string,
    changes: Array<string | { roleIsAtLeast: Role, change: string }>
  }[] = [
      {
        version: '2022.3.2',
        changes: [
          Changelog.UI
        ]
      },
      {
        version: '2022.3.1',
        changes: [
          Changelog.openems('2022.3.0'),
          Changelog.UI + "Anzeigen/Ändern der Kontaktdaten im Benutzer-Menü",
          Changelog.UI + "Beim Login mit Benutzername/E-Mail-Adresse wird die Groß-/Kleinschreibung ignoriert",
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + "Überarbeitung des Widgets im Online-Monitoring",
          Changelog.product(Product.PRO_HYBRID_10) + "Kompatibilität mit KACO Firmware Version 8 (nur lesend)",
          Changelog.product(Product.FEMS_MODBUS_TCP_API) + "Auslesen On-Grid-/Off-Grid-Modus + Überarbeitung der Beschreibungen in der Excel-Datei",
          Changelog.product(Product.FEMS_PV_FRONIUS) + "Kompatibilität mit Fronius PV-Wechselrichtern. Getestet mit Fronius Symo",
          Changelog.library(Library.SLF4J, Library.POSTGRESQL, Library.GSON, Library.GUAVA, Library.NGX_FORMLY),
        ]
      },
      {
        version: '2022.2.3',
        changes: [
          Changelog.UI
        ]
      },
      {
        version: '2022.2.2',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent: Dynamische Begrenzung der Netzeinspeiseleistung in Österreich",
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + "Änderungen an der maximalen Netzeinspeiseleistung müssen durch erneutes Ausführen des Inbetriebnahmeassistenten gesetzt werden",
        ]
      },
      {
        version: '2022.2.1',
        changes: [
          Changelog.openems('2022.2.0'),
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + "Verbesserung der Einberechnung der Notstromvorhaltung und von DC-/Hybrid-Speichersystemen",
          Changelog.library(Library.FASTEXCEL, Library.SLF4J, Library.D3, Library.NGX_FORMLY, Library.APACHE_FELIX_CONFIGADMIN),
        ]
      },
      {
        version: '2022.1.3',
        changes: [
          Changelog.product(Product.FEMS_HOCHLASTZEITFENSTER) + "Kompatibilität des Hochlastzeitfensters mit Blindleistungsregelung",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent: Eingabe der Installateursdaten, externer Optimierer (Schattenmanagement)",
          Changelog.product(Product.HOME, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + "Möglichkeit zur Deaktivierung des MPP-Trackers bei externen Optimierern",
          Changelog.product(Product.HOME) + "Verbesserung des 'SMART'-Mode",
          { roleIsAtLeast: Role.ADMIN, change: "Verbesserung an FEMS System-Update Funktion" },
          { roleIsAtLeast: Role.ADMIN, change: "Bugfix OCPP-Server mit Java 11" },
        ]
      },
      {
        version: '2022.1.2',
        changes: [
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + "Verbesserung der historischen Darstellung über einen längeren Zeitraum",
        ]
      },
      {
        version: '2022.1.1',
        changes: [
          Changelog.openems('2022.1.0'),
          Changelog.product(Product.HOME, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + "Verbesserung des 'SMART'-Mode",
          Changelog.library(Library.RRD4J, Library.PAX_LOGGING, Library.GRADLE, Library.ANGULAR, Library.NGX_FORMLY, Library.IONIC, Library.DATE_FNS, Library.FASTEXCEL, Library.HIKARI_CP),
          "Ab dem Jahr 2022 steht die zweite Zahl in der Versionsnummer für den Monat des Releases; 2022.1.1 wurde also im Januar 2022 veröffentlicht",
          { roleIsAtLeast: Role.ADMIN, change: Changelog.EDGE + " Start Beta-Test App-Manager für FENECON Home" },
          { roleIsAtLeast: Role.ADMIN, change: Changelog.BACKEND + " Setze in Odoo Tag beim Partner, wenn dieser über IBN-Assistent angelegt wurde" }
        ]
      },
      {
        version: '2021.22.1',
        changes: [
          Changelog.openems('2021.22.0'),
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent für FEMS-App SG-Ready Wärmepumpe",
          Changelog.product(Product.PRO_HYBRID_10) + "Fehlerbehebung beim Erfassen der Seriennummer",
          "Implementierung Siemens PAC2200/3200/4200 Zähler",
          Changelog.library(Library.APACHE_FELIX_WEBCONSOLE, Library.PAX_LOGGING),
          "Aktualisierung auf Log4j Version 2 mit aktualiserten Sicherheitspatches. Vorher wurde Log4j in Version 1 genutzt, die für die kritische Schwachstelle an Log4j (CVE-2021-44228) ebenfalls nicht anfällig war.",
          { roleIsAtLeast: Role.ADMIN, change: Changelog.EDGE + " PV-Wechselrichter und DC-Laderegler können für Modbus/TCP-Slave-Api freigegeben werden" }
        ]
      },
      {
        version: '2021.21.5',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.library(Library.APACHE_FELIX_HTTP_JETTY, Library.APACHE_FELIX_FRAMEWORK, Library.D3, Library.DATE_FNS),
          { roleIsAtLeast: Role.ADMIN, change: "SimulatedESS: Modbus-Protokoll ist jetzt gleich wie GenericManagedSymmetricEss" },
        ]
      },
      {
        version: '2021.21.4',
        changes: [
          Changelog.library(Library.PAX_LOGGING) + " (CVE-2021-44228)"
        ]
      },
      {
        version: '2021.21.3',
        changes: [
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.HOME) + "Fehlerbehebung 'Q von U' Kurve",
          { roleIsAtLeast: Role.ADMIN, change: "Werbungswidgets im Monitoring werden nur noch für FENECON angezeigt, nicht für OEM (z. B. Heckert)" },
        ]
      },
      {
        version: '2021.21.2',
        changes: [
          Changelog.product(Product.PRO_HYBRID_10) + "Fehlerbehebung für Java 11",
        ]
      },
      {
        version: '2021.21.1',
        changes: [
          Changelog.openems('2021.21.0'),
          Changelog.UI + "Entferne Leerzeichen bei Login mit Benutzername und Passwort.",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.library(Library.JNA, Library.IONIC, Library.NGX_FORMLY, Library.DATE_FNS, Library.GRADLE),
          "Aktualisierung auf Java 11",
          { roleIsAtLeast: Role.ADMIN, change: "InfluxDB: erhöhe Timeout für Abfragen" },
        ]
      },
      {
        version: '2021.20.1',
        changes: [
          Changelog.openems('2021.20.0'),
          Changelog.product(Product.FEMS_CORRENTLY) + "In Abstimmung mit STOMDAO können jetzt 15-Minuten Werte genutzt werden.",
          Changelog.product(Product.FEMS_TIBBER) + "Release",
          Changelog.product(Product.DESS) + "Fehlerbehebung bei DC-PV-Leistung",
          "Fehlerbehebung bei Berechnung der Kapazität in einem Cluster aus Speichersystemen"
        ]
      },
      {
        version: '2021.19.6',
        changes: [
          Changelog.product(Product.HOME) + "Erkennung für FENECON Batteriewechselrichter",
        ]
      },
      {
        version: '2021.19.5',
        changes: [
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + Changelog.GENERAL_OPTIMIZATION,
        ]
      },
      {
        version: '2021.19.3',
        changes: [
          Changelog.UI,
          Changelog.product(Product.HOME, Product.PRO_HYBRID_10, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.FEMS_KEBA) + "Kein Fehler, wenn IP-Adresse Leerzeichen am Anfang oder Ende enthält",
        ]
      },
      {
        version: '2021.19.2',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2021.19.1',
        changes: [
          Changelog.openems('2021.19.0'),
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + Changelog.GENERAL_OPTIMIZATION + " (für Varianten 'Cluster B', 'Single C' und 'Cluster C')",
          Changelog.UI + " (Detail-Widgets)",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring (OEM)",
          Changelog.product(Product.COMMERCIAL_30) + " Überarbeitung der Implementierung für den Sinexcel Batteriewechselrichter",
          Changelog.library(Library.GSON, Library.POSTGRESQL, Library.OSGI_SERVICE_JDBC, Library.DATE_FNS, Library.D3, Library.INFLUXDB),
          { roleIsAtLeast: Role.ADMIN, change: "Beta-Test für neue FEMS System-Update Funktion" },
        ]
      },
      {
        version: '2021.18.1',
        changes: [
          Changelog.openems('2021.18.0'),
          Changelog.product(...Product.ALL_EVCS) + "Verbesserung der Kompatibilität mit nicht aktiv gesteuerten Stromspeichersytemen",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.FEMS_PQ_PLUS_ZAEHLER) + "Kompatibilität mit PQ Plus UMD96",
          "Fehlerbehebung in der Datenaufzeichnung phasengenauer Blindleistung",
          Changelog.library(Library.OSGI_UTIL_PROMISE, Library.OSGI_UTIL_FUNCTION, Library.POSTGRESQL, Library.GUAVA),
        ]
      },
      {
        version: '2021.17.1',
        changes: [
          Changelog.openems('2021.17.0'),
          Changelog.UI + "Permanentes Speichern der Spracheinstellung",
          Changelog.product(Product.FEMS_PV_SMA) + "Fehlerbehebung der Darstellung im Online-Monitoring",
          Changelog.product(Product.COMMERCIAL_30) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + "Darstellung im Online-Monitoring",
          Changelog.product(Product.FEMS_HARDY_BARTH) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.HOME, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + "Konfiguration des minimalen Ladezustands im Off-Grid-Fall; "
          + "Aufzeichnung des notstromversorgten Energieverbrauchs",
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + Changelog.GENERAL_OPTIMIZATION + " (für Variante 'Single C')",
        ]
      },
      {
        version: '2021.16.5',
        changes: [
          Changelog.UI,
          Changelog.product(Product.COMMERCIAL_30) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.HOME, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.EDGE + Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: "Implementierung ESS Cycle-Controller für Zyklus- und Kapazitätstests" },
        ]
      },
      {
        version: '2021.16.4',
        changes: [
          Changelog.product(Product.COMMERCIAL_30) + "Umstellung der Software-Architektur",
          {
            roleIsAtLeast: Role.ADMIN, change: "Ab dieser Version muss der Sinexcel Wechselrichter als Battery-Inverter angelegt werden; "
              + "je nach Anwendungsfall zusammen mit einem 'ESS Generic Off Grid' oder einem 'ESS Generic Managed Symmetric'"
          },
          Changelog.EDGE + "Fehlermeldung bei Modbus-Kommunikationsabbruch wird bei dem Gerät und nicht mehr bei der Modbus-Bridge angezeigt",
        ]
      },
      {
        version: '2021.16.3',
        changes: [
          Changelog.UI,
          Changelog.product(Product.FEMS_AWATTAR) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.PRO_HYBRID_10) + "Verbesserung der Fehleranalyse für die interne Eigenverbrauchsoptimierung",
          Changelog.product(Product.FEMS_SCHWELLWERT_STEUERUNG) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.FEMS_REST_JSON_API) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.HOME) + "Verbesserungen an der Notstromfunktion",
        ]
      },
      {
        version: '2021.16.2',
        changes: [
          Changelog.product(Product.FEMS_SCHWELLWERT_STEUERUNG) + "Parallele Ansteuerung mehrerer Relaisausgänge",
        ]
      },
      {
        version: '2021.16.1',
        changes: [
          Changelog.openems('2021.16.0'),
          Changelog.UI,
          Changelog.product(Product.HOME) + "Verbesserungen an der Notstromfunktion: Algorithmus zur Vorhaltung einer Notstromreserve; einstellbar via IBN-Assistent oder über das Widget 'Speichersystem' im Online-Monitoring",
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + "Verbesserung der Fehlermeldung bei Kommunikationsabbruch (Cluster Version B)",
          Changelog.library(Library.GRADLE),
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.HOME) + "Fehlerbehebung bei der Start-Prozedur",
          { roleIsAtLeast: Role.ADMIN, change: "Die 'openems.jar'-Datei für jeden Branch wird jetzt automatisch bei jedem 'push' gebaut. Download unter 'https://dev.intranet.fenecon.de/{branch}/openems.jar'" },
          { roleIsAtLeast: Role.ADMIN, change: "Mit einer Konfigurationseinstellungen in der 'Sum'-Komponente können jetzt Warnungen und Fehler des Gesamtsystems ignoriert/ausgeblendet werden" },
          { roleIsAtLeast: Role.ADMIN, change: "Der Debug-Log-Controller kann so konfiguriert werden, dass er auch den Alias mit ausgibt" },
        ]
      },
      {
        version: '2021.15.12',
        changes: [
          Changelog.product(Product.HOME, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + "Verbrauchszähler für notstromversorgte Lasten ('GoodWe.EmergencyPowerMeter'); Bei aktiviertem Notstrom aktiviere auch Schwarzstart-Funktion",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.product(Product.HOME) + "Optimierung der Start-Prozedur und des Fehlerhandlings der Batterie",
          Changelog.UI + "Link korrigiert zu " + Changelog.product(Product.FEMS_HEIZSTAB),
          Changelog.product(...Product.ALL_ESS) + Changelog.BATTERY_PROTECTION,
          Changelog.EDGE + "Fehlerbehebung am lokalen Monitoring",
          Changelog.library(Library.APACHE_FELIX_SCR, Library.FASTEXCEL),
        ]
      },
      {
        version: '2021.15.11',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2021.15.10',
        changes: [
          Changelog.openems('2021.15.0'),
          Changelog.product(Product.HOME, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + "Prüfung im 'SMART'-Mode, ob das erforderliche GoodWe Smart-Meter verbunden ist",
        ]
      },
      {
        version: '2021.15.9',
        changes: [
          Changelog.product(Product.FEMS_MODBUS_TCP_API) + "Verbessertes Fehlerhandling bei ungültigen Anfragen; neuer Fehlerzustand 'Fault in Process Image'",
        ]
      },
      {
        version: '2021.15.8',
        changes: [
          Changelog.product(Product.HOME) + "Prüfung im 'SMART'-Mode, ob das erforderliche GoodWe Smart-Meter verbunden ist",
        ]
      },
      {
        version: '2021.15.7',
        changes: [
          Changelog.UI,
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.product(Product.FEMS_REST_JSON_API) + "Optimierung bei Verwendung regulärer Ausdrücke",
          Changelog.product(Product.INDUSTRIAL) + "Release ESS Standby-Controller",
          Changelog.EDGE + "Fehlerbehebung nach Update der Apache Felix Webconsole",
          Changelog.library(Library.APACHE_FELIX_HTTP_JETTY),
          { roleIsAtLeast: Role.ADMIN, change: "Entfernen von KACO50.ESS aus dem Release" },
        ]
      },
      {
        version: '2021.15.6',
        changes: [
          Changelog.EDGE + "Fehlerbehebung bei Datenkonvertierung sehr großer Zahlen",
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + "Verbesserung der Aufzeichnung von Zellspannungen",
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.FEMS_HARDY_BARTH) + "Verwende werkseitige Standard-IP 192.168.25.30",
          Changelog.product(Product.HOME, Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.library(Library.APACHE_FELIX_WEBCONSOLE),
        ]
      },
      {
        version: '2021.15.5',
        changes: [
          Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: Changelog.product(Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + "Fehlerbehebung für übersteuerte Systeme; setze standardmäßig auf 'INTERNAL'- anstelle von 'SMART'-Mode" },
        ]
      },
      {
        version: '2021.15.4',
        changes: [
          Changelog.UI + "Link korrigiert zu " + Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG),
          { roleIsAtLeast: Role.ADMIN, change: Changelog.product(Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + "Fehlerbehebung für übersteuerte Systeme" },
          { roleIsAtLeast: Role.ADMIN, change: Changelog.EDGE + "Änderung der Berechtigung zum Setzen von Netzwerkeinstellungen auf 'owner'; notwendig für IBN-Assistent Heckert bei FEMS-Apps" },
        ]
      },
      {
        version: '2021.15.3',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring (Konfiguration AC-PV-Zähler)",
        ]
      },
      {
        version: '2021.15.2',
        changes: [
          Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: "Version übersprungen" },
        ]
      },
      {
        version: '2021.15.1',
        changes: [
          Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: "Version übersprungen" },
        ]
      },
      {
        version: '2021.14.1',
        changes: [
          Changelog.openems('2021.14.0'),
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.UI + "Neustrukturierung des Anlagenprofils",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.library(Library.CHARGETIME_OCPP, Library.GSON, Library.JNA, Library.PAX_LOGGING, Library.APACHE_FELIX_WEBCONSOLE),
          { roleIsAtLeast: Role.ADMIN, change: "OEM-Version des Online-Monitorings für Heckert: " + Changelog.link("Link", 'https://symphon-e.heckert-solar.com/') },
        ]
      },
      {
        version: '2021.13.10',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.EDGE + "Fehlerbehebung nach Update des OSGi-Frameworks",
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung bei Prüfung auf fehlerhaftes, doppeltes Mapping von Modbus-Register zu Channel" },
          { roleIsAtLeast: Role.ADMIN, change: "UI-Entwicklungen werden per Continuous Integration immer sofort unter https://dev.intranet.fenecon.de/feature/ui- ... zur Verfügung gestellt" },
        ]
      },
      {
        version: '2021.13.9',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.library(Library.D3, Library.NGX_COOKIE_SERVICE, Library.MYDATEPICKER),
          "Similar-Day Predictor. Übernahme aus dem Forschungsprojekt " + Changelog.link("EMSIG", 'https://openems.io/research/emsig/'),
          Changelog.EDGE + "Fehlerbehebung am lokalen Monitoring",
          Changelog.BACKEND + "Spracheinstellungen für Benutzer",
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung bei Prüfung auf fehlerhaftes, doppeltes Mapping von Modbus-Register zu Channel" },
          { roleIsAtLeast: Role.ADMIN, change: Changelog.UI + "Werbungs-Widget: Ändere E-Mail zu partner@fenecon.de" },
        ]
      },
      {
        version: '2021.13.8',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
        ]
      },
      {
        version: '2021.13.7',
        changes: [
          Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: Changelog.EDGE + "Ändere SystemUpdate zu neuem Paket 'fems'; 'openems-core' und 'openems-core-fems' sind obsolet" },
          { roleIsAtLeast: Role.ADMIN, change: Changelog.UI + "Reaktivierung des Werbungs-Widgets" },
        ]
      },
      {
        version: '2021.13.6',
        changes: [
          Changelog.product(...Product.ALL_EVCS) + "Fehlerbehebung bei Limitierung der maximalen Energie eines Ladevorgangs",
          Changelog.UI + Changelog.GENERAL_OPTIMIZATION + "Jahresansicht in der Historie",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          { roleIsAtLeast: Role.ADMIN, change: Changelog.product(Product.FEMS_KEBA, Product.FEMS_HEIZSTAB) + "FEMS-App Assistant setzt zusätzliche IP-Adresse am FEMS jetzt richtig" },
        ]
      },
      {
        version: '2021.13.5',
        changes: [
          Changelog.UI + "Jahresansicht in der Historie",
        ]
      },
      {
        version: '2021.13.4',
        changes: [
          Changelog.product(Product.PRO_HYBRID_10) + "Fehlerbehebung bei ungültiger Seriennummer oder Version des Wechselrichters",
        ]
      },
      {
        version: '2021.13.3',
        changes: [
          Changelog.EDGE + "Datenkonvertierung von Array-Werten",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.product(Product.FEMS_JANITZA_ZAEHLER) + Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: "Refactoring von ESS Cluster: sofortige Übernahme von Channel-Werten, Berechnung gewichteter SoC anhand der Kapazität" },
          { roleIsAtLeast: Role.ADMIN, change: "Switch zu Apache Felix Framework" },
        ]
      },
      {
        version: '2021.13.2',
        changes: [
          Changelog.UI + "Übersetzungen",
          Changelog.UI + "Portal zur Registrierung als Benutzer",
          Changelog.library(Library.SLF4J)
        ]
      },
      {
        version: '2021.13.1',
        changes: [
          Changelog.openems('2021.13.0'),
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.FEMS_JANITZA_ZAEHLER) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.GENERAL_OPTIMIZATION + " an Beta-Version für go-e Charger Home",
          Changelog.EDGE + "Verarbeitung 'read-only' Modus von Speichersystemen",
          Changelog.library(Library.DATE_FNS, Library.ANGULAR, Library.IONIC, Library.NGX_COOKIE_SERVICE, Library.RXJS)
        ]
      },
      {
        version: '2021.12.6',
        changes: [
          Changelog.BACKEND + "Spracheinstellungen für Benutzer",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.product(...Product.ALL_EVCS) + Changelog.GENERAL_OPTIMIZATION,
          "Beta-Version für go-e Charger Home",
        ]
      },
      {
        version: '2021.12.5',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent über das Online-Monitoring",
        ]
      },
      {
        version: '2021.12.4',
        changes: [
          Changelog.library(Library.CHARGETIME_OCPP)
        ]
      },
      {
        version: '2021.12.3',
        changes: [
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + Changelog.GENERAL_OPTIMIZATION
        ]
      },
      {
        version: '2021.12.2',
        changes: [
          Changelog.UI + "Portal zur Registrierung als Benutzer",
          Changelog.product(Product.HOME) + "Release Inbetriebnahmeassistent über das Online-Monitoring",
          Changelog.product(Product.FEMS_JANITZA_ZAEHLER) + Changelog.GENERAL_OPTIMIZATION,
        ]
      },
      {
        version: '2021.12.1',
        changes: [
          Changelog.UI,
          Changelog.BACKEND + "Inbetriebnahmeassistent",
          Changelog.product(Product.COMMERCIAL_BYD) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.FEMS_PV_SMA) + "Kompatibilität mit SMA Sunny Tripower 8.0 und 10.0",
          { roleIsAtLeast: Role.ADMIN, change: "Prüfung auf fehlerhaftes, doppeltes Mapping von Modbus-Register zu Channel" },
          Changelog.library(Library.POSTGRESQL, Library.UUID, Library.ECLIPSE_OSGI, Library.GSON, Library.NG2_CHARTS, Library.NGX_FORMLY, Library.MYDATEPICKER)
        ]
      },
      {
        version: '2021.11.3',
        changes: [
          Changelog.UI,
          { roleIsAtLeast: Role.ADMIN, change: "FixActivePower-Controller reagiert schneller auf Änderungen (via @Modified)" }
        ]
      },
      {
        version: '2021.11.2',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2021.11.1',
        changes: [
          Changelog.openems('2021.11.0'),
          Changelog.library(Library.MSGPACK, Library.FASTEXCEL, Library.APACHE_FELIX_INVENTORY, Library.APACHE_FELIX_WEBCONSOLE,
            Library.ANGULAR, Library.IONIC, Library.NGX_FORMLY, Library.D3, Library.DATE_FNS, Library.NGX_COOKIE_SERVICE, Library.NGX_SPINNER, Library.RXJS),
          { roleIsAtLeast: Role.ADMIN, change: "Möglichkeit zum Export aller aktuellen Channel-Werte als Excel-Datei. Link im Anlagenprofil unter der jeweiligen Komponente." },
          { roleIsAtLeast: Role.ADMIN, change: "Entfernen von Refu.ESS und Streetscooter.ESS aus dem Release" },
          { roleIsAtLeast: Role.ADMIN, change: "Downgrade Soltaro Single A 'Connector Wire Fault' zu Level 'Warnung'" }
        ]
      },
      {
        version: '2021.10.9',
        changes: [
          Changelog.UI + "Anpassungen für neue FENECON Corporate Identity",
          Changelog.EDGE + "Vereinheitlichung von AC, DC und Hybrid-Speichern",
          Changelog.product(Product.FEMS_MODBUS_TCP_API) + "Bereitstellung von DC-Leistung und -Energie für Hybrid-Speicher",
          Changelog.library(Library.GRADLE, Library.APACHE_FELIX_HTTP_JETTY, Library.POSTGRESQL),
        ]
      },
      {
        version: '2021.10.8',
        changes: [
          Changelog.product(Product.DESS) + "Erfassung Ladezustand"
        ]
      },
      {
        version: '2021.10.7',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2021.10.6',
        changes: [
          Changelog.BACKEND + "Inbetriebnahmeassistent"
        ]
      },
      {
        version: '2021.10.5',
        changes: [
          Changelog.UI,
          Changelog.EDGE + "Datenkonvertierung von String-Werten",
          "Simulator: Verbesserung der Ladezustandsberechnung"
        ]
      },
      {
        version: '2021.10.4',
        changes: [
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + Changelog.GENERAL_OPTIMIZATION
        ]
      },
      {
        version: '2021.10.3',
        changes: [
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + Changelog.GENERAL_OPTIMIZATION
        ]
      },
      {
        version: '2021.10.2',
        changes: [
          Changelog.EDGE + "Datenkonvertierung von Boolean-Werten"
        ]
      },
      {
        version: '2021.10.1',
        changes: [
          Changelog.openems('2021.10.0'),
          Changelog.UI,
          Changelog.EDGE,
          Changelog.library(Library.IONIC, Library.NGX_FORMLY, Library.MYDATEPICKER, Library.D3, Library.DATE_FNS, Library.NGX_SPINNER, Library.RXJS, Library.GRADLE),
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + "Release"
        ]
      },
      {
        version: '2021.9.3',
        changes: [
          Changelog.UI,
          Changelog.product(Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.library(Library.GRADLE),
        ]
      },
      {
        version: '2021.9.2',
        changes: [
          Changelog.UI,
          Changelog.EDGE + "Erfassung lokaler Hostname",
        ]
      },
      {
        version: '2021.9.1',
        changes: [
          Changelog.openems('2021.9.0'),
          Changelog.EDGE + "lokale Netzwerkkonfiguration, verbesserte Zugriffskontrolle",
          "Implementierung SOCOMEC Countis E34",
          { roleIsAtLeast: Role.ADMIN, change: "Lokaler Login als 'admin' nur noch mit FEMS-spezifischem Passwort" }
        ]
      },
      {
        version: '2021.8.9',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2021.8.8',
        changes: [
          Changelog.UI,
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION
        ]
      },
      {
        version: '2021.8.7',
        changes: [
          Changelog.UI,
          Changelog.library(Library.PAX_LOGGING),
          Changelog.GENERAL_OPTIMIZATION + " an Beta-Version für Anbindung an MQTT-Broker",
        ]
      },
      {
        version: '2021.8.6',
        changes: [
          Changelog.UI
        ]
      },
      {
        version: '2021.8.5',
        changes: [
          Changelog.BACKEND + "Login und Session-Handling",
          Changelog.library(Library.GUAVA, Library.RETROFIT, Library.POSTGRESQL, Library.FASTEXCEL),
        ]
      },
      {
        version: '2021.8.4',
        changes: [
          "Beta-Version für Anbindung an MQTT-Broker",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION
        ]
      },
      {
        version: '2021.8.3',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.8.2',
        changes: [
          Changelog.GENERAL_OPTIMIZATION
        ]
      },
      {
        version: '2021.8.1',
        changes: [
          Changelog.openems('2021.8.0'),
          Changelog.EDGE,
          Changelog.UI,
          Changelog.library(Library.GRADLE),
        ]
      },
      {
        version: '2021.7.8',
        changes: [
          Changelog.EDGE,
        ]
      },
      {
        version: '2021.7.7',
        changes: [
          Changelog.UI + "Information zum Gesamtsystemstatus",
        ]
      },
      {
        version: '2021.7.6',
        changes: [
          Changelog.BACKEND,
          Changelog.library(Library.NGX_COOKIE_SERVICE),
        ]
      },
      {
        version: '2021.7.5',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " an Batteriewechselrichter-Implementierung (adaptive Berechnung maximalen AC-Leistung)",
        ]
      },
      {
        version: '2021.7.4',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2021.7.3',
        changes: [
          Changelog.UI,
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + Changelog.BATTERY_PROTECTION,
          Changelog.GENERAL_OPTIMIZATION + " bei Konfigurationsänderungen über das Online-Monitoring"
        ]
      },
      {
        version: '2021.7.2',
        changes: [
          Changelog.BACKEND,
          Changelog.product(Product.HOME) + Changelog.BATTERY_PROTECTION,
        ]
      },
      {
        version: '2021.7.1',
        changes: [
          Changelog.openems('2021.7.0'),
          Changelog.UI,
          Changelog.product(Product.PRO_HYBRID_10) + "Verarbeitung von Authentifizierungsfehlern, Seriennummern und adaptive Berechnung der Maximalleistung",
          Changelog.product(...Product.ALL_ESS) + Changelog.BATTERY_PROTECTION,
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + "Adaptive Berechnung der Kapazität (für Variante 'Single B')",
          "Refactoring des ESS Linear Power Band Controller ('Controller.Ess.LinearPowerBand')",
          Changelog.BACKEND,
          Changelog.library(Library.ANGULAR, Library.IONIC, Library.DATE_FNS, Library.MYDATEPICKER, Library.D3, Library.NGX_FORMLY, Library.NG2_CHARTS, Library.NGX_SPINNER,
            Library.HIKARI_CP, Library.ECLIPSE_OSGI, Library.APACHE_FELIX_HTTP_JETTY, Library.JNA, Library.MOSHI, Library.JAVA_WEBSOCKET)
        ]
      },
      { //
        version: '2021.6.2',
        changes: [
          "Online-Monitoring: Produktinformation über das " + Changelog.link("Heimatstrom", 'https://regionalwerke.com/pv/reststrom') + "-Angebot der Regionalwerke"
        ]
      },
      {
        version: '2021.6.1',
        changes: [
          Changelog.openems('2021.6.0'),
        ]
      },
      {
        version: '2021.5.2',
        changes: [
          Changelog.UI,
          Changelog.library(Library.FASTEXCEL),
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " an Batterie-Implementierung (adaptive Berechnung der Kapazität)",
        ]
      },
      {
        version: '2021.5.1',
        changes: [
          Changelog.openems('2021.5.0'),
          Changelog.BACKEND,
          Changelog.library(Library.ANGULAR, Library.IONIC),
          Changelog.GENERAL_OPTIMIZATION + " an OneWire-Implementierung",
          "Optimierungen an der lokalen Zeitreihendatenbank ('RRD4j')",
        ]
      },
      {
        version: '2021.4.16',
        changes: [
          "Performance-Optimierung am Backend für das Online-Monitoring"
        ]
      },
      {
        version: '2021.4.15',
        changes: [
          "Performance-Optimierung am Backend für das Online-Monitoring"
        ]
      },
      {
        version: '2021.4.14',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " an Batteriewechselrichter- und Batterie-Implementierung",
        ]
      },
      {
        version: '2021.4.13',
        changes: [
          "Implementierung des FEMS-Relais 4-Kanal",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " an Batteriewechselrichter- und Batterie-Implementierung",
          Changelog.GENERAL_OPTIMIZATION + " bei Konfigurationsänderungen über das Online-Monitoring"
        ]
      },
      {
        version: '2021.4.12',
        changes: [
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + "Fehlerbehebung bei Aufzeichnung der Zelltemperaturen",
          "Optimierung der Anbindung an den Backend-Server ('Controller.Backend.Api')",
        ]
      },
      {
        version: '2021.4.11',
        changes: [
          Changelog.product(Product.PRO_HYBRID_10) + "Verbesserung der Fehlermeldung bei Kommunikationsabbruch",
          "Optimierung der Anbindung an den Backend-Server ('Controller.Backend.Api')",
        ]
      },
      {
        version: '2021.4.10',
        changes: [
          "Optimierung der Anbindung an den Backend-Server ('Controller.Backend.Api')",
        ]
      },
      {
        version: '2021.4.9',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.4.8',
        changes: [
          Changelog.UI,
          Changelog.BACKEND,
          "Optimierung der Anbindung an den Backend-Server ('Controller.Backend.Api')",
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + Changelog.BATTERY_PROTECTION
        ]
      },
      {
        version: '2021.4.7',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.4.6',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.4.5',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.4.4',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.4.3',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.4.2',
        changes: [
          "Optimierungen an der lokalen Zeitreihendatenbank ('RRD4j')",
          "Optimierung der Anbindung an den Backend-Server ('Controller.Backend.Api')",
        ]
      },
      {
        version: '2021.4.1',
        changes: [
          Changelog.openems('2021.4.0'),
        ]
      },
      {
        version: '2021.3.4',
        changes: [
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + Changelog.BATTERY_PROTECTION
        ]
      },
      {
        version: '2021.3.3',
        changes: [
          Changelog.GENERAL_OPTIMIZATION,
        ]
      },
      {
        version: '2021.3.2',
        changes: [
          Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: "Service-Assistent für Soltaro-Batterien Version C" }
        ]
      },
      {
        version: '2021.3.1',
        changes: [
          Changelog.openems('2021.3.0'),
          Changelog.product(Product.INDUSTRIAL) + Changelog.GENERAL_OPTIMIZATION + " für BMW i3-Batterien",
          Changelog.library(Library.FASTEXCEL, Library.ECLIPSE_OSGI, Library.APACHE_FELIX_METATYPE, Library.ANGULAR, Library.DATE_FNS),
          "Kundeninformation über mögliches Update der BYD Battery Box Premium HVS"
        ]
      },
      {
        version: '2021.2.1',
        changes: [
          Changelog.openems('2021.2.0'),
          Changelog.library(Library.FASTEXCEL, Library.HIKARI_CP),
          { roleIsAtLeast: Role.ADMIN, change: "Service-Assistent für Soltaro-Batterien" }
        ]
      },
      {
        version: '2021.1.13',
        changes: [
          Changelog.product(...Product.ALL_ESS) + Changelog.BATTERY_PROTECTION
        ]
      },
      {
        version: '2021.1.12',
        changes: [
          Changelog.product(Product.COMMERCIAL_50) + "Stabilitätsverbesserung bei Kommunikationsabbruch"
        ]
      },
      {
        version: '2021.1.11',
        changes: [
          Changelog.product(Product.COMMERCIAL_50, Product.FEMS_PV_KACO) + "Korrektor von Rundungsfehlern bei der Vorgabe von Set-Points"
        ]
      },
      {
        version: '2021.1.10',
        changes: [
          Changelog.GENERAL_OPTIMIZATION
        ]
      },
      {
        version: '2021.1.9',
        changes: [
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + Changelog.GENERAL_OPTIMIZATION
        ]
      },
      {
        version: '2021.1.8',
        changes: [
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50, Product.COMMERCIAL_BYD) + Changelog.BATTERY_PROTECTION
        ]
      },
      {
        version: '2021.1.7',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.1.6',
        changes: [
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + "Fehlerbehebung bei Aufzeichnung der Zelltemperaturen",
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.1.5',
        changes: [
          Changelog.product(Product.COMMERCIAL_50) + "Korrektur der Anzeige im Monitoring bei manuellem Stop des Batteriewechselrichters",
          Changelog.UI
        ]
      },
      {
        version: '2021.1.4',
        changes: [
          Changelog.product(Product.FEMS_KEBA) + "Aufzeichnung der Energiedaten zur E-Auto-Beladung"
        ]
      },
      {
        version: '2021.1.3',
        changes: [
          "Stabilitätsverbesserung bei Kommunikationsabbruch zu Peripheriegeräten"
        ]
      },
      {
        version: '2021.1.2',
        changes: [
          Changelog.BACKEND
        ]
      },
      {
        version: '2021.1.1',
        changes: [
          Changelog.openems('2021.1.0')
        ]
      }
    ];

}