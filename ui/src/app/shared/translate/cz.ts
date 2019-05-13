export const TRANSLATION = {
    General: {
        Cumulative: "Kumulativní Hodnoty",
        Grid: "Síť",
        GridBuy: "Nákup ze sítě",
        GridSell: "Prodej do sítě",
        OffGrid: "žádné připojení k síti!",
        Production: "Výroba",
        Consumption: "Spotřeba",
        Load: "nálož",
        Power: "Výkon",
        StorageSystem: "Systém bateriového úložiště",
        History: "Historie",
        Live: 'Live',
        NoValue: "Žádná hodnota",
        Soc: "Stav nabití",
        Percentage: "Procentuální vyjádření",
        More: "Další",
        ChargePower: "Nabíjecí výkon",
        DischargePower: "Vybíjecí výkon",
        ActualPower: "E-Car Nabíjecí výkon",
        PeriodFromTo: "od {{value1}} do {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "dd.MM.yyyy", // e.g. German: dd.MM.yyyy, English: yyyy-MM-dd (dd = Day, MM = Month, yyyy = Year)
        Week: {
            Monday: "Pondělí",
            Tuesday: "Úterý",
            Wednesday: "Středa",
            Thursday: "Čtvrte",
            Friday: "Pátek",
            Saturday: "Sobota",
            Sunday: "Neděle"
        },
        ReportValue: "Nahlášení poškozených dat"
    },
    Menu: {
        Index: "Přehled",
        AboutUI: "About OpenEMS UI",
        GeneralSettings: 'Obecné Nastavení',
        EdgeSettings: 'FEMS Obecné Nastavení',
        Menu: 'Menu',
        Overview: 'FEMS Overvire',
        Logout: 'Odhlásit'
    },
    Index: {
        AllConnected: "Všechna připojení aktivní.",
        ConnectionSuccessful: "Úspěšně připojeno k {{value}}.", // value = name of websocket
        ConnectionFailed: "Připojení k {{value}} selhalo.", // value = name of websocket
        ToEnergymonitor: "Do Monitoringu energetických toků…",
        IsOffline: "OpenEMS je ve stavu offline!"
    },
    Edge: {
        Index: {
            Energymonitor: {
                Title: "Monitoring energetických toků",
                ConsumptionWarning: "Spotřeba & neznámá výroba",
                Storage: "Úložiště",
                ReactivePower: "Jalový výkon",
                ActivePower: "Činný výkon",
                GridMeter: "Elektroměr - Odběr",
                ProductionMeter: "Elektroměr - Výroba"
            },
            Energytable: {
                Title: "Tabulka hodnot",
                LoadingDC: "Načítání DC",
                ProductionDC: "Generování DC"
            },
            Widgets: {
                EVCS: {
                    ChargingStation: "Nabíjecí stanice",
                    Status: "Postavení",
                    Starting: "Začínající",
                    NotReadyForCharging: "Není připraven k nabíjení",
                    ReadyForCharging: "Připraven k nabíjení",
                    Charging: "Se nabíjí",
                    Error: "Chyba",
                    NotAuthorized: "Neautorizovaný",
                    Unplugged: "Odpojena",
                    ChargingStationPluggedIn: "Nabíjecí stanice zapojena",
                    ChargingStationPluggedInLocked: "Nabíjecí stanice zapojena + uzamčena",
                    ChargingStationPluggedInEV: "Nabíjecí stanice + e-car připojené",
                    ChargingStationPluggedInEVLocked: "Nabíjecí stanice + e-car připojené + uzamčena",
                    ChargingLimit: "Omezení nabíjení",
                    ChargingPower: "Nabíjecí výkon",
                    AmountOfChargingStations: "Počet nabíjecích stanic",
                    TotalChargingPower: "Celkový nabíjecí výkon",
                    CurrentCharge: "Aktuální nabíjení",
                    TotalCharge: "Celkový poplatek",
                    EnforceCharging: "Prosazování poplatků",
                    Cable: "Kabel",
                    CableNotConnected: "Kabel není připojen",
                    CarFull: "Auto je plné",
                    EnergieSinceBeginning: "Energie od začátku nabíjení",
                    ChargeMode: "režim načítání",
                    ActivateCharging: "Aktivujte nabíjecí stanici",
                    NoConnection: {
                        Description: "Nelze jej připojit k nabíjecí stanici.",
                        Help1: "Zkontrolujte, zda je nabíjecí stanice zapnutá a zda je dostupná prostřednictvím sítě",
                        Help1_1: "Při opětovném zapnutí se objeví IP nabíjecí stanice"
                    },
                    OptimizedChargeMode: {
                        Name: "Optimalizované zatížení",
                        ShortName: "optimalizované",
                        Info: "V tomto režimu je zatížení vozidla přizpůsobeno aktuální výrobě a spotřebě.",
                        MinInfo: "Pokud chcete zabránit tomu, aby se auto nenabíjelo v noci, můžete nastavit minimální poplatek.",
                        MinCharging: "Garance minimálního poplatku?",
                        ChargingPriority: {
                            Info: "V závislosti na prioritizaci bude vybraná komponenta načtena jako první",
                            Car: "Car",
                            Storage: "Storage"
                        }
                    },
                    ForceChargeMode: {
                        Name: "Nucené nakládání",
                        ShortName: "vynucený",
                        Info: "V tomto režimu je vynuceno zatížení vozidla, i. je vždy zaručeno, že vozidlo bude nabíjeno, i když nabíjecí stanice potřebuje přístup k síti.",
                        MaxCharging: "Maximální síla náboje",
                        MaxChargingDetails: "Pokud vůz nemůže načíst zadanou maximální hodnotu, je výkon automaticky omezen."
                    }
                }
            }
        },
        History: {
            SelectedPeriod: "Zvolené období: ",
            OtherPeriod: "Další období",
            Period: "Období",
            Today: "Dnes",
            Yesterday: "Včera",
            LastWeek: "Poslední týden",
            LastMonth: "Poslední měsíc",
            LastYear: "Poslední rok",
            Go: "Jdi!"
        },
        Config: {
            Index: {
                Bridge: "Připojená zařízení",
                Scheduler: "Plánovač aplikací",
                Controller: "Aplikace",
                Simulator: "Simulátor",
                ExecuteSimulator: "Zahájit simulaci",
                Log: "Log",
                LiveLog: "Live log systému",
                AddComponents: "Komponenten installieren",
                AdjustComponents: "Komponenten konfigurieren",
                ManualControl: "Manuální ovládání",
                DataStorage: "Ukládání dat"
            },
            More: {
                ManualCommand: "Manuální příkaz ",
                Send: "Odeslat",
                RefuInverter: "REFU Střídač",
                RefuStartStop: "Start/Stop střídače",
                RefuStart: "Start",
                RefuStop: "Stop",
                ManualpqPowerSpecification: "Specifikace výkonu",
                ManualpqSubmit: "Zadání",
                ManualpqReset: "Reset"
            },
            Scheduler: {
                NewScheduler: "Nový plánovač...",
                Class: "Třída:",
                NotImplemented: "Zadání nebylo implementováno: ",
                Contact: "Došlo k chybě. Prosím kontaktujte <a href=\"mailto:{{value}}\">{{value}}</a>.",
                Always: "Vždy"
            },
            Log: {
                AutomaticUpdating: "Automatický update",
                Timestamp: "Časové razítko",
                Level: "Úroveň",
                Source: "Zdroj",
                Message: "Zpráva"
            },
            Controller: {
                InternallyID: "Vnitřní ID:",
                App: "App:",
                Priority: "Priorita:"
            },
            Bridge: {
                NewDevice: "Nové zařízení...",
                NewConnection: "Nové připojení..."
            }
        }
    },
    About: {
        UI: "Uživatelské rozhraní pro OpenEMS",
        Developed: "Toto uživatelské rozhraní bylo vyvinuto jako open-source software.",
        Sourcecode: "Zdrojový kód",
        CurrentDevelopments: "Aktuální vývoj",
        Build: "Aktuální verze",
        Contact: "S případnými návrhy a pro další informace k systému prosím kontaktujte náš tým na <a href=\"mailto:{{value}}\">{{value}}</a>.",
        Language: "Zvolte jazyk:"
    },
    Notifications: {
        Failed: "Připojení selhalo.",
        LoggedInAs: "Uživatel přihlášen jako {{value}}.", // value = username
        LoggedIn: "Přihlášení proběhlo úspěšně.",
        AuthenticationFailed: "Žádné připojení: Ověření uživatele selhalo.",
        Closed: "Připojení ukončeno."
    }
}
