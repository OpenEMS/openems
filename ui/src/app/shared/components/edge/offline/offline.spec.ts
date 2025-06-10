import { TestContext, TestingUtils } from "../../shared/testing/utils.spec";
import { OfflineComponent } from "./offline.component";

describe("OfflineComponent", () => {

    let TEST_CONTEXT: TestContext;
    beforeAll(async () => {
        TEST_CONTEXT = await TestingUtils.sharedSetup();
    });

    it("-formatMilliSecondsToValidRange - 1 minute", () => {
        const ms: number = 60000;
        expect(OfflineComponent.formatMilliSecondsToValidRange(ms, TEST_CONTEXT.translate)).toBe("1 Minute");
    });
    it("-formatMilliSecondsToValidRange - 2 hours", () => {
        const ms: number = 60000 * 121;
        expect(OfflineComponent.formatMilliSecondsToValidRange(ms, TEST_CONTEXT.translate)).toBe("2 Stunden");
    });
    it("-formatMilliSecondsToValidRange - 1 day", () => {
        const ms: number = 3 * 24 * 60 * 60 * 1000;
        expect(OfflineComponent.formatMilliSecondsToValidRange(ms, TEST_CONTEXT.translate)).toBe("3 Tage");
    });
});
