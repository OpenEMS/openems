import { TranslateService } from '@ngx-translate/core';
import { Filter } from 'src/app/index/filter/filter.component';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
export { environment } from './dummy';

export type Theme = 'OpenEMS';

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
    },
    readonly PRODUCT_TYPES: (translate: TranslateService) => Filter
}