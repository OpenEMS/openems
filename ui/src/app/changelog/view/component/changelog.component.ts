import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';
import { Service } from '../../../shared/shared';
import { Role } from '../../../shared/type/role';
import { Changelog, Library, OpenemsComponent, Product } from './changelog.constants';

@Component({
  selector: 'changelog',
  templateUrl: './changelog.component.html'
})
export class ChangelogComponent implements OnInit {

  public environment = environment;

  protected slice: number = 10;
  protected showAll: boolean = false;
  constructor(
    public translate: TranslateService,
    public service: Service,
    private route: ActivatedRoute,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent({ languageKey: 'Menu.changelog' }, this.route);
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
        version: '2023.3.4',
        changes: [
          Changelog.UI,
          "Verbesserung am Changelog: Anzeige im Bereich \"Systemupdate\" und verbessertes Layout",
          { roleIsAtLeast: Role.INSTALLER, change: Changelog.product(Product.HOME) + "Vorbereitung einer Batterie-Erweiterung (Be-/Entladen auf 30 % SoC) über das Speicher-Widget im Online-Monitoring" },
          Changelog.product(Product.FEMS_SG_READY_WAERMEPUMPE) + "Verbesserung der Fehlerprüfung bei Konfigurationsänderungen",
          { roleIsAtLeast: Role.ADMIN, change: "Beta-Release Webasto Next Ladestation" },
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Optimierung Validierung kostenloser Apps, Verbesserung bei Default-Werten, Hardy-Barth mit zwei Ladepunkten" },
          { roleIsAtLeast: Role.ADMIN, change: "Backend: historische Abfragen auffüllen mit 'null'-Werten, retry by InfluxDB Schreibfehlern, bessere Logs" },
          Changelog.library(Library.POSTGRESQL, Library.APACHE_FELIX_FILEINSTALL),
        ]
      },
      {
        version: '2023.3.3',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2023.3.2',
        changes: [
          "Fehlerbehebung in der historischen Ansicht im lokalen Monitoring",
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + "Fehlerbehebung bei der Datenanzeige in der Live-Ansicht",
          "Umfangreiche Performance-Optimierungen bei historischen Daten",
          "Fehlerbehebung/Verbesserung bei der Datenübertragung zum Backend",
          { roleIsAtLeast: Role.ADMIN, change: "Beta-Release Webasto Unite Ladestation" },
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Umbenennung FEMS Relaisboard 8-Kanal TCP, Berechtigungsproblem bei Validierung von Keys im Backend, Auswahl der Phase bei SMA PV-Wechselrichter, Verbesserung handling kostenloser Apps" },
          { roleIsAtLeast: Role.ADMIN, change: "UI: neues CI für Heckert" },
          { roleIsAtLeast: Role.ADMIN, change: "Alpha-Test für PWA Service-Worker" },
          Changelog.library(Library.FASTEXCEL),
        ]
      },
      {
        version: '2023.3.1',
        changes: [
          Changelog.openems('2023.3.0'),
          "Fehlerbehebung/Verbesserung bei der Datenübertragung zum Backend",
          Changelog.library(Library.FASTEXCEL, Library.APACHE_FELIX_HTTP_JETTY),
        ]
      },
      {
        version: '2023.2.11',
        changes: [
          "Fehlerbehebung/Verbesserung bei der Datenübertragung zum Backend",
        ]
      },
      {
        version: '2023.2.9',
        changes: [
          Changelog.UI,
          "Reduzierung der Datenübertragung zum Backend",
          { roleIsAtLeast: Role.ADMIN, change: "Fix-State-of-Charge-/Prepare-Battery-Extension-Controller: Änderung des Datum-Formats" },
          { roleIsAtLeast: Role.ADMIN, change: "UI: Routing-Fehler, Blockieren auf Übersicht-Seite, Browser-Kompatibilität" },
          { roleIsAtLeast: Role.ADMIN, change: "UI: Feldtest für neuen Time-Of-Use Controller" },
          { roleIsAtLeast: Role.ADMIN, change: "Backend: Handling von aggregierten Daten, InfluxDB adaptives Lese-/Schreib-Limit, Fehlerbehebung bei Logout" },
          { roleIsAtLeast: Role.ADMIN, change: "FENECON Home-App: Länder Schweden, Tschechien und Niederlande" },
        ]
      },
      {
        version: '2023.2.6',
        changes: [
          Changelog.UI + "Fehlerbehebung beim Reconnect der Websocket-Verbindung",
          Changelog.library(Library.POSTGRESQL, Library.GRADLE),
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Verbesserungen an Keys" },
          { roleIsAtLeast: Role.ADMIN, change: "Beta-Release Alpitronic Hypercharger DC" },
          { roleIsAtLeast: Role.ADMIN, change: "Bugfix Meter.Virtual.Symmetric.Add: Energiewerte" },
          { roleIsAtLeast: Role.ADMIN, change: "Bugfix SolarEdge Grid-Meter (SunSpec Meter): Energie je Phase" },
          { roleIsAtLeast: Role.ADMIN, change: "IBN-Assistent/Backend: Verbesserung der Fehlerhandlings" },
        ]
      },
      {
        version: '2023.2.5',
        changes: [
          Changelog.UI,
          "Netzwerkkonfiguration: Fehlerbehebung bei Benutzernamen mit Umlauten",
        ]
      },
      {
        version: '2023.2.4',
        changes: [
          Changelog.UI,
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Start Demo-Test, Meldung freie Digital-/Relaisausgänge" },
        ]
      },
      {
        version: '2023.2.3',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2023.2.2',
        changes: [
          Changelog.UI,
          Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent",
          Changelog.product(Product.FEMS_TIBBER) + "Fehlerbehebungen bei mehreren registrierten Zählern",
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Verbesserungen an Keys, Navigation, Übersetzungen" },
          { roleIsAtLeast: Role.ADMIN, change: "Fix-State-of-Charge-/Prepare-Battery-Extension-Controller: Änderung der Datums-Konfiguration, Automatische Installation auf Home-Systemen" },
          Changelog.library(Library.POSTGRESQL, Library.FASTEXCEL),
        ]
      },
      {
        version: '2023.2.1',
        changes: [
          Changelog.openems('2023.2.0'),
          Changelog.product(Product.COMMERCIAL_30) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent",
          Changelog.product(Product.FEMS_REST_JSON_API) + Changelog.GENERAL_OPTIMIZATION,
          Changelog.library(Library.ANGULAR, Library.NGX_FORMLY, Library.IONIC, Library.D3),
        ]
      },
      {
        version: '2023.1.3',
        changes: [
          Changelog.GENERAL_OPTIMIZATION + " an der Übersicht 'Alle Systeme'",
          "Fehlerbehebung am Online-Monitoring: Anzeige der Live-Daten nach Aktualisierung der Seite im Browser",
        ]
      },
      {
        version: '2023.1.2',
        changes: [
          Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Aktiviere App-Keys" },
          { roleIsAtLeast: Role.ADMIN, change: "Beta-Release Fix-State-of-Charge-/Prepare-Battery-Extension-Controller" },
        ]
      },
      {
        version: '2023.1.1',
        changes: [
          Changelog.openems('2023.1.0'),
          Changelog.product(Product.HOME) + "Verbesserungen Inbetriebnahmeassistent für KDK-Zähler (Modbus/RTU Parität)",
          Changelog.product(Product.HOME) + "Verbesserung SMART-Mode bei Vorgabe einer maximalen Be-/Entladeleistung",
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + "Historie-Chart: Skalierung der Preis-Achse",
          Changelog.library(Library.NGX_FORMLY, Library.IONIC, Library.JNA, Library.D3),
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Verbesserung der Geschwindigkeit, automatische Installation von Abhängigkeiten, UI-Verbesserungen, Schreibzugriff-Apps, Übersetzungen/Begriffe" },
        ]
      },
      {
        version: '2022.12.7',
        changes: [
          "Inbetriebnahmeassistent: Fehlerbehebung",
        ]
      },
      {
        version: '2022.12.6',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2022.12.5',
        changes: [
          Changelog.product(Product.PRO_HYBRID_GW, Product.PRO_HYBRID_AC_GW) + "Fehlerbehebung bei Kompatibilität mit älterer Firmware des GoodWe Wechselrichters",
          { roleIsAtLeast: Role.ADMIN, change: "Besseres Logging von UI-Fehlern im Backend, z. B. für IBN" },
        ]
      },
      {
        version: '2022.12.4',
        changes: [
          Changelog.UI,
          "Inbetriebnahmeassistent: Fehlerbehebung",
          Changelog.library(Library.ANGULAR, Library.IONIC, Library.NGX_FORMLY, Library.NGX_COOKIE_SERVICE, Library.D3, Library.FASTEXCEL, Library.OSGI_UTIL_PROMISE),
        ]
      },
      {
        version: '2022.12.3',
        changes: [
          Changelog.UI,
          "Inbetriebnahmeassistent: Fehlerbehebung"
        ]
      },
      {
        version: '2022.12.2',
        changes: [
          "Implementierung KDK 420506PRO20-U (2PU CT) Zähler"
        ]
      },
      {
        version: '2022.12.1',
        changes: [
          Changelog.openems('2022.12.0'),
          Changelog.UI + "Verbesserung Login/Session-Management",
          "Verbesserungen Inbetriebnahmeassistent Commercial: englische Übersetzung und Unterstützung KDK-Zähler",
          Changelog.product(Product.FEMS_KEBA) + "Anzeige Ladeleistung und Status am Display",
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Vorbereitungen für Key-Modell" },
          { roleIsAtLeast: Role.ADMIN, change: "UI: Verhindere mehrfaches Klicken des Login-Button; unterscheide Authentifizierungs-Fehler vs. Odoo-Timeout" },
          { roleIsAtLeast: Role.ADMIN, change: "ESS-Cluster: Unterstützung für Start-Stop von mehreren ESS" },
          { roleIsAtLeast: Role.ADMIN, change: Changelog.product(Product.HOME) + "Korrektur bei Frequenzmessung des Netzzählers" },
          { roleIsAtLeast: Role.ADMIN, change: "Backend Alerting: Verhinderung von negativen Seiteneffekten beim Abarbeiten von Meldungen" },
          { roleIsAtLeast: Role.ADMIN, change: "Aktualisierung/Bereinigung der Werbewidgets" },
        ]
      },
      {
        version: '2022.11.7',
        changes: [
          Changelog.UI,
          { roleIsAtLeast: Role.ADMIN, change: "Performance-Optimierungen am Backend" },
        ]
      },
      {
        version: '2022.11.6',
        changes: [
          Changelog.UI,
          { roleIsAtLeast: Role.ADMIN, change: "Commercial Gen1-Batterie: Anpassung der Parameter für die Batterie-Protection" },
        ]
      },
      {
        version: '2022.11.5',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2022.11.4',
        changes: [
          Changelog.product(Product.FEMS_MODBUS_TCP_API) + Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.INSTALLER, change: "Anzeige von Strom und Spannung des Netzzählers" },
          { roleIsAtLeast: Role.INSTALLER, change: Changelog.product(Product.HOME) + "Korrektur bei Strom-/Spannungswerten des Netzzählers" },
          { roleIsAtLeast: Role.ADMIN, change: "Kleine Korrekturen im App-Center" },
        ]
      },
      {
        version: '2022.11.3',
        changes: [
          "Fehlerbehebungen der Übersetzung",
          Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistenten",
        ]
      },
      {
        version: '2022.11.2',
        changes: [
          Changelog.product(Product.FEMS_SG_READY_WAERMEPUMPE) + "Verbesserung der Logik + Übersetzung",
          Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistenten",
          Changelog.UI + "Links zur Dokumentation/Hilfe für Apps aktualisiert",
          Changelog.UI + "Verbesserungen in der historischen Ansicht",
          "Verbesserungen bei automatischen E-Mail-Benachrichtungen",
          "Darstellung des Zeitpunkts der letzten Datenübertragung in der Übersicht",
          { roleIsAtLeast: Role.ADMIN, change: "Kleine Korrekturen im App-Center" },
          { roleIsAtLeast: Role.ADMIN, change: "Unterstützung für KDK 2PU CT Zähler" },
        ]
      },
      {
        version: '2022.11.1',
        changes: [
          Changelog.openems('2022.11.0'),
          "Fehlerbehebung bei der automatischem Reconnect mit dem Online-Monitoring nach Verbindungsausfall",
          Changelog.library(Library.APACHE_FELIX_HTTP_JETTY, Library.DATE_FNS, Library.MOSHI, Library.ANGULAR, Library.IONIC, Library.NGX_FORMLY),
          { roleIsAtLeast: Role.ADMIN, change: "KACO Blueplanet Hybrid 10: interne Architektur geändert" },
          { roleIsAtLeast: Role.ADMIN, change: "Simulator Grid-Meter: Fehlerbehebung in Verbindung mit Ess-Cluster" },
        ]
      },
      {
        version: '2022.10.7',
        changes: [
          Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistenten",
          "Fehlerbehebungen der Übersetzung Deutsch und Englisch",
          Changelog.product(...Product.ALL_EVCS) + Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: "Modbus/TCP-Api-Kompatibilität für GoodWe Zähler" },
          { roleIsAtLeast: Role.ADMIN, change: "Unterstützung für Ziehl EFR4001IP Zähler" },
          { roleIsAtLeast: Role.ADMIN, change: "Unterstützung für Weidmüller Modbus/TCP Feldbuskoppler UR20-FBC-MOD-TCP-V2" },
        ]
      },
      {
        version: '2022.10.4',
        changes: [
          Changelog.product(Product.FEMS_TIBBER) + "Allgemeine Verbesserungen",
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + "Fehlerbehebung bei der Darstellung der Zielzeit in der richtigen Zeitzone",
          "Umfangreiche Anpassungen an den Möglichkeiten zur Netzwerkkonfiguration",
          "Anzeige aktueller Systemstatus-Meldungen, z. B. bei Wartungsarbeiten",
          "Fehlerbehebung in der Anzeige dreiphasiger historischer Verbrauchs- und Erzeugungsdaten",
          { roleIsAtLeast: Role.ADMIN, change: "Modbus/TCP-Api-Kompatibilität für KACO 10 und GoodWe-ESS" },
          { roleIsAtLeast: Role.ADMIN, change: "Bereinigung der Werbewidgets" },
          { roleIsAtLeast: Role.ADMIN, change: "Verbesserung der Erstinitialisierung von OpenEMS Edge" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung bei automatischem Reconnect Edge mit Backend" },
        ]
      },
      {
        version: '2022.10.3',
        changes: [
          Changelog.UI,
          Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistenten",
        ]
      },
      {
        version: '2022.10.1',
        changes: [
          Changelog.openems('2022.10.0'),
          Changelog.product(Product.COMMERCIAL_30) + "Optimierung der Lese- und Regelgeschwindigkeit",
          Changelog.UI + "Verbesserung der Einstellungsoberfläche für Benachrichtungen",
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + "Allgemeine Verbesserungen",
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung bei automatischem Reconnect Edge mit Backend" },
          { roleIsAtLeast: Role.ADMIN, change: "Betatest Edge-2-Edge: https://github.com/OpenEMS/openems/blob/develop/io.openems.edge.edge2edge/readme.adoc" },
        ]
      },
      {
        version: '2022.9.4',
        changes: [
          Changelog.UI + "Fehlerbehebung bei Übersetzungen",
          Changelog.product(Product.FEMS_MODBUS_TCP_API) + "Schreibzugriff: Fehlerbehebung bei Registern für das Minimum und Maximum der verfügbaren Speicherleistung",
        ]
      },
      {
        version: '2022.9.2',
        changes: [
          Changelog.UI,
          Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistenten",
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + "Fehlerbehebung in der Darstellung der Live-Daten",
          Changelog.UI + "Fehlerbehebung bei Farbdarstellung in der historischen Ansicht",
          Changelog.UI + "Historische Ansicht der eingestellten oder deaktivierten Notstromreserve",
          { roleIsAtLeast: Role.ADMIN, change: "Aktivieriung ESLint für automatische Prüfung der Codequalität im UI" },
          { roleIsAtLeast: Role.ADMIN, change: "Anpassung Übersetzungsframework für einfachere Übersetzungen im UI" },
          { roleIsAtLeast: Role.ADMIN, change: "Refactoring und detaillierte JUnit-Tests der zentralen JsonUtils-Klasse" },
          { roleIsAtLeast: Role.ADMIN, change: "Battery Soltaro.Single.C: entferne 'Cell Voltage Low Pre-Alarm'" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung bei automatischem Reconnect Edge mit Backend" },
          { roleIsAtLeast: Role.ADMIN, change: "Kleine Korrekturen im App-Center" },
          { roleIsAtLeast: Role.ADMIN, change: "In 'Channels' können die gewählten Channels jetzt gespeichert werden" },
        ]
      },
      {
        version: '2022.9.1',
        changes: [
          Changelog.openems('2022.9.0'),
          Changelog.UI,
          Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistenten",
          Changelog.product(Product.FEMS_MODBUS_TCP_API) + "Schreibzugriff: Zusätzliche Register für das Minimum und Maximum der verfügbaren Speicherleistung",
          { roleIsAtLeast: Role.ADMIN, change: Changelog.UI + "Verbesserung der Codequalität" },
          { roleIsAtLeast: Role.ADMIN, change: "Erweiterung der PV-WR Abregelung um phasengenaues Detektieren (Vorerst für dreiphasige PV-WR)" },
        ]
      },
      {
        version: '2022.8.5',
        changes: [
          Changelog.UI,
          Changelog.product(Product.FEMS_HOCHLASTZEITFENSTER) + Changelog.GENERAL_OPTIMIZATION,
          { roleIsAtLeast: Role.ADMIN, change: "Unterstützung von Plexlog Datalogger als Zähler" },
        ]
      },
      {
        version: '2022.8.4',
        changes: [
          Changelog.product(Product.COMMERCIAL_50, Product.COMMERCIAL_30) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistenten",
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF, Product.FEMS_HEIZSTAB) + "Überarbeitung des Widgets im Online-Monitoring",
          { roleIsAtLeast: Role.INSTALLER, change: Changelog.GENERAL_OPTIMIZATION + " für invertierte Produktionszähler" },
          { roleIsAtLeast: Role.ADMIN, change: Changelog.UI + " Anzeige von Strom und Spannung für einzelne Erzeuger" },
          { roleIsAtLeast: Role.ADMIN, change: "Strukturelle Änderungen am Online-Monitoring. Verbesserung der automatischen Tests." },
        ]
      },
      {
        version: '2022.8.3',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2022.8.2',
        changes: [
          Changelog.product(Product.COMMERCIAL_50) + "Inbetriebnahmeassistent ist jetzt auch für Commercial 50 verfügbar",
          Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent",
          "Funktion für Systemupdate in den Einstellungen",
          { roleIsAtLeast: Role.INSTALLER, change: "Anzeige von Strom und Spannung für Erzeuger" },
          { roleIsAtLeast: Role.ADMIN, change: "Werbewidgets: GLS Crowdfunding" },
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Info, wenn ein Update zur Verfügung steht" },
          { roleIsAtLeast: Role.ADMIN, change: "Shelly: Anzeige als Stromzähler" },
        ]
      },
      {
        version: '2022.8.1',
        changes: [
          Changelog.openems('2022.8.0'),
          "Optimierung der Stabilität und Leistung des Backends",
          Changelog.library(Library.NGX_FORMLY),
          { roleIsAtLeast: Role.ADMIN, change: "Unterstützung für SMA Sunny Island 4.4 und 6" },
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Verbesserungen" },
          { roleIsAtLeast: Role.ADMIN, change: "OCPP-Server für Ladesäulen: Fehlerbehebung" },
        ]
      },
      {
        version: '2022.7.5',
        changes: [
          Changelog.UI,
        ]
      },
      {
        version: '2022.7.3',
        changes: [
          Changelog.UI,
          Changelog.product(Product.HOME) + "Automatisches Update durch den Inbetriebnahmeassistent",
          { roleIsAtLeast: Role.ADMIN, change: "GoodWe/Home Charger: Channel CURRENT/VOLTAGE" },
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: App-Icons überarbeitet" },
          { roleIsAtLeast: Role.ADMIN, change: "UI: generische Überarbeitung Live-Ansicht Verbrauch/Netz" },
        ]
      },
      {
        version: '2022.7.2',
        changes: [
          Changelog.UI,
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent",
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung an Schnittstelle FEMS Backend zu InfluxDB" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung an SMA ESS" },
          { roleIsAtLeast: Role.ADMIN, change: "Werbewidgets: fenecon.de/heizstab" },
          { roleIsAtLeast: Role.ADMIN, change: "Excel-Export gesperrt für Zeiträume > 3 Monate" },
        ]
      },
      {
        version: '2022.7.1',
        changes: [
          Changelog.openems('2022.7.0'),
          Changelog.product(Product.FEMS_PV_FRONIUS) + "Neue FEMS-App",
          Changelog.product(Product.COMMERCIAL_30) + "Inbetriebnahmeassistent ist jetzt auch für Commercial 30 verfügbar",
          Changelog.library(Library.INFLUXDB, Library.ANGULAR, Library.IONIC, Library.NGX_FORMLY, Library.D3, Library.NGX_COOKIE_SERVICE,
            Library.NGX_SPINNER, Library.FASTEXCEL, Library.OKHTTP, Library.OKIO, Library.POSTGRESQL, Library.JNA),
          { roleIsAtLeast: Role.ADMIN, change: "Werbewidgets: Alerting" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung bei SunSpec 32-bit Registern" },
        ]
      },
      {
        version: '2022.6.6',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent",
          { roleIsAtLeast: Role.ADMIN, change: "Backend-Alerting: Übernahme der FEMS-Nummer und Anpassung UI" },
          { roleIsAtLeast: Role.ADMIN, change: "App-Center: Installation/Deinstallation von Read-Write-/Read-Only-Apps" },
          { roleIsAtLeast: Role.ADMIN, change: "User-Registrierung: OEM-fähigkeit" },
          { roleIsAtLeast: Role.ADMIN, change: "TimescaleDB: Weitere Entwicklungen; Test auf fems888" },
        ]
      },
      {
        version: '2022.6.4',
        changes: [
          Changelog.product(Product.HOME) + "Inbetriebnahmeassistent unterstützt die Einbindung eines Rundsteuerempfängers",
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + "Anpassungen für die neueste Batteriegeneration (Gen2)",
          { roleIsAtLeast: Role.ADMIN, change: "Commercial Gen2-Batterie: Darstellung SoC zwischen 0 und 100 %; Anpassung der Parameter für die Batterie-Protection" },
          { roleIsAtLeast: Role.ADMIN, change: "'App-Assistent' aus dem UI entfernt. Dieser wird vollständig ersetzt vom neuen App-Center" },
          { roleIsAtLeast: Role.ADMIN, change: "Umfangreiche Verbesserungen am App-Center" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung beim Setzen von Shelly Relais" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung beim Auslesen der FEMS-Nummber über die Modbus-TCP-Api" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung für via OCPP eingebundene Ladesäulen (ABL, IES KeyWatt)" },
          { roleIsAtLeast: Role.ADMIN, change: "Alpha-Tests für neue Zeitreihendatenbank TimescaleDB; diese wird InfluxDB ablösen" },
          { roleIsAtLeast: Role.ADMIN, change: "Backend-Alerting: Anpassung der Benutzeroberfläche (Settings -> Alarmierung)" },
        ]
      },
      {
        version: '2022.6.3',
        changes: [
          Changelog.product(Product.FEMS_PV_SMA) + "Fehlerbehebung bei der Unterstützung für einphasige SMA-Wechselrichter via SunSpec",
          { roleIsAtLeast: Role.ADMIN, change: "Der WebSocket Reconnect zum Backend hat jetzt ein weiteres Timeout. Evtl. löst das Probleme, dass sich manche FEMSe nicht mehr verbunden haben." },
        ]
      },
      {
        version: '2022.6.2',
        changes: [
          Changelog.UI
        ]
      },
      {
        version: '2022.6.1',
        changes: [
          Changelog.openems('2022.6.0'),
          Changelog.product(Product.FEMS_MODBUS_TCP_API) + "Auslesen der FEMS-Nummer über das Register 'Manufacturer EMS Serial Number' (Modbus-Addresse 85)",
          Changelog.product(Product.FEMS_KEBA) + "Auslesen der über die DIP-Switches gesetzten Leistungslimitierung",
          Changelog.product(Product.FEMS_PV_SMA) + "Unterstützung für einphasige SMA-Wechselrichter via SunSpec",
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent: Setze 'Vorname Nachname, Ort' als Beschreibung in der Übersicht",
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + Changelog.GENERAL_OPTIMIZATION + " (für Variante 'Cluster C')",
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung beim Senden von Daten an das Backend, wenn Component-IDs doppelt vorkommen" },
          { roleIsAtLeast: Role.ADMIN, change: "Anpassungen am IBN-Assistent: Vorbereitung für IBN-Assistent für Commercial-Serie" },
          { roleIsAtLeast: Role.ADMIN, change: "Anpassungen am IBN-Assistent: Beschränke Eingabemöglichkeit auf positive Zahlen" },
          { roleIsAtLeast: Role.ADMIN, change: "Werbewidgets: Optimierung der netzdienlichen Beladung, Energiewende mit FEMS" },
          { roleIsAtLeast: Role.ADMIN, change: "Vorarbeiten am Backend für Fehler-/Status-Lognachrichten aus dem UI" },
          { roleIsAtLeast: Role.ADMIN, change: "FENECON Pro 9-12: Ausblenden von unnötigen Statusmeldungen" },
        ]
      },
      {
        version: '2022.5.6',
        changes: [
          Changelog.UI
        ]
      },
      {
        version: '2022.5.5',
        changes: [
          Changelog.UI + "Verbesserung des lokalen Monitorings für historische Daten",
          Changelog.library(Library.APACHE_FELIX_WEBCONSOLE),
          { roleIsAtLeast: Role.ADMIN, change: "URL für 'Passwort zurücksetzen' geändert" },
          { roleIsAtLeast: Role.ADMIN, change: "Backend-Änderungen für Alerting und IBN-Assistent" },
        ]
      },
      {
        version: '2022.5.4',
        changes: [
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + "Fehlerbehebung wenn keine Produktion verfügbar ist",
          Changelog.product(Product.FEMS_HEIZSTAB, Product.FEMS_SCHWELLWERT_STEUERUNG, Product.FEMS_BLOCKHEIZKRAFTWERK,
            Product.FEMS_MANUELLE_RELAISSTEUERUNG, Product.FEMS_AWATTAR, Product.FEMS_TIBBER, Product.FEMS_CORRENTLY,
            Product.FEMS_NETZDIENLICHE_BELADUNG)
          + "Verbesserte Darstellung des aktiven Zeitraums in der Historie",
          Changelog.library(Library.POSTGRESQL),
          { roleIsAtLeast: Role.ADMIN, change: "Refactoring des Component-Managers" },
          { roleIsAtLeast: Role.ADMIN, change: "UI-Verbesserung am System-Update" },
        ]
      },
      {
        version: '2022.5.2',
        changes: [
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG) + "Allgemeine Verbesserungen; Sicherstellung einer minimalen Beladeleistung",
          Changelog.product(Product.PRO_HYBRID_10) + "Kompatibilität mit KACO Firmware Version 8 (nur lesend)",
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + "Fehlerbehebung in der Anzeige bei Ladezustand 0 %",
          Changelog.product(Product.COMMERCIAL_30) + "Verbesserung an Implementierung Sinexcel (Ländereinstellung, Notstrom)",
          { roleIsAtLeast: Role.ADMIN, change: "Umbenennung SolarEdge PV-Inverter und Meter" },
          { roleIsAtLeast: Role.ADMIN, change: "Prüfung auf Apikey (/etc/fems und Controller.Api.Backend) -> neuer Channel ctrlBackend0/WrongApikeyConfiguration" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung an Schnittstelle FEMS Backend zu InfluxDB" },
          { roleIsAtLeast: Role.ADMIN, change: "FEMS-Authentifizierung am Backend: entferne Leerzeichen von Apikey" },
          Changelog.library(Library.APACHE_FELIX_WEBCONSOLE),
        ]
      },
      {
        version: '2022.5.1',
        changes: [
          Changelog.openems('2022.5.0'),
          Changelog.product(Product.FEMS_NETZDIENLICHE_BELADUNG, Product.FEMS_HEIZSTAB) + "Korrektur der Anzeige des Betriebsstundenzählers in der historischen Darstellung",
          Changelog.library(Library.INFLUXDB),
          Changelog.UI + "Fehlerbehebung bei Konfiguration von Uhrzeiten",
          { roleIsAtLeast: Role.ADMIN, change: "Soltaro-Batterien Version C: PreAlarmCellVoltageHigh und PreAlarmTotalVoltageHigh ausgeblendet" },
          { roleIsAtLeast: Role.ADMIN, change: "Commercial Gen2-Batterie: Warning, CommunicationStopCharging, CommunicationStopDischarging ausgeblendet" },
          { roleIsAtLeast: Role.ADMIN, change: "Verbesserung 'Virtual Subtract Meter'" },
          { roleIsAtLeast: Role.ADMIN, change: "Fehlerbehebung an Schnittstelle FEMS Backend zu InfluxDB" }
        ]
      },
      {
        version: '2022.4.1',
        changes: [
          Changelog.openems('2022.4.0'),
          Changelog.product(...Product.FEMS_ALL_TIME_OF_USE_TARIFF) + "Allgemeine Verbesserung",
          Changelog.library(Library.INFLUXDB, Library.ANGULAR, Library.IONIC, Library.GRADLE, Library.APACHE_FELIX_CONFIGADMIN, Library.D3),
          "Mehrsprachiger Excel-Export aus dem Online-Monitoring",
          Changelog.product(Product.COMMERCIAL_30) + "Fehlerbehebungen und Berechnung von Energie-Werten",
          Changelog.UI + "Verbesserung der Anzeige von Verbrauchs- und Erzeugungszählern",
          Changelog.UI + "Refactoring der Charts für historische Daten",
          Changelog.openemsComponent(OpenemsComponent.SDM630_ZAEHLER, "Korrektur der Modbus-Register für Blindleistung"),
          { roleIsAtLeast: Role.ADMIN, change: "Umfangreiche Verbesserungen am FEMS App-Center (Beta-Test)" },
          { roleIsAtLeast: Role.ADMIN, change: "Benachrichtigung bei Ausfall eines FEMS (Beta-Test)" },
          { roleIsAtLeast: Role.ADMIN, change: "Neues Werbewidget für direkte Anbindung eines KOSTAL PV-Wechselrichter" },
        ]
      },
      {
        version: '2022.3.6',
        changes: [
          Changelog.library(Library.INFLUXDB)
        ]
      },
      {
        version: '2022.3.3',
        changes: [
          Changelog.product(Product.HOME) + Changelog.GENERAL_OPTIMIZATION + " am Inbetriebnahmeassistent: Responsive Größe des Eingabefensters, besser lesbare PV-Ausrichtung im IBN-Protokoll",
          Changelog.product(Product.FEMS_HEIZSTAB) + "Kontinuierliche Aufsummierung der Betriebsstunden je Level",
          Changelog.UI + "Fehlerbehebungen in der Darstellung der historischen Daten",
          Changelog.product(Product.COMMERCIAL_30, Product.COMMERCIAL_50) + "Anpassungen für die neueste Batteriegeneration (Gen2)",
          Changelog.product(Product.FEMS_PV_KOSTAL) + "Kompatibilität mit Kostal PV-Wechselrichtern. Getestet mit Kostal Plenticore 5.5 und Pico 5.5",
          Changelog.library(Library.INFLUXDB),
          { roleIsAtLeast: Role.ADMIN, change: "Zusammenlegung der FEMS System-Update Funktion, je nach installierter FEMS-Version" },
          { roleIsAtLeast: Role.ADMIN, change: "Umfangreiche Verbesserungen am FEMS App-Center (Beta-Test)" },
        ]
      },
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
          Changelog.openemsComponent(OpenemsComponent.PV_FRONIUS, "Kompatibilität mit Fronius PV-Wechselrichtern. Getestet mit Fronius Symo"),
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
          Changelog.openemsComponent(OpenemsComponent.PQ_PLUS_ZAEHLER, "Kompatibilität mit PQ Plus UMD96"),
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