// @ts-strict-ignore
import { DummyConfig } from "../EDGECONFIG.SPEC";
import { EvcsUtils } from "./evcs-utils";

describe("EvcsUtils", () => {
    const config = DUMMY_CONFIG.FROM(
        DUMMY_CONFIG.COMPONENT.EVCS_HARDY_BARTH("evcs0", "Charging Station"),
        DUMMY_CONFIG.COMPONENT.EVCS_MENNEKES("evcs1", "Wallbox"),
    );

    it("#getEvcsPowerChannelId should return 'ChargePower' when edge is null", () => {
        const result = EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(
            CONFIG.GET_COMPONENT("evcs0"),
            config,
            null,
        );
        expect(result).toBe("ChargePower");
    });

    it("#getEvcsPowerChannelId should return 'ChargePower' for old edges", () => {
        const result = EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(
            CONFIG.GET_COMPONENT("evcs0"),
            config,
            DUMMY_CONFIG.DUMMY_EDGE({ version: "2024.10.1" }),
        );
        expect(result).toBe("ChargePower");
    });

    it("#getEvcsPowerChannelId should return 'ChargePower' for deprecated components", () => {
        const result = EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(
            CONFIG.GET_COMPONENT("evcs0"),
            config,
            DUMMY_CONFIG.DUMMY_EDGE({ version: "2024.10.2" }),
        );
        expect(result).toBe("ChargePower");
    });

    it("#getEvcsPowerChannelId should return 'ActivePower' for non deprecated components", () => {
        const result = EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(
            CONFIG.GET_COMPONENT("evcs1"),
            config,
            DUMMY_CONFIG.DUMMY_EDGE({ version: "2024.10.2" }),
        );
        expect(result).toBe("ActivePower");
    });

    it("#getEvcsPowerChannelId should return 'ChargePower' for null inputs", () => {
        const result = EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(null, null, DUMMY_CONFIG.DUMMY_EDGE({ version: "2024.10.2" }));
        expect(result).toBe("ChargePower");
    });
});
