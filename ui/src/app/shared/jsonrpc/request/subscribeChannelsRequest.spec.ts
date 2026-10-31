import { ChannelAddress } from "src/app/shared/type/channeladdress";

import { SubscribeChannelsRequest } from "./subscribeChannelsRequest";

describe("SubscribeChannelsRequest", () => {
    const paramsOf = (request: SubscribeChannelsRequest) =>
        request.params as { count: number; channels: string[] };

    it("serializes the given channels as string array in params", () => {
        const request = new SubscribeChannelsRequest([
            new ChannelAddress("_sum", "ActivePower"),
            new ChannelAddress("meter0", "ActivePower"),
        ]);

        expect(request.method).toBe("subscribeChannels");
        expect(paramsOf(request).channels).toEqual(["_sum/ActivePower", "meter0/ActivePower"]);
        expect(typeof paramsOf(request).count).toBe("number");
    });

    it("increments the count on every new request", () => {
        const first = new SubscribeChannelsRequest([]);
        const second = new SubscribeChannelsRequest([]);

        expect(paramsOf(second).count).toBe(paramsOf(first).count + 1);
    });

    it("does not leak a 'channels' property onto the request", () => {
        const request = new SubscribeChannelsRequest([new ChannelAddress("_sum", "ActivePower")]);
        const serialized = JSON.parse(JSON.stringify(request));

        expect(Object.prototype.hasOwnProperty.call(request, "channels")).toBe(false);
        expect(serialized.channels).toBeUndefined();
        expect(serialized.params.channels).toEqual(["_sum/ActivePower"]);
    });
});
