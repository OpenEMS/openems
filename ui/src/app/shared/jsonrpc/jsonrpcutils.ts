import { ChannelAddress } from "../type/channeladdress";

export class JsonRpcUtils {

    /**
     * Converts an array of ChannelAddresses to a string array with unique values.
     */
    public static channelsToStringArray(channels: ChannelAddress[]): string[] {
        let result = [];
        for (let channel of channels) {
            result.push(channel.toString());
        }
        return Array.from(new Set(result));
    }

}