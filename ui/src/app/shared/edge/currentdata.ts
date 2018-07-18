import { DefaultTypes } from '../service/defaulttypes';
import { ConfigImpl } from './config';
import { Utils } from '../service/utils';
import { Edge } from './edge';

export class CurrentDataAndSummary {
    public summary: DefaultTypes.Summary;

    constructor(public data: DefaultTypes.Data) {

    }
}