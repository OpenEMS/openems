export const TRANSLATION = {
    General: {
        Grid: "Red",
        GridBuy: "Relación",
        GridSell: "Fuente de alimentación",
        OffGrid: "No hay conexión de red",
        Production: "Producción",
        Consumption: "Consumo",
        Power: "Rendimiento",
        StorageSystem: "Almacenamiento",
        History: "Historia",
        NoValue: "Sin valor",
        Soc: "Cargo",
        Percentage: "Por ciento",
        More: "Más...",
        ChargePower: "Carga",
        DischargePower: "Descarga",
        ActualPower: "e-car Carga",
        PeriodFromTo: "de {{value1}} para {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "dd.MM.yyyy", // e.g. German: dd.MM.yyyy, English: yyyy-MM-dd (dd = Day, MM = Month, yyyy = Year)
        Search: "Búsqueda",
        Week: {
            Monday: "Lunes",
            Tuesday: "Martes",
            Wednesday: "Miércoles",
            Thursday: "Jueves",
            Friday: "Viernes",
            Saturday: "Sábado",
            Sunday: "Domingo"
        }
    },
    Menu: {
        Index: "Visión general",
        AboutUI: "Sobre OpenEMS-UI",
        Settings: 'Configuración general',
        Logout: 'Desuscribirse'
    },
    Index: {
        AllConnected: "Todas las conexiones establecidas.",
        ConnectionSuccessful: "Conexión a {{value}} hecho.", // value = name of websocket
        ConnectionFailed: "Conexión a {{value}} seperados.", // value = name of websocket
        ToEnergymonitor: "Al monitor de energía...",
        IsOffline: "OpenEMS está fuera de línea!"
    },
    Edge: {
        Index: {
            Energymonitor: {
                Title: "Monitor de energía",
                ConsumptionWarning: "Consumo y productores desconocidos",
                Storage: "Memoria",
                ReactivePower: "Potencia reactiva",
                ActivePower: "Potencia de salida",
                GridMeter: "Medidor de potencia",
                ProductionMeter: "Contador de generación",
                StorageDischarge: "Descarga de memoria",
                StorageCharge: "Carga del almacenaje"
            },
            Energytable: {
                Title: "Zabla de energía",
                LoadingDC: "Cargando DC",
                ProductionDC: "Generación DC"
            },
            Widgets: {
                EVCS: {
                    ChargingStation: "Carga",
                    Status: "Status",
                    Starting: "Comenzó",
                    NotReadyForCharging: "No está liesto para la carga",
                    ReadyForCharging: "Listo para cargar",
                    Charging: "Inicio de Carga",
                    Error: "Error",
                    NotAuthorized: "No autorizado",
                    Unplugged: "No conectado",
                    ChargingStationPluggedIn: "Estación de carga encufada",
                    ChargingStationPluggedInLocked: "Estación de carga enchufada + bloqueado",
                    ChargingStationPluggedInEV: "Estación de carga + e-Car enchufado",
                    ChargingStationPluggedInEVLocked: "Estación de carga + e-Car enchufado + bloqueando",
                    ChargingLimit: "Límite de carga",
                    ChargingPower: "Energía de carga",
                    CurrentCharge: "Carga actual",
                    TotalCharge: "Carga total",
                    EnforceCharging: "Forzar la carga",
                    Cable: "Cable"
                }
            }
        },
        History: {
            SelectedPeriod: "Período seleccionado: ",
            OtherPeriod: "Otro período",
            Period: "Período",
            Today: "Hoy",
            Yesterday: "Ayer",
            LastWeek: "La semana pasada",
            LastMonth: "El me pasado",
            LastYear: "El año pasado",
            Go: "Nwo!"
        },
        Config: {
            Index: {
                Bridge: "Conexiones y dispositivos",
                Scheduler: "Planificador de aplicaciones",
                Controller: "Aplicaciones",
                Simulator: "Simulador",
                ExecuteSimulator: "Ejecutar simulaciones",
                Log: "Registro",
                LiveLog: "Protocolos de sistema de vida",
                ManualControl: "Control manual",
                DataStorage: "Almacenamiento de datos"
            },
            More: {
                ManualCommand: "Comando manual",
                Send: "Enviar",
                RefuInverter: "REFU Inversor",
                RefuStartStop: "Iniciar/detener inversor",
                RefuStart: "Empezar",
                RefuStop: "Parada",
                ManualpqPowerSpecification: "Especificaciones de rendimiento",
                ManualpqSubmit: "Tomar",
                ManualpqReset: "Restablecer"
            },
            Scheduler: {
                NewScheduler: "Nuevo programador...",
                Class: "Clase:",
                NotImplemented: "Formulario no implementado: ",
                Contact: "Eso no debería suceder. Póngase es contacto con <a href=\"mailto:{{value}}\">{{value}}</a>.",
                Always: "Siempre"
            },
            Log: {
                AutomaticUpdating: "Actualización automática",
                Timestamp: "Hora",
                Level: "Nivel",
                Source: "Ésos",
                Message: "Mensaje"
            },
            Controller: {
                InternallyID: "Interno ID:",
                App: "Aplicación:",
                Priority: "Priodad:"
            },
            Bridge: {
                NewDevice: "Nuevo dispositivo...",
                NewConnection: "Nueva conexión..."
            }
        }
    },
    About: {
        UI: "Interfaz de usario para OpenEMS",
        Developed: "Esta interfaz de usario es desarrollada por FENECON como software de código abierto.",
        Fenecon: "Acerca de FENECON",
        OpenEMS: "Acerca de OpenEMS",
        CurrentDevelopments: "Desarrollos actuales",
        Build: "Esta compilación",
        Contact: "Para preguntas y sugerencias sobre el sistema, por favor contacte a nuestro OpenEMS-Team en <a href=\"mailto:{{value}}\">{{value}}</a>.",
        Language: "Seleccionar idioma:"
    },
    Notifications: {
        Failed: "Error al configurar la conexión.",
        LoggedInAs: "Conectado como usuario \"{{value}}\".", // value = username
        LoggedIn: "Registrado.",
        AuthenticationFailed: "Sin conexión: error de autenticación.",
        Closed: "Conexión terminada."
    }
}
