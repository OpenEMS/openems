export const TRANLSATION = {
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
        Percent: "Prozent"
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
    DeviceOverview: {
        Energymonitor: {
            Title: "Energiemonitor",
            ConsumptionWarning: "Verbrauch & unbekannte Erzeuger",
            Storage: "Speicher",
            Charging: "Beladeleistung",
            Discharging: "Entladeleistung",
            ReactivePower: "Blindleistung",
            ActivePower: "Ausgabeleistung",
            GridCounter: "Netzzähler",
            ProductionCounter: "Erzeugungszähler"
        },
        Energytable: {
            Title: "Energietabelle"
        }
    },
    DeviceHistory: {
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
    ConfigOverview: {
        Bridge: "Verbindungen und Geräte",
        Scheduler: "Anwendungsplaner",
        Controller: "Anwendungen",
        Simulator: "Simulator",
        ExecuteSimulator: "Simulationen ausführen",
        Log: "Log",
        LiveLog: "Live Systemprotokoll",
        More: "Mehr...",
        ManualControl: "Manuelle Steuerung",
        ConfigMore: {
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
        ConfigScheduler: {
            NewScheduler: "Neuer Scheduler...",
            Class: "Klasse:",
            NotImplemented: "Formular nicht implementiert: ",
            Contact: "Das sollte nicht passieren. Bitte kontaktieren Sie",
            Always: "Immer",
            Weektime: {
                Monday: "Montag",
                Tuesday: "Dienstag",
                Wednesday: "Mittwoch",
                Thursday: "Donnerstag",
                Friday: "Freitag",
                Saturday: "Samstag",
                Sunday: "Sonntag"
            }
        },
        ConfigLog: {
            AutomaticUpdating: "Automatische Aktualisierung",
            Timestamp: "Zeitpunkt",
            Level: "Level",
            Source: "Quelle",
            Message: "Nachricht"
        },
        ConfigController: {
            InternallyID: "Interne ID:",
            App: "Anwendung:",
            Priority: "Priorität:"
        },
        ConfigBridge: {
            NewDevice: "Neues Gerät...",
            NewConnection: "Neue Verbindung..."
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
        Contact: "Für Rückfragen und Anregungen zum System, wenden Sie sich bitte an unser FEMS-Team unter",
        Language: "Sprache wählen:"
    },
    Notifications: {
        Failed: "Verbindungsaufbau fehlgeschlagen.",
        LoggedInAs: "Angemeldet als Benutzer",
        LoggedIn: "Angemeldet.",
        AuthenticationFailed: "Keine Verbindung: Authentifizierung fehlgeschlagen.",
        Closed: "Verbindung beendet."
    }
}