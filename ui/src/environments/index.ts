import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
export { environment } from './dummy';

export interface Environment {
    readonly theme: 'OpenEMS' | 'FENECON' | 'Heckert';

    readonly title: string;
    readonly shortName: string;

    readonly url: string;
    readonly backend: DefaultTypes.Backend;

    readonly production: boolean;
    debugMode: boolean;
}