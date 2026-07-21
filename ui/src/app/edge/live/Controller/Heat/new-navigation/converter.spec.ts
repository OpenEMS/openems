import globalDe from "src/assets/i18n/de.json";
import globalEn from "src/assets/i18n/en.json";
import { TestContext, TestingUtils } from "../../../../../shared/components/shared/testing/utils.spec";
import { HeatStatus } from "../shared/shared";
import { HeatConverter } from "./converter";

describe("HeatConverter.CONVERT_POWER_2_HEAT_STATE", () => {
    let testContext: TestContext;

    beforeEach(async () => {
        testContext = await TestingUtils.sharedSetup();
        testContext.translate.setTranslation("en", globalEn, true);
        testContext.translate.use("en");
    });

    it("maps CONTROL_NOT_ALLOWED to 'Control not allowed'", () => {
        const result = HeatConverter.CONVERT_POWER_2_HEAT_STATE(testContext.translate)(HeatStatus.CONTROL_NOT_ALLOWED);

        expect(result).toBe("Control not allowed");
    });

    it("maps TEMPERATURE_REACHED to 'Target temperature reached'", () => {
        const result = HeatConverter.CONVERT_POWER_2_HEAT_STATE(testContext.translate)(HeatStatus.TEMPERATURE_REACHED);

        expect(result).toBe("Target temperature reached");
    });

    it("maps NO_CONTROL_SIGNAL to 'No heating'", () => {
        const result = HeatConverter.CONVERT_POWER_2_HEAT_STATE(testContext.translate)(HeatStatus.NO_CONTROL_SIGNAL);

        expect(result).toBe("No heating");
    });

    it("maps ERROR to 'Error'", () => {
        const result = HeatConverter.CONVERT_POWER_2_HEAT_STATE(testContext.translate)(HeatStatus.ERROR);

        expect(result).toBe("Error");
    });
});

describe("HeatConverter.CONVERT_POWER_2_HEAT_STATE (de)", () => {
    let testContext: TestContext;

    beforeEach(async () => {
        testContext = await TestingUtils.sharedSetup();
        testContext.translate.setTranslation("de", globalDe, true);
        testContext.translate.use("de");
    });

    it("maps CONTROL_NOT_ALLOWED to 'Steuerung nicht erlaubt'", () => {
        const result = HeatConverter.CONVERT_POWER_2_HEAT_STATE(testContext.translate)(HeatStatus.CONTROL_NOT_ALLOWED);

        expect(result).toBe("Steuerung nicht erlaubt");
    });

    it("maps TEMPERATURE_REACHED to 'Zieltemperatur erreicht'", () => {
        const result = HeatConverter.CONVERT_POWER_2_HEAT_STATE(testContext.translate)(HeatStatus.TEMPERATURE_REACHED);

        expect(result).toBe("Zieltemperatur erreicht");
    });

    it("maps NO_CONTROL_SIGNAL to 'Heizt nicht'", () => {
        const result = HeatConverter.CONVERT_POWER_2_HEAT_STATE(testContext.translate)(HeatStatus.NO_CONTROL_SIGNAL);

        expect(result).toBe("Heizt nicht");
    });

    it("maps ERROR to 'Fehler'", () => {
        const result = HeatConverter.CONVERT_POWER_2_HEAT_STATE(testContext.translate)(HeatStatus.ERROR);

        expect(result).toBe("Fehler");
    });
});
