export const TRANSLATION = {
    General: {
        Grid: "Netz",
        GridBuy: "Netzbezug",
        GridSell: "Netzeinspeisung",
        Production: "Erzeugung",
        Consumption: "Verbrauch",
        Power: "Leistung",
        StorageSystem: "Speichersystem",
        History: "Historie",
        NoValue: "Kein Wert",
        Soc: "Ladezustand",
        Percentage: "Prozent",
        More: "Mehr...",
        To: "bis",
        Week: {
            Monday: "Montag",
            Tuesday: "Dienstag",
            Wednesday: "Mittwoch",
            Thursday: "Donnerstag",
            Friday: "Freitag",
            Saturday: "Samstag",
            Sunday: "Sonntag"
        }
    },
    Menu: {
        Overview: "Übersicht",
        AboutUI: "Über FEMS-UI"
    },
    Overview: {
        AllConnected: "Alle Verbindungen hergestellt.",
        ConnectionSuccessful: "Verbindung zu {{value}} hergestellt.",
        ConnectionFailed: "Verbindung zu {{value}} getrennt.",
        ToEnergymonitor: "Zum Energiemonitor...",
        IsOffline: "FEMS ist offline!"
    },
    Device: {
        Overview: {
            Energymonitor: {
                Title: "Energiemonitor",
                ConsumptionWarning: "Verbrauch & unbekannte Erzeuger",
                Storage: "Speicher",
                ChargePower: "Beladeleistung",
                DischargePower: "Entladeleistung",
                ReactivePower: "Blindleistung",
                ActivePower: "Ausgabeleistung",
                GridMeter: "Netzzähler",
                ProductionMeter: "Erzeugungszähler"
            },
            Energytable: {
                Title: "Energietabelle"
            }
        },
        History: {
            SelectedPeriod: "Gewählter Zeitraum: ",
            OtherPeriod: "Anderer Zeitraum:",
            Period: "Zeitraum",
            Today: "Heute",
            Yesterday: "Gestern",
            LastWeek: "Letzte Woche",
            LastMonth: "Letzter Monat",
            LastYear: "Letztes Jahr",
            Go: "Los!"
        },
        Config: {
            Overview: {
                Bridge: "Verbindungen und Geräte",
                Scheduler: "Anwendungsplaner",
                Controller: "Anwendungen",
                Simulator: "Simulator",
                ExecuteSimulator: "Simulationen ausführen",
                Log: "Log",
                LiveLog: "Live Systemprotokoll",
                ManualControl: "Manuelle Steuerung"
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
        UI: "Benutzeroberfläche für FEMS und OpenEMS",
        Developed: "Diese Benutzeroberfläche wird von FENECON als Open-Source-Software entwickelt.",
        Fenecon: "Mehr zu FENECON",
        Fems: "Mehr zu FEMS",
        Sourcecode: "Quellcode",
        CurrentDevelopments: "Aktuelle Entwicklungen",
        Build: "Dieser Build",
        Contact: "Für Rückfragen und Anregungen zum System, wenden Sie sich bitte an unser FEMS-Team unter <a href=\"mailto:{{value}}\">{{value}}</a>.",
        Language: "Sprache wählen:"
    },
    Notifications: {
        Failed: "Verbindungsaufbau fehlgeschlagen.",
        LoggedInAs: "Angemeldet als Benutzer \"{{value}}\".",
        LoggedIn: "Angemeldet.",
        AuthenticationFailed: "Keine Verbindung: Authentifizierung fehlgeschlagen.",
        Closed: "Verbindung beendet."
    }
}