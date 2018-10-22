export const TRANSLATION = {
    General: {
        Grid: "Grid",
        GridBuy: "Buy from grid",
        GridSell: "Sell to grid",
        GridMode: "No Grid Connection!",
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
        DateFormat: "yyyy-MM-dd", // e.g. German: dd.MM.yyyy (dd = Day, MM = Month, yyyy = Year)
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
        Index: "Index",
        AboutUI: "About PRIMUS-UI",
        Settings: 'General Settings',
        Logout: 'Sign Out'
    },
    Index: {
        AllConnected: "All connections established.",
        ConnectionSuccessful: "Successfully connected to {{value}}.", // value = name of websocket
        ConnectionFailed: "Connection to {{value}} failed.", // value = name of websocket
        ToEnergymonitor: "To Energymonitor...",
        IsOffline: "PRIMUS is offline!",
        PleaseLogin: "Please enter your access data or confirm to log in as a guest.",
        Username: "Username/email",
        Password: "Password",
        LostPassword: "Password lost",
        FormInvalid: "Please fill out the form completely",
        Connecting: "Connecting...",
        LoginWrong: "Wrong username / password."
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
                EVCS: {
                    ChargingStation: "Charging Station",
                    Status: "Status",
                    Starting: "Starting",
                    NotReadyForCharging: "Not ready for charging",
                    ReadyForCharging: "Ready for charging",
                    Charging: "Is charing",
                    Error: "Error",
                    NotAuthorized: "Not authorized",
                    Unplugged: "Unplugged",
                    ChargingStationPluggedIn: "Charing Station plugged in",
                    ChargingStationPluggedInLocked: "Charing Station plugged in + locked",
                    ChargingStationPluggedInEV: "Charing Station + E-Vehicel plugged in",
                    ChargingStationPluggedInEVLocked: "Charing Station + E-Vehicel plugged in + locked",
                    ChargingLimit: "Charging limit",
                    ChargingPower: "Charing power",
                    CurrentCharge: "Current charge",
                    TotalCharge: "Total charge",
                    EnforceCharging: "Enforce charging",
                    Cable: "Cable"
                }
            }
        },
        History: {
            SelectedPeriod: "Selected period: ",
            OtherPeriod: "Other period",
            Period: "Period",
            Today: "Today",
            Yesterday: "Yesterday",
            LastWeek: "Last week",
            LastMonth: "Last month",
            LastYear: "Last year",
            Go: "Go!"
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
                ManualControl: "Manual control",
                DataStorage: "Data Storage"
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
                Contact: "This shouldn't happen. Please contact <a href=\"mailto:{{value}}\">{{value}}</a>.", // value = Mail from PRIMUS-Team
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
        UI: "User interface for PRIMUS and OpenEMS",
        Developed: "This user interface is developed by KACO new energy as open-source software.",
        Fenecon: "More about KACO new energy",
        Fems: "More about PRIMUS",
        OpenEMS: "More about OpenEMS",
        CurrentDevelopments: "Current developments",
        Build: "This build",
        Contact: "Please contact our PRIMUS team for further information or suggestions about the system at <a href=\"mailto:{{value}}\">{{value}}</a>.", // value = Mail from PRIMUS-Team
        Language: "Select language:"
    },
    Notifications: {
        Failed: "Connection failed.",
        LoggedInAs: "Logged in as \"{{value}}\".", // value = username
        LoggedIn: "Logged in.",
        AuthenticationFailed: "No Connection: Authentication failed.",
        Closed: "Connection closed."
    },
    Alerts: {
        Error: "Error",
        Default: "Something went wrong. Please try again.",
        RetrievePwdHeader: "Lost password",
        RetrievePwdMsg: "Please insert your email / username to set a new password.",
        RetrievePwdPlaceholder: "email/username",
        Cancel: "Cancel",
        Send: "Send",
        RetrievePwdSent: "A link to reset your password has been sent. Please check your email.",
        RetrievePwdError: "An error occurd while retrieving your password. Please check your input and try again."
    }
}