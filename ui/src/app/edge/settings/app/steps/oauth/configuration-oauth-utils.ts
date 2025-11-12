import { ConnectionState } from "../../oauth/jsonrpc/getOAuthConnectionState";

/**
 * Maps the numeric channel value to a ConnectionState.
 *
 * @param channelValue the numeric value received from the channel
 * @returns
 */
export function mapChannelValueToConnectionState(channelValue: number): ConnectionState {
    switch (channelValue) {
        case -1: return "UNDEFINED";
        case 0: return "NOT_CONNECTED";
        case 1: return "EXPIRED";
        case 2: return "VALIDATING";
        case 3: return "CONNECTED";
    }
    return "UNDEFINED";
}
