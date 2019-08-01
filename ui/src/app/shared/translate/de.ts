export const TRANSLATION = {
    General: {
        Cumulative: "Kumulierte Werte",
        Grid: "Netz",
        GridBuy: "Netzbezug",
        GridSell: "Netzeinspeisung",
        OffGrid: "Keine Netzverbindung!",
        Production: "Erzeugung",
        Consumption: "Verbrauch",
        Load: "Last",
        Power: "Leistung",
        StorageSystem: "Speichersystem",
        History: "Historie",
        Live: 'Live',
        NoValue: "Kein Wert",
        Soc: "Ladezustand",
        Percentage: "Prozent",
        More: "Mehr...",
        ChargePower: "Beladung",
        DischargePower: "Entladung",
        ActualPower: "E-Auto Beladung",
        PeriodFromTo: "von {{value1}} bis {{value2}}", // value1 = start date, value2 = end date
        DateFormat: "dd.MM.yyyy", // z.B. Englisch: yyyy-MM-dd (dd = Tag, MM = Monat, yyyy = Jahr)
        Search: "Suchen",
        Week: {
            Monday: "Montag",
            Tuesday: "Dienstag",
            Wednesday: "Mittwoch",
            Thursday: "Donnerstag",
            Friday: "Freitag",
            Saturday: "Samstag",
            Sunday: "Sonntag"
        },
        ReportValue: "Fehlerhafte Daten melden",
        Capacity: "Kapazität"
    },
    Menu: {
        Index: "Übersicht",
        AboutUI: "Über FEMS",
        GeneralSettings: 'Allgemeine Einstellungen',
        EdgeSettings: 'FEMS Einstellungen',
        Menu: 'Menü',
        Overview: 'FEMS Übersicht',
        Logout: 'Abmelden'
    },
    Index: {
        AllConnected: "Alle Verbindungen hergestellt.",
        ConnectionSuccessful: "Verbindung zu {{value}} hergestellt.", // value = name of websocket
        ConnectionFailed: "Verbindung zu {{value}} getrennt.", // value = name of websocket
        ToEnergymonitor: "Zum Energiemonitor...",
        IsOffline: "FEMS ist offline!"
    },
    Edge: {
        Index: {
            Energymonitor: {
                Title: "Energiemonitor",
                ConsumptionWarning: "Verbrauch & unbekannte Erzeuger",
                Storage: "Speicher",
                ReactivePower: "Blindleistung",
                ActivePower: "Ausgabeleistung",
                GridMeter: "Netzzähler",
                ProductionMeter: "Erzeugungszähler",
                StorageDischarge: "Speicher-Entladung",
                StorageCharge: "Speicher-Beladung"
            },
            Energytable: {
                Title: "Energietabelle",
                LoadingDC: "Beladung DC",
                ProductionDC: "Erzeugung DC"
            },
            Widgets: {
                Info: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen.",
                CHP: {
                    LowThreshold: "Unterer Schwellenwert",
                    HighThreshold: "Oberer Schwellenwert"
                },
                EVCS: {
                    ChargingStation: "Ladestation",
                    Status: "Status",
                    Starting: "Startet",
                    NotReadyForCharging: "Nicht bereit zur Beladung",
                    ReadyForCharging: "Bereit zur Beladung",
                    Charging: "Beladung läuft",
                    Error: "Fehler",
                    NotAuthorized: "Nicht authorisiert",
                    Unplugged: "Ausgesteckt",
                    ChargingStationPluggedIn: "Ladestation eingesteckt",
                    ChargingStationPluggedInLocked: "Ladestation eingesteckt + gesperrt",
                    ChargingStationPluggedInEV: "Ladestation + E-Auto eingesteckt",
                    ChargingStationPluggedInEVLocked: "Ladestation + E-Auto eingesteckt + gesperrt",
                    ChargingLimit: "Lade-Begrenzung",
                    ChargingPower: "Lade-Leistung",
                    CurrentCharge: "Aktuelle Beladung",
                    TotalCharge: "Gesamte Beladung",
                    EnforceCharging: "Erzwinge Beladung",
                    Cable: "Kabel",
                    CableNotConnected: "Kabel ist nicht angeschlossen",
                    CarFull: "Auto ist voll",
                    EnergieSinceBeginning: "Energie seit Beginn der Ladung",
                    ChargeMode: "Belademodus",
                    ActivateCharging: "Aktivieren der Ladesäule",
                    NoConnection: {
                        Description: "Es konnte keine Verbindung zur Ladestation aufgebaut werden.",
                        Help1: "Prüfen sie ob die Ladestation eingeschaltet und über das Netz erreichbar ist",
                        Help1_1: "Die IP der Ladesäule erscheint beim erneuten einschalten"
                    },
                    OptimizedChargeMode: {
                        Name: "Optimierte Beladung",
                        ShortName: "Optimiert",
                        Info: "In diesem Modus wird die Beladung des Autos an die aktuelle Produktion und den aktuellen Verbrauch angepasst.",
                        MinInfo: "Falls verhindert werden soll, dass das Auto in der Nacht gar nicht lädt, kann eine minimale Aufladung festgelegt werden.",
                        MinCharging: "Minimale Aufladung garantieren?",
                        ChargingPriority: {
                            Info: "Je nach Priorisierung wird die ausgewählte Komponente zuerst beladen",
                            Car: "Auto",
                            Storage: "Speicher"
                        }
                    },
                    ForceChargeMode: {
                        Name: "Erzwungene Beladung",
                        ShortName: "Erzwungen",
                        Info: "In diesem Modus wird die Beladung des Autos erzwungen, d.h. es wird immer garantiert, dass das Auto geladen wird, auch wenn die Ladesäule auf Netzstrom zugreifen muss.",
                        MaxCharging: "Maximale Ladestärke",
                        MaxChargingDetails: "Falls das Auto den eingegebenen Maximalwert nicht laden kann, wird die Leistung automatisch begrenzt."
                    }
                }
            }
        },
        History: {
            SelectedPeriod: "Gewählter Zeitraum: ",
            OtherPeriod: "Anderer Zeitraum",
            Period: "Zeitraum",
            SelectedDay: "{{value}}",
            Today: "Heute",
            Yesterday: "Gestern",
            LastWeek: "Letzte Woche",
            LastMonth: "Letzter Monat",
            LastYear: "Letztes Jahr",
            Go: "Los!",
            Export: "Download als EXCEL-Datei"
        },
        Config: {
            Index: {
                Bridge: "Verbindungen und Geräte",
                Scheduler: "Anwendungsplaner",
                Controller: "Anwendungen",
                Simulator: "Simulator",
                ExecuteSimulator: "Simulationen ausführen",
                Log: "Log",
                LiveLog: "Live Systemprotokoll",
                AddComponents: "Komponenten installieren",
                AdjustComponents: "Komponenten konfigurieren",
                ManualControl: "Manuelle Steuerung",
                DataStorage: "Datenspeicher"
            },
            More: {
                ManualCommand: "Manueller Befehl",
                Send: "Senden",
                RefuInverter: "REFU Wechselrichter",
                RefuStartStop: "Wechselrichter starten/stoppen",
                RefuStart: "Starten",
                RefuStop: "Stoppen",
                ManualpqPowerSpecification: "Leistungsvorgabe",
                ManualpqSubmit: "Übernehmen",
                ManualpqReset: "Zurücksetzen"
            },
            Scheduler: {
                NewScheduler: "Neuer Scheduler...",
                Class: "Klasse:",
                NotImplemented: "Formular nicht implementiert: ",
                Contact: "Das sollte nicht passieren. Bitte kontaktieren Sie <a href=\"mailto:{{value}}\">{{value}}</a>.",
                Always: "Immer"
            },
            Log: {
                AutomaticUpdating: "Automatische Aktualisierung",
                Timestamp: "Zeitpunkt",
                Level: "Level",
                Source: "Quelle",
                Message: "Nachricht"
            },
            Controller: {
                InternallyID: "Interne ID:",
                App: "Anwendung:",
                Priority: "Priorität:"
            },
            Bridge: {
                NewDevice: "Neues Gerät...",
                NewConnection: "Neue Verbindung..."
            }
        }
    },
    About: {
        UI: "Benutzeroberfläche für FEMS",
        Developed: "Diese Benutzeroberfläche wird als Open-Source-Software entwickelt.",
        OpenEMS: "Mehr zu OpenEMS",
        CurrentDevelopments: "Aktuelle Entwicklungen",
        Build: "Dieser Build",
        Contact: "Für Rückfragen und Anregungen zum System, wenden Sie sich bitte an unser Team unter <a href=\"mailto:{{value}}\">{{value}}</a>.",
        Language: "Sprache wählen:",
        FAQ: "Häufig gestellte Fragen (FAQ)"
    },
    Notifications: {
        Failed: "Verbindungsaufbau fehlgeschlagen.",
        LoggedInAs: "Angemeldet als Benutzer \"{{value}}\".", // value = username
        LoggedIn: "Angemeldet.",
        AuthenticationFailed: "Keine Verbindung: Authentifizierung fehlgeschlagen.",
        Closed: "Verbindung beendet."
    }
}