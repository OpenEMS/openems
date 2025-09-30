import { ChannelAddress } from "./channeladdress";

/**
 * Holds subscribed 'CurrentData' provided by AbstractFlatWidget.
 */
export interface CurrentData {
    allComponents: {
        [channelAddress: string]: any
    };
}

export namespace CurrentDataUtils {
    export function getChannel<T>(address: ChannelAddress | null, allComponents: CurrentData["allComponents"]): T | null {
        if (address == null || address?.toString() == null || allComponents == null) {
            return null;
        }

        return allComponents[ADDRESS.TO_STRING()];
    };
}
