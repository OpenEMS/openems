import { ChannelAddress } from "../type/channeladdress";

export class JsonRpcUtils {

    private static THRESHOLD: number = -0.50;

    public static normalizeQueryData(data: (number | null)[]): (number | null)[] {
        return data.map(el => JsonRpcUtils.roundSlightlyNegativeValues(el));
    }

    /**
     * Rounds values between 0 and -1kW to 0
     *
     * @param value the value to convert
     */
    public static roundSlightlyNegativeValues(value: number | null): number | null {
        if (value == null) {
            return null;
        }

        return (value > JsonRpcUtils.THRESHOLD && value < 0) ? 0 : value;
    }


    /**
     * Converts an array of ChannelAddresses to a string array with unique values.
     */
    public static channelsToStringArray(channels: ChannelAddress[]): string[] {
        const result = [];
        for (const channel of channels) {
            result.push(channel.toString());
        }
        return Array.from(new Set(result));
    }
}
