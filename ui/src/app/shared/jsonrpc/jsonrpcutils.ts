import { ChannelAddress } from "../type/channeladdress";
import { ObjectUtils } from "../utils/object/object-utils";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "./base";

export class JsonRpcUtils {
    private static THRESHOLD: number = -0.5;

    /**
     * Gets the most inner/most deeply nested request.
     *
     * @param request The request
     * @returns The most inner request
     */
    public static getMostInnerRequest(
        request: JsonrpcRequest,
    ): JsonrpcRequest | null {
        let innerReq: JsonrpcRequest | null = request;
        let condition = ObjectUtils.getValueByKeySafely(
            innerReq?.params as any,
            "payload",
        );
        while (condition != null) {
            innerReq = condition;
            condition = ObjectUtils.getValueByKeySafely(
                innerReq?.params as any,
                "payload",
            );
        }
        return innerReq;
    }

    public static normalizeQueryData(
        data: (number | null)[],
    ): (number | null)[] {
        return data.map((el) => JsonRpcUtils.roundSlightlyNegativeValues(el));
    }

    /**
     * Rounds values between 0 and -1kW to 0
     *
     * @param value The value to convert
     */
    public static roundSlightlyNegativeValues(
        value: number | null,
    ): number | null {
        if (value == null) {
            return null;
        }

        return value > JsonRpcUtils.THRESHOLD && value < 0 ? 0 : value;
    }

    /**
     * Converts an array of ChannelAddresses to a string array with unique
     * values.
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
     * @param promise The promise
     * @returns Either an error or the result
     */
    public static handle<T = JsonrpcRequest>(
        promise: Promise<T>,
    ): Promise<[Error | null, T | null]> {
        return promise
            .then((data): [null, T] => [null, data])
            .catch((err: Error): [Error, null] => [err, null]);
    }

    /**
     * Handles a jsonRpcRequests, with fallback value if error thrown
     *
     * @param promise The promise
     * @param orElse The default value to use, if err thrown
     * @returns Either the the result or if error thrown the fallback value
     *   orElse
     */
    public static handleOrElse<T = JsonrpcRequest>(
        promise: Promise<T>,
        orElse: T,
    ): Promise<[null | Error, T]> {
        return promise
            .then((data): [null, T] => [null, data])
            .catch((err): [Error, T] => [err, orElse]);
    }

    /**
     * Handles a jsonRpcRequests, with fallback value if error thrown
     *
     * @param promise The promise
     * @param orElse The default value to use, if err thrown
     * @returns Either the the result or if error thrown the fallback value
     *   orElse
     */
    public static handleResponse<T extends JsonrpcResponseSuccess>(
        promise: Promise<T>,
    ): Promise<[null | Error, T | null]> {
        return promise
            .then((data): [null, T] => [null, data])
            .catch((err): [Error, null] => [err, null]);
    }
}
