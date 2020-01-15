export const TRANSLATION = {
    General: {
        Mode: "Režim",
        Automatic: "Automaticky",
        On: "zapnutý",
        Off: "Pryč",
        State: "Stát",
        Active: "aktivně",
        Inactive: "Neaktivní",
        Manually: "Ruční",
        Phase: "Fáze",
        Phases: "Fáze",
        Autarchy: "Soběstačnost",
        SelfConsumption: "Vlastní spotřeba",
        Cumulative: "Kumulativní Hodnoty",
        Grid: "Síť",
        GridBuy: "Nákup ze sítě",
        GridSell: "Prodej do sítě",
        GridBuyAdvanced: "Nákup",
        GridSellAdvanced: "Prodej",
        OffGrid: "žádné připojení k síti!",
        Production: "Výroba",
        Consumption: "Spotřeba",
        otherConsumption: "jiná spotřeba",
        Total: "celková spotřeba",
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
        ChargeDischarge: "Debetní/vybíjení",
        ActualPower: "E-Car Nabíjecí výkon",
        PeriodFromTo: "od {{value1}} do {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "dd.MM.yyyy", // e.g. German: dd.MM.yyyy, English: yyyy-MM-dd (dd = Day, MM = Month, yyyy = Year)
        ChangeAccepted: "Změna byla přijata",
        ChangeFailed: "Změna se nezdařila",
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
        EdgeSettings: 'hy-control Předvolby',
        Menu: 'Menu',
        Overview: 'hy-control Přehled',
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
                ProductionMeter: "Elektroměr - Výroba",
                StorageDischarge: "baterie výtok",
                StorageCharge: "baterie nakládání"
            },
            Energytable: {
                Title: "Tabulka hodnot",
                LoadingDC: "Načítání DC",
                ProductionDC: "Generování DC"
            },
            Widgets: {
                Channeltreshold: {
                    Output: "Výstup"
                },
                phasesInfo: "Součet jednotlivých fází se může z technických důvodů mírně lišit od celkového počtu.",
                autarchyInfo: "Autarky označuje procento aktuální energie, kterou lze pokrýt vybitím z výroby a skladování.",
                selfconsumptionInfo: "Vlastní spotřeba označuje procento aktuálně generovaného výstupu, který lze použít přímou spotřebou a samotným zatížením úložiště.",
                twoWayInfoStorage: "Negative Werte entsprechen Speicher Beladung, Positive Werte entsprechen Speicher Entladung",
                twoWayInfoGrid: "Negative Werte entsprechen Netzeinspeisung, Positive Werte entsprechen Netzbezug",
                CHP: {
                    LowThreshold: "Nízký práh",
                    HighThreshold: "vysoký práh"
                },
                EVCS: {
                    ChargingStation: "Nabíjecí stanice",
                    ChargingStationCluster: "Klastr nabíjecí stanice",
                    OverviewChargingStations: "Přehled nabíjecích stanic",
                    ChargingStationDeactivated: "Nabíjecí stanice byla deaktivována",
                    Prioritization: "Stanovení priorit",
                    Status: "Postavení",
                    Starting: "Začínající",
                    NotReadyForCharging: "Není připraven k nabíjení",
                    ReadyForCharging: "Připraven k nabíjení",
                    Charging: "Se nabíjí",
                    NotCharging: "Nenabíjí se",
                    Error: "Chyba",
                    NotAuthorized: "Neautorizovaný",
                    Unplugged: "Odpojena",
                    ChargeLimitReached: "Bylo dosaženo limitu nabíjení",
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
                    EnergieSinceBeginning: "Energie od posledního začátku nabíjení",
                    ChargeMode: "režim načítání",
                    ActivateCharging: "Aktivujte nabíjecí stanici",
                    ClusterConfigError: "V konfiguraci clusteru Evcs došlo k chybě",
                    EnergyLimit: "Limit energie",
                    MaxEnergyRestriction: "Omezte maximální energii na jedno nabití",
                    NoConnection: {
                        Description: "Nelze jej připojit k nabíjecí stanici.",
                        Help1: "Zkontrolujte, zda je nabíjecí stanice zapnutá a zda je dostupná prostřednictvím sítě",
                        Help1_1: "Při opětovném zapnutí se objeví IP nabíjecí stanice"
                    },
                    OptimizedChargeMode: {
                        Name: "Optimalizované zatížení",
                        ShortName: "automaticky",
                        Info: "V tomto režimu je zatížení vozidla přizpůsobeno aktuální výrobě a spotřebě.",
                        MinInfo: "Pokud chcete zabránit tomu, aby se auto nenabíjelo v noci, můžete nastavit minimální poplatek.",
                        MinCharging: "Garance minimálního poplatku",
                        MinChargePower: "nakládací sazba",
                        ChargingPriority: {
                            Info: "V závislosti na prioritizaci bude vybraná komponenta načtena jako první",
                            Car: "Car",
                            Storage: "Storage"
                        }
                    },
                    ForceChargeMode: {
                        Name: "Nucené nakládání",
                        ShortName: "Ruční",
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
            SelectedDay: "{{value}}",
            Today: "Dnes",
            Yesterday: "Včera",
            LastWeek: "Poslední týden",
            LastMonth: "Poslední měsíc",
            LastYear: "Poslední rok",
            Go: "Jdi!",
            Export: "stáhnout jako soubor programu Excel",
            Day: "Den",
            Week: "Týden",
            Month: "Měsíc",
            Year: "Rok",
            noData: "data nejsou k dispozici",
            activeDuration: "aktivní trvání",
            BeginDate: "Vyberte datum zahájení",
            EndDate: "Vyberte datum ukončení",
            Sun: "Ned",
            Mon: "Pon",
            Tue: "Úte",
            Wed: "Stř",
            Thu: "Čtv",
            Fri: "Pát",
            Sat: "Sob",
            Jan: "Led",
            Feb: "Úno",
            Mar: "Bře",
            Apr: "Dub",
            May: "Kvě",
            Jun: "ČeN",
            Jul: "ČeC",
            Aug: "Srp",
            Sep: "Zář",
            Oct: "Říj",
            Nov: "Lis",
            Dec: "Pro"
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
                DataStorage: "Ukládání dat",
                SystemExecute: "Spusťte příkaz systému"
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
            },
            Kaco: {
                ChangePassword: "Change inverter password",
                EnterNewPassword: "Enter new inverter password",
                UpdatePassword: "Update password",
                UpdateSuccess: "Succesfully updated password!",
                UpdateError: "Error updating the password"
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
