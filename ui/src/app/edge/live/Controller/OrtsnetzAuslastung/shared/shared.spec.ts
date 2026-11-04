import { TranslateService } from "@ngx-translate/core";
import { SharedControllerOrtsnetzAuslastung as Shared } from "./shared";

describe("SharedControllerOrtsnetzAuslastung", () => {
    const I18N = "EDGE.INDEX.WIDGETS.ORTSNETZ_AUSLASTUNG.";
    const translate = { instant: (key: string) => key } as TranslateService;

    it("CONVERT_GRID_STATUS", () => {
        const convert = Shared.CONVERT_GRID_STATUS(translate);
        expect(convert(0)).toEqual(I18N + "GREEN");
        expect(convert(1)).toEqual(I18N + "YELLOW");
        expect(convert(2)).toEqual(I18N + "RED");
        expect(convert(-1)).toEqual(I18N + "NO_DATA");
        expect(convert(null)).toEqual(I18N + "NO_DATA");
    });

    it("CONVERT_STORAGE_RECOMMENDATION", () => {
        const convert = Shared.CONVERT_STORAGE_RECOMMENDATION(translate);
        expect(convert(0)).toEqual(I18N + "RECOMMENDATION_NONE");
        expect(convert(1)).toEqual(I18N + "RECOMMENDATION_CHARGE");
        expect(convert(2)).toEqual(I18N + "RECOMMENDATION_DISCHARGE");
        expect(convert(-1)).toEqual(I18N + "NO_DATA");
    });

    it("CONVERT_YES_NO", () => {
        const convert = Shared.CONVERT_YES_NO(translate);
        expect(convert(1)).toEqual("GENERAL.YES");
        expect(convert(0)).toEqual("GENERAL.NO");
        expect(convert(null)).toEqual(I18N + "NO_DATA");
    });

    it("getStatusColor", () => {
        expect(Shared.getStatusColor(0)).toEqual("success");
        expect(Shared.getStatusColor(1)).toEqual("warning");
        expect(Shared.getStatusColor(2)).toEqual("danger");
        expect(Shared.getStatusColor(null)).toEqual("normal");
    });

    it("getProblemKeys", () => {
        expect(Shared.getProblemKeys("ctrl0", { "ctrl0/UnableToSend": 0, "ctrl0/NoVoltage": null })).toEqual([]);
        expect(Shared.getProblemKeys("ctrl0", { "ctrl0/UnableToSend": 1, "ctrl0/NoVoltage": 1 })).toEqual([
            I18N + "NO_VOLTAGE",
            I18N + "UNABLE_TO_SEND",
        ]);
    });
});
