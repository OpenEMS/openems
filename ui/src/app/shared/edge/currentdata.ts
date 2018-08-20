import { DefaultTypes } from '../service/defaulttypes';

export class CurrentDataAndSummary {
    public summary: DefaultTypes.Summary;

    constructor(public data: DefaultTypes.Data) {

    }
}