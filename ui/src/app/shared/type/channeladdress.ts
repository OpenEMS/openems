export class ChannelAddress {

    /**
     * Parses a string to a ChannelAddress
     *
     * @param address in the form 'Component-ID/Channel-ID'
     */
    public static fromString(address: string): ChannelAddress {
        const array = address.split('/', 2);
        return new ChannelAddress(array[0], array[1]);
    }

    constructor(
        public readonly componentId: string,
        public readonly channelId: string,
    ) { }

    public toString() {
        return this.componentId + "/" + this.channelId;
    }
}
