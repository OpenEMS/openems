export const TRANSLATION = {
    General: {
        Cumulative: "Cumulative Values",
        Grid: "Grid",
        GridBuy: "Buy from grid",
        GridSell: "Sell to grid",
        OffGrid: "No Grid Connection!",
        Production: "Production",
        Consumption: "Consumption",
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
        ActualPower: "e-car Charge power",
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
        },
        ReportValue: "Report corrupted data",
        Capacity: "Capacity"
    },
    Menu: {
        Index: "Index",
        AboutUI: "About OpenEMS UI",
        GeneralSettings: 'General Settings',
        EdgeSettings: 'hy-control Settings',
        Menu: 'Menu',
        Overview: 'hy-control Overview',
        Logout: 'Sign Out'
    },
    Index: {
        AllConnected: "All connections established.",
        ConnectionSuccessful: "Successfully connected to {{value}}.", // value = name of websocket
        ConnectionFailed: "Connection to {{value}} failed.", // value = name of websocket
        ToEnergymonitor: "To Energymonitor...",
        IsOffline: "hy-control is offline!",
        PleaseLogin: "Please enter your access data or confirm to log in.",
        Username: "Username/email",
        Password: "Password",
        LostPassword: "Password lost",
        FormInvalid: "Please fill out the form completely",
        Connecting: "Connecting...",
        LoginWrong: "Wrong username / password.",
        NotOnline: "The device is not connected.",
        Type: "Type:",
        ConnectedAs: "Connected as:",
        MoreDevices: "There are more devices... Please adjust the filter.",
        SaveLogin: "Save login data."
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
                Info: "For technical reasons, the sum of the individual phases can be slightly different from the total sum.",
                CHP: {
                    LowThreshold: "Low Threshold",
                    HighThreshold: "High Threshold"
                },
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
                    Cable: "Cable",
                    CableNotConnected: "Cable is not connected",
                    CarFull: "Car is full",
                    EnergieSinceBeginning: "Energy since the begin of charge",
                    ChargeMode: "Charge Mode",
                    ActivateCharging: "Activate the charging station",
                    NoConnection: {
                        Description: "No connection to the charging station.",
                        Help1: "Check if the charging station is switched on and can be reached via the network.",
                        Help1_1: "The IP of the charging station appears when switching on again"
                    },
                    OptimizedChargeMode: {
                        Name: "Optimized charging",
                        ShortName: "Optimized",
                        Info: "In this mode, the load of the car is adjusted to the current production and consumption.",
                        MinInfo: "If you want to prevent that the car is not charging at the night, you could set a minimum charge.",
                        MinCharging: "Guarantee minimum charge?",
                        ChargingPriority: {
                            Info: "Depending on the prioritization, the selected component will be loaded first",
                            Car: "Car",
                            Storage: "Storage"
                        }
                    },
                    ForceChargeMode: {
                        Name: "Force charging",
                        ShortName: "Forced",
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
            Export: "download as excel file"
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