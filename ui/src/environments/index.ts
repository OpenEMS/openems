import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
export { environment } from './dummy';

export interface Environment {
    readonly theme: 'OpenEMS';

    readonly uiTitle: string;
    readonly edgeShortName: string;
    readonly edgeLongName: string;

    readonly url: string;
    readonly backend: DefaultTypes.Backend;

    readonly production: boolean;
    debugMode: boolean;
}
