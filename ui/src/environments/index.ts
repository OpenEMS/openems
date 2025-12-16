import { TranslateService } from "@ngx-translate/core";
import { Filter } from "src/app/index/filter/filter.component";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
export { environment } from "./dummy";

export type Theme = "OpenEMS";
export type BaseMeta = Pick<Environment, "links" | "images">;

export interface Environment {
    readonly theme: Theme;

    readonly uiTitle: string;
    readonly edgeShortName: string;
    readonly edgeLongName: string;
    readonly defaultLanguage: string;

    readonly url: string;
    readonly backend: DefaultTypes.Backend;

    readonly production: boolean;
    debugMode: boolean;

    readonly docsUrlPrefix: string;
    readonly icons: {
        readonly "COMMON": {
            readonly "CONSUMPTION": string,
            readonly "SELFCONSUMPTION": string,
            readonly "GRID": string,
            readonly "GRID_STORAGE": string,
            readonly "GRID_RESTRICTION": string,
            readonly "MEGAFON": string,
            readonly "OFFGRID": string,
            readonly "PRODUCTION": string,
            readonly "STORAGE": string,

            readonly "WEATHER": {
                readonly "CLEAR_DAY": string,
                readonly "CLEAR_NIGHT": string,
                readonly "PARTLY_CLOUDY_DAY": string,
                readonly "PARTLY_CLOUDY_NIGHT": string,
                readonly "THUNDERSTORM": string,
                readonly "WEATHER_CLOUDY": string,
                readonly "WEATHER_FOGGY": string,
                readonly "WEATHER_MIX": string,
                readonly "WEATHER_RAINY": string,
                readonly "WEATHER_SNOWY": string,
                readonly "SUNSHINE_DURATION": string,
                readonly "HELP": string,
            },
        },
        readonly "COMPONENT": {
            readonly "HEATPUMP": string,
            readonly "EVCS": string,
        },
        readonly "STATUS": {
            readonly "CHECKMARK": string,
            readonly "ERROR": string,
            readonly "WARNING": string,
            readonly "INFO": string,
        },
    },
    readonly images: {
        readonly EVSE: {
            readonly KEBA_P30: string | null,
            readonly KEBA_P40: string | null,
            readonly HARDY_BARTH: string | null,
            readonly ALPITRONIC: string | null,
        },
    },
    readonly links: {
        readonly REDIRECT: {
            readonly BETA_CHANGE_LOG: string | null,
            readonly COMMON_STORAGE: string | null,
            readonly COMMON_AUTARCHY: string | null,
            readonly COMMON_CONSUMPTION: string | null,
            readonly COMMON_GRID: string | null,
            readonly COMMON_PRODUCTION: string | null,
            readonly COMMON_SELFCONSUMPTION: string | null,

            readonly EVCS_KEBA: string | null,
            readonly EVCS_HARDY_BARTH: string | null,
            readonly EVCS_MENNEKES: string | null,
            readonly EVCS_GO_E: string | null,
            readonly EVCS_IES: string | null,
            readonly EVCS_ALPITRONIC_HYPER: string | null,
        }


        readonly DATA_PROTECTION: string | null,
        readonly FORGET_PASSWORD: string,
        readonly EVCS: string | null,

        readonly CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE: string,
        readonly CONTROLLER_CHP_SOC: string
        readonly CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD: string,
        readonly CONTROLLER_IO_FIX_DIGITAL_OUTPUT: string,
        readonly CONTROLLER_IO_HEAT_PUMP_SG_READY: string,
        readonly CONTROLLER_IO_HEATING_ELEMENT: string,
        readonly CONTROLLER_ESS_TIME_OF_USE_TARIFF: string,

        readonly CONTROLLER_API_MODBUSTCP_READ: string,
        readonly CONTROLLER_API_MODBUSTCP_READWRITE: string,

        readonly CONTROLLER_API_REST_READ: string,
        readonly CONTROLLER_API_REST_READWRITE: string,

        readonly SETTINGS_ALERTING: string | null,
        readonly SETTINGS_NETWORK_CONFIGURATION: string | null,
        readonly EVCS_CLUSTER: string,

        readonly WARRANTY: {
            readonly HOME: {
                readonly EN: string,
                readonly DE: string,
            },
            readonly COMMERCIAL: {
                readonly EN: string,
                readonly DE: string,
            },
        }

        readonly GTC: {
            readonly EN: string,
            readonly DE: string
        },

        readonly METER: {
            readonly SOCOMEC: string,
            readonly KDK: string
        },

        readonly MANUALS: {
            readonly SYSTEM: {
                readonly HOME: {
                    readonly HOME_10: string,
                    readonly HOME_20_30: {
                        readonly DE: string,
                        readonly EN: string,
                    },
                    readonly HOME_GEN_2: {
                        readonly DE: string,
                        readonly EN: string,
                    },
                },
                readonly COMMERCIAL: {
                    readonly COMMERCIAL_30: string,
                    readonly COMMERCIAL_50_GEN_1: string,
                    readonly COMMERCIAL_50_GEN_3: string,
                    readonly COMMERCIAL_92: string,
                    readonly COMMERCIAL_92_CLUSTER: string,
                },
                readonly INDUSTRIAL?: {
                    S: string,
                    L: string
                }
            },

            readonly RUNDSTEUER: {
                readonly HOME: string,
                readonly HOME_GEN_2: string,
                readonly HOME_20_30: string,
                readonly COMMERCIAL_50_GEN_3: string,
            },

            readonly AVU: string,
        },

        APP_CENTER: {
            /**
             * Gets the image url of an OpenemsApp.
             *
             * The current order of the displayed image of an app is:
             * Image from edge -> Image from Url -> No image just the app name
             *
             * @param language the currently used language; can be obtained with {@link TranslateService#currentLang}
             * @param appId    the appId of the image
             * @returns the url of the image or null if not provided
             */
            APP_IMAGE: (language: string, appId: string) => string | null;
        },
        APP: {
            ANDROID: string | null,
            IOS: string | null,
        },
        readonly ENERGY_JOURNEY: {
            readonly HOME_10: {
                readonly DE: string,
                readonly EN: string,
            },
            readonly HOME_6_10_15: {
                readonly DE: string,
                readonly EN: string,
            },
            readonly HOME_20_30: {
                readonly DE: string,
                readonly EN: string,
            }
        },
        SYSTEM: {
            INDUSTRIAL_S: string,
            INDUSTRIAL_L: string,
        },
    },
    readonly PRODUCT_TYPES: (translate: TranslateService) => Filter | null,
}

/*
 * Return the proper websocket scheme ("ws" or "wss") depending on whether the page is accessed via HTTP or HTTPS.
 */
export function getWebsocketScheme(protocol: string = window.location.protocol): string {
    return protocol === "https:" ? "wss" : "ws";
}
