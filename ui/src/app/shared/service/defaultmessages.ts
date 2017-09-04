import { UUID } from 'angular2-uuid';
import * as moment from 'moment';

import { DefaultTypes } from './defaulttypes';

export class DefaultMessages {
    public static configQuery() {
        return {
            device: String,
            id: [UUID.UUID()],
            config: {
                mode: "query",
                language: 'de'
            }
        }
    };

    public static currentDataSubscribe(channels: DefaultTypes.ChannelAddresses) {
        return {
            device: String,
            id: ["currentData"],
            currentData: {
                mode: "subscribe",
                channels: channels
            }
        }
    };

    public static historicDataQuery(fromDate: moment.Moment, toDate: moment.Moment, timezone: number /*offset in seconds*/, channels: DefaultTypes.ChannelAddresses) {
        return {
            device: String,
            id: [UUID.UUID()],
            historicData: {
                mode: "query",
                fromDate: fromDate.format('YYYY-MM-DD'),
                toDate: toDate.format('YYYY-MM-DD'),
                timezone: timezone,
                channels: channels
                // TODO
                // kwhChannels: {
                //     address: 'grid' | 'production' | 'storage',
                // }
            }
        }
    };
}

export module DefaultMessages {
    export interface Reply {
        id: string[]
    }

    export interface ConfigQueryReply extends Reply {
        config: DefaultTypes.Config
    }

    export interface CurrentDataReply extends Reply {
        currentData: DefaultTypes.Data
    }

    export interface HistoricDataReply extends Reply {
        historicData: DefaultTypes.HistoricData
    }
}