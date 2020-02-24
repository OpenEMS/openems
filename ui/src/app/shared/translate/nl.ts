export const TRANSLATION = {
    General: {
        Mode: "Mode",
        Automatic: "Automatisch",
        State: "Staat",
        On: "Naar",
        Off: "Van",
        Active: "Actief",
        currentValue: 'Huidige waarde',
        Inactive: "Inactief",
        Manually: "handmatig",
        Phase: "Fase",
        Phases: "Fases",
        Autarchy: "Autarkie",
        SelfConsumption: "Eigen consumptie",
        Cumulative: "Cumulatieve Waarden",
        Grid: "Net",
        GridBuy: "Netafname",
        GridSell: "Netteruglevering",
        GridBuyAdvanced: "Netafname",
        GridSellAdvanced: "Netteruglevering",
        OffGrid: "Geen Netaansluiting!",
        Production: "Opwekking",
        Consumption: "Verbruik",
        otherConsumption: "andere consumptie",
        Total: "totale verbruik",
        Load: "Laden",
        Power: "Vermogen",
        StorageSystem: "Batterij",
        History: "Historie",
        Live: 'Live',
        NoValue: "Geen waarde",
        Soc: "Laadstatus",
        Percentage: "Procent",
        More: "Meer…",
        ChargePower: "Laad vermogen",
        DischargePower: "Ontlaad vermogen",
        ChargeDischarge: "Debet/ontlaad",
        ActualPower: "e-car Laad vermogen",
        PeriodFromTo: "van {{value1}} tot {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "dd-MM-yyyy", // e.g. German: dd.MM.yyyy, English: yyyy-MM-dd (dd = Day, MM = Month, yyyy = Year)
        ChangeAccepted: "Wijziging geaccepteerd",
        ChangeFailed: "Wijziging mislukt",
        InputNotValid: "Invoer ongeldig",
        Week: {
            Monday: "Maandag",
            Tuesday: "Dinsdag",
            Wednesday: "Woensdag",
            Thursday: "Donderdag",
            Friday: "Vrijdag",
            Saturday: "Zaterdag",
            Sunday: "Zondag"
        },
        ReportValue: "Rapporteer beschadigde gegevens"
    },
    Menu: {
        Index: "Overzicht",
        AboutUI: "Over OpenEMS UI",
        GeneralSettings: 'Algemene instellingen',
        EdgeSettings: 'FEMS instellingen',
        Menu: 'Menu',
        Overview: 'FEMS overzicht',
        Logout: 'Uitloggen'
    },
    Index: {
        AllConnected: "Alle verbindingen gemaakt.",
        ConnectionSuccessful: "Succesvol verbonden met {{value }}.", // (value = Name vom Websocket)
        ConnectionFailed: "Verbinding met {{ value } } mislukt.", // (value = Name vom Websocket)
        ToEnergymonitor: "Naar Energiemonitor...",
        IsOffline: "OpenEMS is offline!"
    },
    Edge: {
        Index: {
            Energymonitor: {
                Title: "Energiemonitor",
                ConsumptionWarning: "Verbruik & onbekende producenten",
                Storage: "Batterij",
                ReactivePower: "Blind vermogen",
                ActivePower: "Actief vermogen",
                GridMeter: "Energiemeter",
                ProductionMeter: "Productiemeter"
            },
            Energytable: {
                Title: "Energie tabel",
                LoadingDC: "DC laden",
                ProductionDC: "Generatie DC"
            },
            Widgets: {
                Channeltreshold: {
                    Output: "uitgang"
                },
                Peakshaving: {
                    peakshaving: 'Piek scheren',
                    peakshavingPower: 'Afvoer voorbij',
                    rechargePower: 'Bezig met laden onder',
                    relationError: 'Ontladingslimiet moet groter zijn dan of gelijk zijn aan de belastingslimiet'
                },
                phasesInfo: "De som van de afzonderlijke fasen kan om technische redenen enigszins afwijken van het totaal.",
                autarchyInfo: "Autarky geeft het percentage huidig ​​vermogen aan dat kan worden gedekt door opwekking en ontlading van de opslag.",
                selfconsumptionInfo: "Eigen verbruik geeft het percentage van de momenteel gegenereerde uitvoer aan dat kan worden gebruikt door direct verbruik en opslagbelasting zelf.",
                twoWayInfoStorage: "Negative Werte entsprechen Speicher Beladung, Positive Werte entsprechen Speicher Entladung",
                twoWayInfoGrid: "Negative Werte entsprechen Netzeinspeisung, Positive Werte entsprechen Netzbezug",
                CHP: {
                    LowThreshold: "Lage drempelwaarde",
                    HighThreshold: "hoge drempel"
                },
                EVCS: {
                    ChargeTarget: "Lading doel",
                    ChargingStation: "Laadstation",
                    ChargingStationCluster: "Laadstation cluster",
                    OverviewChargingStations: "Overzicht laadstations",
                    ChargingStationDeactivated: "Laadstation gedeactiveerd",
                    Prioritization: "Prioritering",
                    Status: "Staat",
                    Starting: "Beginnend",
                    NotReadyForCharging: "Niet klaar voor opladen",
                    ReadyForCharging: "Klaar om op te laden",
                    Charging: "Is aan het laden",
                    NotCharging: "Niet opladen",
                    Error: "Fout",
                    NotAuthorized: "Geen bevoegdheid",
                    Unplugged: "Unplugged",
                    ChargeLimitReached: "Oplaadlimiet bereikt",
                    CharingStationPluggedIn: "Laadstation aangesloten",
                    ChargingStationPluggedInLocked: "Laadstation aangesloten + op slot",
                    ChargingStationPluggedInEV: "Laadstation + E-Auto aangesloten",
                    ChargingStationPluggedInEVLocked: "Laadstation + E-Auto aangesloten + op slot",
                    ChargingLimit: "Laadlimiet",
                    ChargingPower: "Oplaadvermogen",
                    AmountOfChargingStations: "Aantal laadstations",
                    TotalChargingPower: "Totaal laadvermogen",
                    CurrentCharge: "Huidige lading",
                    TotalCharge: "Totale lading",
                    EnforceCharging: "Handhaaf het laden",
                    Cable: "Kabel",
                    CableNotConnected: "Kabel is niet aangesloten",
                    CarFull: "Auto is vol",
                    EnergieSinceBeginning: "Energie sinds de laatste lading start",
                    ChargeMode: "laadmodus",
                    ActivateCharging: "Activeer het laadstation",
                    ClusterConfigError: "Er is een fout opgetreden in de configuratie van het Evcs-cluster",
                    EnergyLimit: "Energielimiet",
                    MaxEnergyRestriction: "Beperk de maximale energie per lading",
                    NoConnection: {
                        Description: "Hij kon niet op het laadstation worden aangesloten.",
                        Help1: "Controleer of het laadstation is ingeschakeld en via het netwerk kan worden bereikt",
                        Help1_1: "Het IP-adres van het laadstation verschijnt bij het opnieuw inschakelen"
                    },
                    OptimizedChargeMode: {
                        Name: "Geoptimaliseerd laden",
                        ShortName: "Automatisch",
                        Info: "In deze modus wordt de belasting van de auto aangepast aan de huidige productie en het huidige verbruik.",
                        MinInfo: "Als u wilt voorkomen dat de auto 's nachts niet oplaadt, kunt u een minimale lading instellen.",
                        MinCharging: "Minimale vergoeding betalen",
                        MinChargePower: "Loading rate",
                        ChargingPriority: "Afhankelijk van de prioriteit, wordt het geselecteerde onderdeel eerst geladen"
                    },
                    ForceChargeMode: {
                        Name: "Gedwongen laden",
                        ShortName: "handmatig",
                        Info: "In deze modus wordt het laden van de auto afgedwongen, d.w.z. het is altijd gegarandeerd dat de auto wordt opgeladen, zelfs als het laadstation toegang moet hebben tot netstroom.",
                        MaxCharging: "Maximale laadstroom",
                        MaxChargingDetails: "Als de auto de ingevoerde maximale waarde niet kan laden, wordt het vermogen automatisch beperkt."
                    }
                }
            }
        },
        History: {
            SelectedPeriod: "Geselecteerde periode: ",
            OtherPeriod: "Andere periode",
            Period: "Periode",
            SelectedDay: "{{value}}",
            Today: "Vandaag",
            Yesterday: "Gisteren",
            LastWeek: "Vorige week",
            LastMonth: "Vorige maand",
            LastYear: "Vorig jaar",
            Go: "Ga!",
            Export: "download als Excel-bestand",
            Day: "Dag",
            Week: "Woche",
            Month: "Maand",
            Year: "Jaar",
            noData: "geen gegevens beschikbaar",
            activeDuration: "actieve duur",
            BeginDate: "Selecteer startdatum",
            EndDate: "Selecteer einddatum",
            Sun: "Zon",
            Mon: "Maa",
            Tue: "Din",
            Wed: "Woe",
            Thu: "Don",
            Fri: "Vri",
            Sat: "Zat",
            Jan: "Jan",
            Feb: "Feb",
            Mar: "Maa",
            Apr: "Apr",
            May: "Mei",
            Jun: "Jun",
            Jul: "Jul",
            Aug: "Aug",
            Sep: "Sep",
            Oct: "Okt",
            Nov: "Nov",
            Dec: "Dec"
        },
        Config: {
            Index: {
                Bridge: "Verbindingen en apparaten",
                Scheduler: "Toepassingsschema",
                Controller: "Toepassingen",
                Simulator: "Simulator",
                ExecuteSimulator: "Simulatie uitvoeren",
                Log: "Log",
                LiveLog: "Live System log",
                AddComponents: "Componenten installeren",
                AdjustComponents: "Componenten configureren",
                ManualControl: "Handmatige bediening",
                DataStorage: "Gegevensopslag",
                SystemExecute: "Voer systeemopdracht uit"
            },
            More: {
                ManualCommand: "Handmatig commando",
                Send: "Verstuur",
                RefuInverter: "REFU inverter",
                RefuStartStop: "Inverter starten/ stoppen",
                RefuStart: "Start",
                RefuStop: "Stop",
                ManualpqPowerSpecification: "Gespecificeerd vermogen",
                ManualpqSubmit: "Toepassen",
                ManualpqReset: "Reset"
            },
            Scheduler: {
                NewScheduler: "New Schema...",
                Class: "Soort: ",
                NotImplemented: "Gegevens niet geïmplementeerd: ",
                Contact: "Dit zou niet mogen gebeuren. Neem contact op met <a href=\"mailto:{{value}}\">{{value}}</a>.",
                Always: "Altijd"
            },
            Log: {
                AutomaticUpdating: "Automatisch updaten",
                Timestamp: "Tijdspit",
                Level: "Niveau",
                Source: "Bron",
                Message: "Bericht"
            },
            Controller: {
                InternallyID: "Intern ID:",
                App: "App:",
                Priority: "Prioriteit: "
            },
            Bridge: {
                NewDevice: "Nieuw apparaat…",
                NewConnection: "Nieuwe verbinding..."
            }
        }
    },
    About: {
        UI: "Gebruikersinterface voor OpenEMS",
        Developed: "Deze gebruikersinterface is ontwikkeld als open-source-software.",
        Sourcecode: "Broncode",
        CurrentDevelopments: "Huidige ontwikkelingen",
        Build: "Versie",
        Contact: "Voor meer informatie of suggesties over het systeem, neem contact op met het team via <a href=\"mailto:{{value}}\">{{value}}</a>.",
        Language: "Selecteer taal: "
    },
    Notifications: {
        Failed: "Verbinding mislukt.",
        LoggedInAs: "Aangemeld als gebruiker {{ value } }.", // (value = Benutzername)
        LoggedIn: "Aangemeld.",
        AuthenticationFailed: "Geen verbinding.Autorisatie mislukt.",
        Closed: "Verbinding beëindigd."
    }
}
