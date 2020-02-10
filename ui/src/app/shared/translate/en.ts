export const TRANSLATION = {
    General: {
        Mode: "Mode",
        Automatic: "Automatically",
        State: "State",
        On: "On",
        Off: "Off",
        Active: "Active",
        currentValue: 'current value',
        Inactive: "Inactive",
        Manually: "Manually",
        Phase: "Phase",
        Phases: "Phases",
        Autarchy: "Autarchy",
        SelfConsumption: "Self Consumption",
        Cumulative: "Cumulative Values",
        Grid: "Grid",
        GridBuy: "Buy from grid",
        GridSell: "Sell to grid",
        GridBuyAdvanced: "Buy",
        GridSellAdvanced: "Sell",
        OffGrid: "No Grid Connection!",
        Production: "Production",
        Consumption: "Consumption",
        otherConsumption: "other Consumption",
        Load: "Load",
        Power: "Power",
        StorageSystem: "Storage System",
        History: "History",
        Live: 'Live',
        NoValue: "No value",
        Soc: "State of charge",
        Percentage: "Percentage",
        More: "More...",
        ChargePower: "Charge power",
        DischargePower: "Discharge power",
        ChargeDischarge: "Charge/Discharge power",
        ActualPower: "e-car Charge power",
        PeriodFromTo: "from {{value1}} to {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "yyyy-MM-dd", // e.g. German: dd.MM.yyyy (dd = Day, MM = Month, yyyy = Year)
        Search: "Search",
        ChangeAccepted: "Change accepted",
        ChangeFailed: "Change failed",
        Week: {
            Monday: "Monday",
            Tuesday: "Tuesday",
            Wednesday: "Wednesday",
            Thursday: "Thursday",
            Friday: "Friday",
            Saturday: "Saturday",
            Sunday: "Sunday"
        },
        ReportValue: "Report corrupted data",
        Capacity: "Capacity"
    },
    Menu: {
        Index: "Index",
        AboutUI: "About OpenEMS UI",
        GeneralSettings: 'General Settings',
        EdgeSettings: 'FEMS Settings',
        Menu: 'Menu',
        Overview: 'FEMS Overview',
        Logout: 'Sign Out'
    },
    Index: {
        AllConnected: "All connections established.",
        ConnectionSuccessful: "Successfully connected to {{value}}.", // value = name of websocket
        ConnectionFailed: "Connection to {{value}} failed.", // value = name of websocket
        ToEnergymonitor: "To Energymonitor...",
        IsOffline: "OpenEMS is offline!"
    },
    Edge: {
        Index: {
            Energymonitor: {
                Title: "Energymonitor",
                ConsumptionWarning: "Consumption & unknown producers",
                Storage: "Storage",
                ReactivePower: "Reactive power",
                ActivePower: "Active power",
                GridMeter: "Grid meter",
                ProductionMeter: "Production meter",
                StorageDischarge: "Storage-Discharge",
                StorageCharge: "Storage-Charge"
            },
            Energytable: {
                Title: "Energytable",
                LoadingDC: "Loading DC",
                ProductionDC: "Production DC"
            },
            Widgets: {
                Channeltreshold: {
                    Output: "Output"
                },
                Peakshaving: {
                    peakshaving: 'Peak-Shaving',
                    peakshavingPower: 'Discharge above',
                    rechargePower: 'Charge below',
                    relationError: 'Discharge value must be greater than the Charge value',
                    asymmetricInfo: 'The entered performance values ​​refer to individual phases. It is adjusted to the most stressed phase.'
                },
                phasesInfo: "For technical reasons, the sum of the individual phases can be slightly different from the total sum.",
                autarchyInfo: "Autarky indicates the percentage of current power that can be covered by generation and storage discharge.",
                selfconsumptionInfo: "Self-consumption indicates the percentage of the currently generated output that can be used by direct consumption and storage load itself.",
                twoWayInfoStorage: "Negative Werte entsprechen Speicher Beladung, Positive Werte entsprechen Speicher Entladung",
                twoWayInfoGrid: "Negative Werte entsprechen Netzeinspeisung, Positive Werte entsprechen Netzbezug",
                CHP: {
                    LowThreshold: "Low Threshold",
                    HighThreshold: "High Threshold"
                },
                EVCS: {
                    ChargingStation: "Charging Station",
                    ChargingStationCluster: "Charging station cluster",
                    OverviewChargingStations: "Overview charging stations",
                    ChargingStationDeactivated: "Charging station deactivated",
                    Prioritization: "Prioritization",
                    Status: "Status",
                    Starting: "Starting",
                    NotReadyForCharging: "Not ready for charging",
                    ReadyForCharging: "Ready for charging",
                    Charging: "Is charing",
                    NotCharging: "Not charging",
                    Error: "Error",
                    NotAuthorized: "Not authorized",
                    Unplugged: "Unplugged",
                    ChargeLimitReached: "Charge limit reached",
                    ChargingStationPluggedIn: "Charing Station plugged in",
                    ChargingStationPluggedInLocked: "Charing Station plugged in + locked",
                    ChargingStationPluggedInEV: "Charing Station + E-Vehicel plugged in",
                    ChargingStationPluggedInEVLocked: "Charing Station + E-Vehicel plugged in + locked",
                    ChargingLimit: "Charging limit",
                    ChargingPower: "Charing power",
                    AmountOfChargingStations: "Amount of charging stations",
                    TotalChargingPower: "Total charging power",
                    CurrentCharge: "Current charge",
                    TotalCharge: "Total charge",
                    EnforceCharging: "Enforce charging",
                    Cable: "Cable",
                    CableNotConnected: "Cable is not connected",
                    CarFull: "Car is full",
                    EnergieSinceBeginning: "Energy since the last charge start",
                    ChargeMode: "Charge Mode",
                    ActivateCharging: "Activate the charging station",
                    ClusterConfigError: "An error has occurred in the configuration of the Evcs cluster",
                    EnergyLimit: "Energy Limit",
                    MaxEnergyRestriction: "Limit maximum energy per charge",
                    NoConnection: {
                        Description: "No connection to the charging station.",
                        Help1: "Check if the charging station is switched on and can be reached via the network.",
                        Help1_1: "The IP of the charging station appears when switching on again"
                    },
                    OptimizedChargeMode: {
                        Name: "Optimized charging",
                        ShortName: "Automatically",
                        Info: "In this mode, the load of the car is adjusted to the current production and consumption.",
                        MinInfo: "If you want to prevent that the car is not charging at the night, you could set a minimum charge.",
                        MinCharging: "Guarantee minimum charge",
                        MinChargePower: "Loading rate",
                        ChargingPriority: {
                            Info: "Depending on the prioritization, the selected component will be loaded first",
                            Car: "Car",
                            Storage: "Storage"
                        }
                    },
                    ForceChargeMode: {
                        Name: "Force charging",
                        ShortName: "Manually",
                        Info: "In this mode the loading of the car is enforced, i.e. it is always guaranteed that the car will be charged, even if the charging station needs to access grid power.",
                        MaxCharging: "Maximum charging power",
                        MaxChargingDetails: "If the car can not load the entered maximum value, the power will be automatically limited."
                    }
                }
            }
        },
        History: {
            SelectedPeriod: "Selected period: ",
            OtherPeriod: "Other period",
            Period: "Period",
            SelectedDay: "{{value}}",
            Today: "Today",
            Yesterday: "Yesterday",
            LastWeek: "Last week",
            LastMonth: "Last month",
            LastYear: "Last year",
            Go: "Go!",
            Export: "download as excel file",
            Day: "Day",
            Week: "Week",
            Month: "Month",
            year: "Year",
            noData: "No data available",
            activeDuration: "active duration",
            BeginDate: "Select Begin Date",
            EndDate: "Select End Date",
            Sun: "Sun",
            Mon: "Mon",
            Tue: "Tue",
            Wed: "Wed",
            Thu: "Thu",
            Fri: "Fri",
            Sat: "Sat",
            Jan: "Jan",
            Feb: "Feb",
            Mar: "Mar",
            Apr: "Apr",
            May: "May",
            Jun: "Jun",
            Jul: "Jul",
            Aug: "Aug",
            Sep: "Sep",
            Oct: "Oxt",
            Nov: "Nov",
            Dec: "Dec"
        },
        Config: {
            Index: {
                Bridge: "Connections and devices",
                Scheduler: "Applicationplanner",
                Controller: "Applications",
                Simulator: "Simulator",
                ExecuteSimulator: "Execute simulations",
                Log: "Log",
                LiveLog: "Live system log",
                AddComponents: "Install components",
                AdjustComponents: "Configure components",
                ManualControl: "Manual control",
                DataStorage: "Data Storage",
                SystemExecute: "Execute system command"
            },
            More: {
                ManualCommand: "Manual command",
                Send: "Send",
                RefuInverter: "REFU Inverter",
                RefuStartStop: "Start/Stop inverter",
                RefuStart: "Start",
                RefuStop: "Stop",
                ManualpqPowerSpecification: "Power specification",
                ManualpqSubmit: "Submit",
                ManualpqReset: "Reset"
            },
            Scheduler: {
                NewScheduler: "New scheduler...",
                Class: "Class:",
                NotImplemented: "Form not implemented: ",
                Contact: "This shouldn't happen. Please contact <a href=\"mailto:{{value}}\">{{value}}</a>.",
                Always: "Always"
            },
            Log: {
                AutomaticUpdating: "Automatic updating",
                Timestamp: "Timestamp",
                Level: "Level",
                Source: "Source",
                Message: "Message"
            },
            Controller: {
                InternallyID: "Internally ID:",
                App: "App:",
                Priority: "Priority:"
            },
            Bridge: {
                NewDevice: "New device...",
                NewConnection: "New connection..."
            }
        }
    },
    About: {
        UI: "User interface for OpenEMS",
        Developed: "This user interface is developed as open-source software.",
        OpenEMS: "More about OpenEMS",
        CurrentDevelopments: "Current developments",
        Build: "This build",
        Contact: "Please contact our team for further information or suggestions about the system at <a href=\"mailto:{{value}}\">{{value}}</a>.",
        Language: "Select language:"
    },
    Notifications: {
        Failed: "Connection failed.",
        LoggedInAs: "Logged in as \"{{value}}\".", // value = username
        LoggedIn: "Logged in.",
        AuthenticationFailed: "No Connection: Authentication failed.",
        Closed: "Connection closed."
    }
}
