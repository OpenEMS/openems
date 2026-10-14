import { Edge } from "src/app/shared/components/edge/edge";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { EdgeRpcRequest } from "src/app/shared/jsonrpc/request/edgeRpcRequest";
import { Websocket } from "src/app/shared/service/websocket";
import { ChannelAddress } from "src/app/shared/shared";

describe("Edge channel subscriptions", () => {
    let edge: Edge;
    let websocket: jasmine.SpyObj<Websocket>;

    /** Channels of all SubscribeChannelsRequests sent so far (after the 100 ms debounce). */
    const flush = async (): Promise<string[][]> => {
        await new Promise((resolve) => setTimeout(resolve, 150));
        return [...websocket.sendRequest.calls.allArgs(), ...websocket.sendStateFullRequest.calls.allArgs()].map(
            (args) => {
                const request = args[0] as unknown as EdgeRpcRequest;
                return (request.params.payload.params as { channels: string[] }).channels;
            },
        );
    };

    beforeEach(() => {
        websocket = jasmine.createSpyObj<Websocket>("Websocket", ["sendRequest", "sendStateFullRequest"]);
        websocket.sendRequest.and.resolveTo({ id: 0, jsonrpc: "2.0", result: {} } as never);
        websocket.sendStateFullRequest.and.resolveTo({ id: 0, jsonrpc: "2.0", result: {} } as never);
        edge = DummyConfig.dummyEdge({});
    });

    /**
     * Tests for the deprecated id-based {@link Edge.unsubscribeChannels}, which removes a whole subscription: the entry
     * must be removed and the reduced channel set re-sent, unless other subscriptions still cover all removed
     * channels.
     */
    describe("unsubscribeChannels", () => {
        it("removes the entry and resends the reduced channel set", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            edge.subscribeChannels(websocket, "b", [new ChannelAddress("c1", "Y")]);
            expect(await flush()).toEqual([["c0/X", "c1/Y"]]);

            edge.unsubscribeChannels(websocket, "b");

            const sent = await flush();
            expect(sent).toEqual([["c0/X", "c1/Y"], ["c0/X"]]);
            expect((edge as unknown as { subscribedChannels: object }).subscribedChannels).toEqual({
                a: [new ChannelAddress("c0", "X")],
            });
        });

        it("skips the request when other subscriptions still cover all removed channels", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            edge.subscribeChannels(websocket, "b", [new ChannelAddress("c0", "X")]);
            await flush();

            edge.unsubscribeChannels(websocket, "b");

            // "a" still covers "c0/X", so no further request may be sent
            expect((await flush()).length).toBe(1);
            expect((edge as unknown as { subscribedChannels: object }).subscribedChannels).toEqual({
                a: [new ChannelAddress("c0", "X")],
            });
        });

        it("is a safe no-op for unknown ids", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            await flush();

            edge.unsubscribeChannels(websocket, "unknown");

            expect((await flush()).length).toBe(1);
            expect((edge as unknown as { subscribedChannels: object }).subscribedChannels).toEqual({
                a: [new ChannelAddress("c0", "X")],
            });
        });
    });

    /**
     * Tests for {@link Edge.subscribeChannels} and {@link Edge.subscribeChannelsWithState}: a request must be sent
     * whenever the union of all subscriptions changes, i.e. when channels are added to or removed from the union - even
     * if the new entry covers all previously subscribed channels.
     */
    describe("subscribeChannels", () => {
        it("sends the request when a subscription adds channels to the union", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            expect(await flush()).toEqual([["c0/X"]]);

            // The new entry covers all previously subscribed channels and adds one: the union grows, so the
            // request must be sent even though the new entry contains every previous channel.
            edge.subscribeChannels(websocket, "b", [new ChannelAddress("c0", "X"), new ChannelAddress("c1", "Y")]);

            expect(await flush()).toEqual([["c0/X"], ["c0/X", "c1/Y"]]);
        });

        it("skips the request when the union of all subscriptions is unchanged", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            edge.subscribeChannels(websocket, "b", [new ChannelAddress("c0", "X")]);

            expect((await flush()).length).toBe(1);
        });

        it("keeps previously subscribed channels when the same id subscribes more channels", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c1", "Y")]);

            // Both calls coalesce into one debounced request, which must contain both channels
            expect(await flush()).toEqual([["c0/X", "c1/Y"]]);
        });

        it("skips the request when the same channels are subscribed again", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);

            expect((await flush()).length).toBe(1);
        });
    });

    /**
     * Tests for {@link Edge.unsubscribeFromChannels}, which subtracts channels from one subscription: only the passed
     * channels are removed; the subscription entry is removed entirely once its last channel is gone.
     */
    describe("unsubscribeFromChannels", () => {
        it("removes only the passed channels and resends the reduced channel set", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X"), new ChannelAddress("c1", "Y")]);
            expect(await flush()).toEqual([["c0/X", "c1/Y"]]);

            edge.unsubscribeFromChannels("a", websocket, [new ChannelAddress("c0", "X")]);

            expect(await flush()).toEqual([["c0/X", "c1/Y"], ["c1/Y"]]);
            expect((edge as unknown as { subscribedChannels: object }).subscribedChannels).toEqual({
                a: [new ChannelAddress("c1", "Y")],
            });
        });

        it("removes the entry when its last channel is unsubscribed", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            await flush();

            edge.unsubscribeFromChannels("a", websocket, [new ChannelAddress("c0", "X")]);

            expect(await flush()).toEqual([["c0/X"], []]);
            expect((edge as unknown as { subscribedChannels: object }).subscribedChannels).toEqual({});
        });

        it("is a safe no-op for unknown ids", async () => {
            edge.subscribeChannels(websocket, "a", [new ChannelAddress("c0", "X")]);
            await flush();

            edge.unsubscribeFromChannels("unknown", websocket, [new ChannelAddress("c0", "X")]);

            expect((await flush()).length).toBe(1);
            expect((edge as unknown as { subscribedChannels: object }).subscribedChannels).toEqual({
                a: [new ChannelAddress("c0", "X")],
            });
        });
    });

    describe("subscribeChannelsWithState", () => {
        it("sends the request when a subscription adds channels to the union", async () => {
            edge.subscribeChannelsWithState(websocket, "a", [new ChannelAddress("c0", "X")]);
            expect(await flush()).toEqual([["c0/X"]]);

            edge.subscribeChannelsWithState(websocket, "a", [
                new ChannelAddress("c0", "X"),
                new ChannelAddress("c1", "Y"),
            ]);

            expect(await flush()).toEqual([["c0/X"], ["c0/X", "c1/Y"]]);
        });
    });
});
