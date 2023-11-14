import { TranslateService } from '@ngx-translate/core';
import { Filter } from 'src/app/index/filter/filter.component';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
export { environment } from './dummy';

export type Theme = 'OpenEMS' | 'FENECON' | 'Heckert';

export interface Environment {
    readonly theme: Theme;

    readonly uiTitle: string;
    readonly edgeShortName: string;
    readonly edgeLongName: string;

    readonly url: string;
    readonly backend: DefaultTypes.Backend;

    readonly production: boolean;
    debugMode: boolean;

    readonly docsUrlPrefix: string;
    readonly links: {

        readonly COMMON_STORAGE: string,
        readonly FORGET_PASSWORD: string,
        readonly EVCS_KEBA_KECONTACT: string,
        readonly EVCS_HARDY_BARTH: string,
        readonly EVCS_OCPP_IESKEYWATTSINGLE: string,

        readonly CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE: string,
        readonly CONTROLLER_CHP_SOC: string
        readonly CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD: string,
        readonly CONTROLLER_IO_FIX_DIGITAL_OUTPUT: string,
        readonly CONTROLLER_IO_HEAT_PUMP_SG_READY: string,
        readonly CONTROLLER_IO_HEATING_ELEMENT: string,

        readonly CONTROLLER_API_MODBUSTCP_READ: string,
        readonly CONTROLLER_API_MODBUSTCP_READWRITE: string,

        readonly CONTROLLER_API_REST_READ: string,
        readonly CONTROLLER_API_REST_READWRITE: string,

        readonly SETTINGS_ALERTING: string,
        readonly SETTINGS_NETWORK_CONFIGURATION: string,

        readonly warranty: {
            readonly home: {
                readonly EN: string,
                readonly DE: string,
            },
            readonly commercial: {
                readonly EN: string,
                readonly DE: string,
            }
        }

        readonly gtc: {
            readonly EN: string,
            readonly DE: string
        },

        readonly METER_SOCOMEC: string,

        readonly MANUALS: {
            readonly HOME: {
                readonly EN: string,
                readonly DE: string,
            },
            readonly COMMERCIAL: {
                readonly EN: string,
                readonly DE: string,
            }
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
        }
    },
    readonly PRODUCT_TYPES: (translate: TranslateService) => Filter
}