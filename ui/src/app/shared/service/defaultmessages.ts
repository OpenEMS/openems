import { UUID } from 'angular2-uuid';
import { format } from 'date-fns';

import { DefaultTypes } from './defaulttypes';

export class DefaultMessages {

    public static authenticateLogin(password: string, username?: string) {
        let m = {
            authenticate: {
                mode: "login",
                password: password
            }
        };
        if (username) m.authenticate["username"] = username;
        return m;
    };

    public static authenticateLogout() {
        return {
            authenticate: {
                mode: "logout"
            }
        };
    };

    public static configQuery(edgeId: number): DefaultTypes.IdentifiedMessage {
        return {
            messageId: {
                ui: UUID.UUID()
            },
            edgeId: edgeId,
            config: {
                mode: "query",
                language: 'de'
            }
        }
    };

    public static configUpdate(edgeId: number, thingId: string, channelId: string, value: any): DefaultTypes.ConfigUpdate {
        return {
            messageId: {
                ui: UUID.UUID()
            },
            edgeId: edgeId,
            config: {
                mode: "update",
                thing: thingId,
                channel: channelId,
                value: value
            }
        }
    }

    public static currentDataSubscribe(edgeId: number, channels: DefaultTypes.ChannelAddresses): DefaultTypes.IdentifiedMessage {
        return {
            messageId: {
                ui: UUID.UUID()
            },
            edgeId: edgeId,
            currentData: {
                mode: "subscribe",
                channels: channels
            }
        }
    };

    public static historicDataQuery(edgeId: number, fromDate: Date, toDate: Date, timezone: number /*offset in seconds*/, channels: DefaultTypes.ChannelAddresses): DefaultTypes.IdentifiedMessage {
        return {
            messageId: {
                ui: UUID.UUID()
            },
            edgeId: edgeId,
            historicData: {
                mode: "query",
                fromDate: format(fromDate, 'YYYY-MM-DD'),
                toDate: format(toDate, 'YYYY-MM-DD'),
                timezone: timezone,
                channels: channels
                // TODO
                // kwhChannels: {
                //     address: 'grid' | 'production' | 'storage',
                // }
            }
        }
    };

    public static logSubscribe(edgeId: number): DefaultTypes.IdentifiedMessage {
        return {
            messageId: {
                ui: UUID.UUID()
            },
            edgeId: edgeId,
            log: {
                mode: "subscribe",
            }
        }
    };

    public static logUnsubscribe(edgeId: number): DefaultTypes.IdentifiedMessage {
        return {
            messageId: {
                ui: UUID.UUID()
            },
            edgeId: edgeId,
            log: {
                mode: "unsubscribe",
            }
        }
    };

    public static systemExecute(edgeId: number, password: string, command: string, background: boolean, timeout: number): DefaultTypes.IdentifiedMessage {
        return {
            messageId: {
                ui: UUID.UUID()
            },
            edgeId: edgeId,
            system: {
                mode: "execute",
                password: password,
                command: command,
                background: background,
                timeout: timeout
            }
        }
    };
}

export module DefaultMessages {

    export interface Reply extends DefaultTypes.IdentifiedMessage {
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

    export interface LogReply extends Reply {
        log: DefaultTypes.Log
    }

    export interface SystemExecuteReply extends Reply {
        system: {
            output: string
        }
    }
}