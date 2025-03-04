// @ts-strict-ignore
import { DummyConfig } from "../edgeconfig.spec";
import { EvcsUtils } from "./evcs-utils";

describe("EvcsUtils", () => {
    const config = DummyConfig.from(
        DummyConfig.Component.EVCS_HARDY_BARTH("evcs0", "Charging Station"),
        DummyConfig.Component.EVCS_MENNEKES("evcs1", "Wallbox"),
    );

    it("#getEvcsPowerChannelId should return 'ChargePower' when edge is null", () => {
        const result = EvcsUtils.getEvcsPowerChannelId(
            config.getComponent("evcs0"),
            config,
            null,
        );
        expect(result).toBe("ChargePower");
    });

    it("#getEvcsPowerChannelId should return 'ChargePower' for old edges", () => {
        const result = EvcsUtils.getEvcsPowerChannelId(
            config.getComponent("evcs0"),
            config,
            DummyConfig.dummyEdge({ version: "2024.10.1" }),
        );
        expect(result).toBe("ChargePower");
    });

    it("#getEvcsPowerChannelId should return 'ChargePower' for deprecated components", () => {
        const result = EvcsUtils.getEvcsPowerChannelId(
            config.getComponent("evcs0"),
            config,
            DummyConfig.dummyEdge({ version: "2024.10.2" }),
        );
        expect(result).toBe("ChargePower");
    });

    it("#getEvcsPowerChannelId should return 'ActivePower' for non deprecated components", () => {
        const result = EvcsUtils.getEvcsPowerChannelId(
            config.getComponent("evcs1"),
            config,
            DummyConfig.dummyEdge({ version: "2024.10.2" }),
        );
        expect(result).toBe("ActivePower");
    });

    it("#getEvcsPowerChannelId should return 'ChargePower' for null inputs", () => {
        const result = EvcsUtils.getEvcsPowerChannelId(null, null, DummyConfig.dummyEdge({ version: "2024.10.2" }));
        expect(result).toBe("ChargePower");
    });
});
