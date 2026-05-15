import { TranslateService } from "@ngx-translate/core";
import { TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ControllerHeatModule } from "./Heat";
import heatDe from "./i18n/de.json";
import heatEn from "./i18n/en.json";

describe("ControllerHeatModule", () => {
    let translate: TranslateService;

    beforeEach(async () => {
        const testContext = await TestingUtils.sharedSetup();
        translate = testContext.translate;
    });

    it("should define the module", () => {
        expect(ControllerHeatModule).toBeDefined();
    });

    it("should have the correct module name", () => {
        expect(ControllerHeatModule.name).toBe("ControllerHeatModule");
    });

    it("should support translation initialization", async () => {
        await expect(() => {
            translate.setTranslation("de", heatDe, true);
            translate.setTranslation("en", heatEn, true);
        }).not.toThrow();
    });
});

