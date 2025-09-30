export class ChannelAddress {

    constructor(
        public readonly componentId: string,
        public readonly channelId: string,
    ) { }

    /**
     * Parses a string to a ChannelAddress
     *
     * @param address in the form 'Component-ID/Channel-ID'
     */
    public static fromString(address: string): ChannelAddress {
        const array = ADDRESS.SPLIT("/", 2);
        return new ChannelAddress(array[0], array[1]);
    }

    /**
     * Parses a string to a ChannelAddress
     *
     * @param address in the form 'Component-ID/Channel-ID'
     */
    public static fromStringSafely(address: string | null): ChannelAddress | null {
        if (address == null) {
            return null;
        }
        const array = ADDRESS.SPLIT("/", 2);
        return new ChannelAddress(array[0], array[1]);
    }

    public toString() {
        return THIS.COMPONENT_ID + "/" + THIS.CHANNEL_ID;
    }
}
