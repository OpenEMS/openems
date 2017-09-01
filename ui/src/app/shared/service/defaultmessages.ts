import { UUID } from 'angular2-uuid';

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
}

export module DefaultMessages {
    export interface Reply {
        id: string[]
    }

    export interface ConfigQueryReply extends Reply {
        config: DefaultTypes.Config
    }

    export interface CurrentDataReply extends Reply {
        currentData: DefaultTypes.CurrentData
    }
}