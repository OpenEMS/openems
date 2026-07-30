import { alertCircleOutline, checkmarkDoneCircleOutline, flame, informationCircleOutline, warningOutline, } from "ionicons/icons";
import { BaseMeta } from "src/environments";

export const OemMeta: BaseMeta = {
    api: {
        SYSTEM_OUTAGE: null,
        CHANGELOG: {
            REMOTE: "assets/json/changelog.json",
            LOCAL: "assets/json/changelog.json",
        },
    },
    icons: {
        COMMON: {
            CONSUMPTION: "assets/img/icon/consumption.svg",
            SELFCONSUMPTION: "assets/img/icon/selfconsumption.svg",
            GENERATOR: "assets/img/icon/generator.svg",
            GRID: "assets/img/icon/grid.svg",
            GRID_STORAGE: "assets/img/icon/gridStorage.svg",
            GRID_RESTRICTION: "assets/img/icon/gridRestriction.svg",
            MEGAFON: "assets/img/icon/megafon.svg",
            OFFGRID: "assets/img/icon/offgrid.svg",
            PRODUCTION: "assets/img/icon/production.svg",
            STORAGE: "assets/img/icon/storage.svg",
            WEATHER: {
                CLEAR_DAY: "assets/img/icon/clear_day.svg",
                CLEAR_NIGHT: "assets/img/icon/clear_night.svg",
                PARTLY_CLOUDY_DAY: "assets/img/icon/partly_cloudy_day.svg",
                PARTLY_CLOUDY_NIGHT: "assets/img/icon/partly_cloudy_night.svg",
                THUNDERSTORM: "assets/img/icon/thunderstorm.svg",
                WEATHER_CLOUDY: "assets/img/icon/weather_cloudy.svg",
                WEATHER_FOGGY: "assets/img/icon/weather_foggy.svg",
                WEATHER_MIX: "assets/img/icon/weather_mix.svg",
                WEATHER_RAINY: "assets/img/icon/weather_rainy.svg",
                WEATHER_SNOWY: "assets/img/icon/weather_snowy.svg",
                SUNSHINE_DURATION: "assets/img/icon/sunshine_duration.svg",
                HELP: "assets/img/icon/help.svg",
            },
            OFFLINE: {
                CLOUD_OFFLINE_OUTLINE: "icons/common/offline/cloud_offline_outline.svg",
            },
            TIME_OF_USE: {
                TIME_OF_USE: "icons/common/time_of_use/time_of_use.svg",
                TIME_OF_USE_THIN: "icons/common/time_of_use/time_of_use-thin.svg",
            },
        },
        COMPONENT: {
            HEATPUMP: flame,
            EVCS: "assets/img/icon/evcs.svg",
            HEATING_ELEMENT: flame,
        },
        STATUS: {
            CHECKMARK: checkmarkDoneCircleOutline,
            ERROR: alertCircleOutline,
            WARNING: warningOutline,
            INFO: informationCircleOutline,
        },
        BATTERY_EXTENSION: null,
        ENERGY_JOURNEY: null,
        WRAP_UP: null,
    },
    images: {
        EVSE: {
            KEBA_P30: null,
            KEBA_P40: null,
            HARDY_BARTH: null,
            ALPITRONIC: null,
            MENNEKES: null,
            SIMULATOR: "assets/img/product_pictures/Evse.ChargePoint.Simulator.webp",
        },
        LOGO: {
            DARK: null,
            LIGHT: null,
        },
        HEAT: {
            ASKOMA: {
                HEATING_ELEMENT: null,
            },
            MYPV: {
                HEATING_ELEMENT: null,
            },
        },
        INSTALLATION: {
            COMMERCIAL: {
                COMMERCIAL_100: {
                    BATTERY_INPUT_1: null,
                    BATTERY_INPUT_2: null,
                },
            },
        },
    },
    links: {
        DATA_PROTECTION: null,
        REDIRECT: {
            BETA_SURVEY: null,
            BETA_CHANGE_LOG: null,
            COMMON_STORAGE: null,
            COMMON_AUTARCHY: null,
            COMMON_CONSUMPTION: null,
            COMMON_GRID: null,
            COMMON_PRODUCTION: null,
            COMMON_SELFCONSUMPTION: null,
            CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE: "io.openems.edge.controller.ess.gridoptimizedcharge/readme.adoc",
            CONTROLLER_IO_HEAT_PUMP_SG_READY: "io.openems.edge.controller.io.heatpump.sgready/readme.adoc",
            CONTROLLER_IO_HEATING_ELEMENT: "io.openems.edge.controller.io.heatingelement/readme.adoc",
            CONTROLLER_ESS_TIME_OF_USE_TARIFF: "io.openems.edge.controller.ess.timeofusetariff/readme.adoc",
            CONTROLLER_CHP_SOC: "io.openems.edge.controller.chp.soc/readme.adoc",
            CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD: "io.openems.edge.controller.io.channelsinglethreshold/readme.adoc",
            CONTROLLER_IO_FIX_DIGITAL_OUTPUT: "io.openems.edge.controller.io.fixdigitaloutput/readme.adoc",
            CONTROLLER_API_MODBUSTCP_READ: "io.openems.edge.controller.api.modbus/readme.adoc",
            CONTROLLER_API_MODBUSTCP_READWRITE: "io.openems.edge.controller.api.modbus/readme.adoc",
            CONTROLLER_API_REST_READ: "io.openems.edge.controller.api.rest/readme.adoc",
            CONTROLLER_API_REST_READWRITE: "io.openems.edge.controller.api.rest/readme.adoc",
            EVCS_KEBA: null,
            EVCS_HARDY_BARTH: null,
            EVCS_MENNEKES: null,
            EVCS_GO_E: null,
            EVCS_IES: null,
            EVCS_ALPITRONIC_HYPER: null,
            SETTINGS_ALERTING: null,
            SETTINGS_NETWORK_CONFIGURATION: null,
            WEATHER_WIDGET: null,
            SYSTEM: {
                INDUSTRIAL_S_MANUAL: null,
                INDUSTRIAL_M_MANUAL: null,
                INDUSTRIAL_L_OVERVIEW: null,
                INDUSTRIAL_XL_MANUAL: null,
            },
            WARRANTY: {
                HOME_GUARANTEE_CONDITIONS: {
                    EN: "#",
                    DE: "#",
                },
                COMMERCIAL_50_GEN3_GUARANTEE_CONDITIONS: {
                    EN: "#",
                    DE: "#",
                },
                COMMERCIAL_92_GUARANTEE_CONDITIONS: {
                    EN: "#",
                    DE: "#",
                },
                COMMERCIAL_100_GUARANTEE_CONDITIONS: {
                    EN: "#",
                    DE: "#",
                },
            },
            OFFLINE_INSTRUCTIONS: null,
        },
        FORGET_PASSWORD: "#",
        EVCS: null,

        EVCS_CLUSTER: "io.openems.edge.evcs.cluster/readme.adoc",

        SYSTEM: {
            INDUSTRIAL_S: "#",
            INDUSTRIAL_L: "#",
        },

        METER: {
            SOCOMEC: "#",
            KDK: "#",
        },

        MANUALS: {
            SYSTEM: {
                HOME: {
                    HOME_10: "#",
                    HOME_20_30: {
                        DE: "#",
                        EN: "#",
                    },
                    HOME_GEN_2: {
                        DE: "#",
                        EN: "#",
                    },
                },
                COMMERCIAL: {
                    COMMERCIAL_30: "#",
                    COMMERCIAL_50_GEN_1: "#",
                    COMMERCIAL_50_GEN_3: "#",
                    COMMERCIAL_92: "#",
                    COMMERCIAL_92_CLUSTER: "#",
                    COMMERCIAL_100: "#",
                },
            },

            RUNDSTEUER: {
                HOME: "#",
                HOME_GEN_2: "#",
                HOME_6_10_15: "#",
                HOME_20_30: "#",
                COMMERCIAL_50_GEN_3: "#",
            },

            AVU: "#",
        },
        APP_CENTER: {
            APP_IMAGE: (language: string, appId: string): string | null => {
                return null;
            },
        },
        APP: {
            ANDROID: null,
            IOS: null,
        },
        ENERGY_JOURNEY: {
            HOME_10: {
                DE: "#",
                EN: "#",
            },
            HOME_20_30: {
                DE: "#",
                EN: "#",
            },
            HOME_6_10_15: {
                DE: "#",
                EN: "#",
            },
        },
    },
};
