import { TestBed } from "@angular/core/testing";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import heatEn from "../i18n/en.json";
import { Mode } from "../settings/settings";
import { HeatManualPayload } from "./js-calendar-utils";

describe("HeatManualPayload", () => {
    let translate: TranslateService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                TranslateModule.forRoot(),
            ],
        }).compileComponents();

        translate = TestBed.inject(TranslateService);
        translate.setTranslation("en", heatEn, true);
        translate.use("en");
    });

    it("serializes the OpenEMS payload", () => {
        const payload = TestBed.runInInjectionContext(() => new HeatManualPayload());
        payload.setValue({ mode: Mode.SURPLUS });

        expect(payload.toOpenEMSPayload()).toEqual({
            "openems.io:payload": { mode: Mode.SURPLUS },
        });
    });

    it("restores the mode from an incoming task", () => {
        const payload = TestBed.runInInjectionContext(() => new HeatManualPayload());

        payload.update(payload, {
            "@type": "Task",
            "start": "2026-04-22T08:00:00",
            "recurrenceRules": [],
            "openems.io:payload": {
                mode: Mode.FAST_HEAT,
            },
        });

        expect(payload["value"]).toEqual({ mode: Mode.FAST_HEAT });
    });

    it("formats the payload text with localized mode labels", () => {
        const payload = TestBed.runInInjectionContext(() => new HeatManualPayload());

        const parser = payload.toPayloadText(translate);
        const text = parser({
            "@type": "Task",
            "start": "2026-04-22T08:00:00",
            "recurrenceRules": [],
            "openems.io:payload": {
                mode: Mode.OFF,
            },
        });

        expect(text).toBe("No heating");
    });
});
