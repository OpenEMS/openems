export const TRANSLATION = {
    General: {
        Grid: "Grid",
        GridBuy: "Buy from grid",
        GridSell: "Sell to grid",
        Production: "Production",
        Consumption: "Consumption",
        Power: "Power",
        StorageSystem: "Storage System",
        History: "History",
        NoValue: "No value",
        Soc: "State of charge",
        Percentage: "Percentage",
        More: "More...",
        ChargePower: "Charge power",
        DischargePower: "Discharge power",
        PeriodFromTo: "from {{value1}} to {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "YYYY-MM-DD", // e.g. German: DD.MM.YYYY (DD = Day, MM = Month, YYYY = Year)
        Search: "Search",
        Week: {
            Monday: "Monday",
            Tuesday: "Tuesday",
            Wednesday: "Wednesday",
            Thursday: "Thursday",
            Friday: "Friday",
            Saturday: "Saturday",
            Sunday: "Sunday"
        }
    },
    Menu: {
        Overview: "Overview",
        AboutUI: "About FEMS-UI"
    },
    Overview: {
        AllConnected: "All connections established.",
        ConnectionSuccessful: "Successfully connected to {{value}}.", // value = name of websocket
        ConnectionFailed: "Connection to {{value}} failed.", // value = name of websocket
        ToEnergymonitor: "To Energymonitor...",
        IsOffline: "FEMS is offline!"
    },
    Device: {
        Overview: {
            Energymonitor: {
                Title: "Energymonitor",
                ConsumptionWarning: "Consumption & unknown producers",
                Storage: "Storage",
                ReactivePower: "Reactive power",
                ActivePower: "Active power",
                GridMeter: "Grid meter",
                ProductionMeter: "Production meter",
                StorageDischarge: "Storage-Charge",
                StorageCharge: "Storage-Discharge"
            },
            Energytable: {
                Title: "Energytable"
            }
        },
        History: {
            SelectedPeriod: "Selected period: ",
            OtherPeriod: "Other period:",
            Period: "Period",
            Today: "Today",
            Yesterday: "Yesterday",
            LastWeek: "Last week",
            LastMonth: "Last month",
            LastYear: "Last year",
            Go: "Go!"
        },
        Config: {
            Overview: {
                Bridge: "Connections and devices",
                Scheduler: "Applicationplanner",
                Controller: "Applications",
                Simulator: "Simulator",
                ExecuteSimulator: "Execute simulations",
                Log: "Log",
                LiveLog: "Live system log",
                ManualControl: "Manual control",
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
                Contact: "This shouldn't happen. Please contact <a href=\"mailto:{{value}}\">{{value}}</a>.", // value = Mail from FEMS-Team
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
        UI: "User interface for FEMS and OpenEMS",
        Developed: "This user interface is developed by FENECON as open-source software.",
        Fenecon: "More about FENECON",
        Fems: "More about FEMS",
        Sourcecode: "Source code",
        CurrentDevelopments: "Current developments",
        Build: "This build",
        Contact: "Please contact our FEMS team for further information or suggestions about the system at <a href=\"mailto:{{value}}\">{{value}}</a>.", // value = Mail from FEMS-Team
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