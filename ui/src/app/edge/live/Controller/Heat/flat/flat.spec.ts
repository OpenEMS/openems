import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { ControllerHeatComponent, Status, State } from "./flat";

/**
 * Minimal unit tests for {@link ControllerHeatComponent}.
 *
 * The component extends {@link AbstractFlatWidget}, which has a heavy DI graph
 * (Websocket, Service, ModalController, ...). To keep these tests minimal we
 * exercise the two pieces of pure logic directly via the prototype, supplying
 * a stub `this` context with only the fields the methods read.
 */
describe("ControllerHeatComponent", () => {

    function callGetChannelAddresses(component: EdgeConfig.Component | null): ChannelAddress[] {
        const stub = { component } as ControllerHeatComponent;
        return (ControllerHeatComponent.prototype as any).getChannelAddresses.call(stub);
    }

    function callOnCurrentData(component: EdgeConfig.Component, currentData: CurrentData): { statusNumber: number | null, state: State | null } {
        const stub: any = { component, statusNumber: null, state: null };
        (ControllerHeatComponent.prototype as any).onCurrentData.call(stub, currentData);
        return { statusNumber: stub.statusNumber, state: stub.status };
    }

    describe("#getChannelAddresses()", () => {

        it("returns an empty list when component is null", () => {
            expect(callGetChannelAddresses(null)).toEqual([]);
        });

        it("returns the base channels for non-Askoma Heat components", () => {
            const component = new EdgeConfig.Component("heat0", "Heat", true, false, "Heat.MyPv.AcThor9s", {});

            const channels = callGetChannelAddresses(component);

            expect(channels).toEqual([
                new ChannelAddress("heat0", "Status"),
                new ChannelAddress("heat0", "ControlNotAllowed"),
                new ChannelAddress("heat0", "ActivePower"),
                new ChannelAddress("heat0", "Temperature"),
            ]);
        });

        it("includes the _PropertyMode channel for Askoma components", () => {
            const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});

            const channels = callGetChannelAddresses(component);

            expect(channels).toEqual([
                new ChannelAddress("heat0", "Status"),
                new ChannelAddress("heat0", "ControlNotAllowed"),
                new ChannelAddress("heat0", "ActivePower"),
                new ChannelAddress("heat0", "Temperature"),
                new ChannelAddress("heat0", "_PropertyMode"),
            ]);
        });
    });

    describe("#onCurrentData()", () => {
        const component = new EdgeConfig.Component("heat0", "Heat", true, false, "Heat.MyPv.AcThor9s", {});

        function dataWith(values: CurrentData["allComponents"]): CurrentData {
            return { allComponents: values } as CurrentData;
        }

        it("maps standby / excess / ControlNotAllowed to heating", () => {
            for (const status of [Status.standby, Status.excess, Status.ControlNotAllowed]) {
                const result = callOnCurrentData(component, dataWith({ "heat0/Status": status }));

                expect(result.statusNumber).toBe(status);
                expect(result.state).toBe(State.heating);
            }
        });

        it("maps temperatureReached to temperatureReached", () => {
            const result = callOnCurrentData(component, dataWith({ "heat0/Status": Status.temperatureReached }));

            expect(result.state).toBe(State.temperatureReached);
        });

        it("maps noControlSignal with positive ActivePower to heating", () => {
            const result = callOnCurrentData(component, dataWith({
                "heat0/Status": Status.noControlSignal,
                "heat0/ActivePower": 500,
            }));

            expect(result.state).toBe(State.heating);
        });

        it("maps noControlSignal without ActivePower to noHeating", () => {
            const result = callOnCurrentData(component, dataWith({
                "heat0/Status": Status.noControlSignal,
                "heat0/ActivePower": 0,
            }));

            expect(result.state).toBe(State.noHeating);
        });

        it("maps error to noHeating", () => {
            const result = callOnCurrentData(component, dataWith({ "heat0/Status": Status.error }));

            expect(result.state).toBe(State.noHeating);
        });

        it("falls back to error/noHeating when Status channel is missing", () => {
            const result = callOnCurrentData(component, dataWith({}));

            expect(result.statusNumber).toBe(Status.error);
            expect(result.state).toBe(State.noHeating);
        });

        it("maps unknown status numbers to noHeating", () => {
            const result = callOnCurrentData(component, dataWith({ "heat0/Status": 99 }));

            expect(result.state).toBe(State.noHeating);
        });
    });
});
