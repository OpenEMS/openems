import { ChannelAddress } from "../type/channeladdress";
import { JsonrpcRequest } from "./base";

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

    /**
     * Handles jsonRpcRequests
     *
     * @param promise the promise
     * @returns either an error or the result
     */
    public static handle<T = JsonrpcRequest>(promise: Promise<T>): Promise<[Error | null, T | null]> {
        return promise
            .then((data): [null, T] => [null, data])
            .catch((err: Error): [Error, null] => [err, null]);
    }

    /**
     * Handles a jsonRpcRequests, with fallback value if error thrown
     *
     * @param promise the promise
     * @param orElse the default value to use, if err thrown
     * @returns either the the result or if error thrown the fallback value orElse
     */
    public static handleOrElse<T = JsonrpcRequest>(promise: Promise<T>, orElse: T): Promise<[null | Error, T]> {
        return promise
            .then((data): [null, T] => [null, data])
            .catch((err): [Error, T] => [err, orElse]);
    }
}
