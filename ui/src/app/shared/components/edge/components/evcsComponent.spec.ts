// @ts-strict-ignore
import { DummyConfig } from "../edgeconfig.spec";
import { EvcsComponent } from "./evcsComponent";

describe("EvcsComponent", () => {
    const config = DummyConfig.from(
        DummyConfig.Component.EVCS_HARDY_BARTH("evcs0", "Charging Station"),
        DummyConfig.Component.EVCS_MENNEKES("evcs1", "Wallbox"),
    );

    it("power channel should be 'ChargePower' when evcs is deprecated", () => {
        const result = EvcsComponent.from(
            config.getComponent("evcs0"),
            config,
            DummyConfig.dummyEdge({ version: "2024.10.2" }),
        );
        expect(result.powerChannel.channelId).toBe("ChargePower");
        expect(result.energyChannel.channelId).toBe("ActiveConsumptionEnergy");
    });

    it("power channel should be 'ChargePower' when edge is null", () => {
        const result = EvcsComponent.from(
            config.getComponent("evcs1"),
            config,
            null,
        );
        expect(result.powerChannel.channelId).toBe("ChargePower");
        expect(result.energyChannel.channelId).toBe("ActiveConsumptionEnergy");
    });


    it("power channel should be 'ActivePower' for mennekes", () => {
        const result = EvcsComponent.from(
            config.getComponent("evcs1"),
            config,
            DummyConfig.dummyEdge({ version: "2024.10.2" }),
        );
        expect(result.powerChannel.channelId).toBe("ActivePower");
        expect(result.energyChannel.channelId).toBe("ActiveProductionEnergy");
    });

});
