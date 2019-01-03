import { ChannelAddress } from "../../type/channeladdress";

export class JsonRpcUtils {

    public static channelsToStringArray(channels: ChannelAddress[]): string[] {
        let result = [];
        for (let channel of channels) {
            result.push(channel.toString());
        }
        return result;
    }

}