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

        readonly SETTINGS_ALERTING: string,
        readonly SETTINGS_NETWORK_CONFIGURATION: string,
    }
}