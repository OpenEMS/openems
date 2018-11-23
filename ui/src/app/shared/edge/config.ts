import { DefaultTypes } from '../service/defaulttypes'
import { Role } from '../type/role'
import { Widget } from '../type/widget';

export abstract class ConfigImpl implements DefaultTypes.Config {

    public esss: string[] = [];
    public chargers: string[] = [];

    public meta: {
        [factoryPid: string]: {
            implements: string[],
            channels?: {
                [channel: string]: {
                    name: string,
                    title: string,
                    type: string | string[],
                    optional: boolean,
                    array: boolean,
                    readRoles: Role[],
                    writeRoles: Role[],
                    defaultValue: string
                }
            }
        }
    };

    public abstract getImportantChannels(): DefaultTypes.ChannelAddresses;

    public abstract getEssSocChannels(): DefaultTypes.ChannelAddresses;

    public abstract getPowerChannels(): DefaultTypes.ChannelAddresses;

    public abstract getEvcsChannels(): DefaultTypes.ChannelAddresses;

    public abstract getWidgets(): Widget[];

}