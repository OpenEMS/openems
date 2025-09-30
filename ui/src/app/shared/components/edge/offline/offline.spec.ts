import { TestContext, TestingUtils } from "../../shared/testing/UTILS.SPEC";
import { OfflineComponent } from "./OFFLINE.COMPONENT";

describe("OfflineComponent", () => {

    let TEST_CONTEXT: TestContext;
    beforeAll(async () => {
        TEST_CONTEXT = await TESTING_UTILS.SHARED_SETUP();
    });

    it("-formatMilliSecondsToValidRange - 1 minute", () => {
        const ms: number = 60000;
        expect(OFFLINE_COMPONENT.FORMAT_MILLI_SECONDS_TO_VALID_RANGE(ms, TEST_CONTEXT.translate)).toBe("1 Minute");
    });
    it("-formatMilliSecondsToValidRange - 2 hours", () => {
        const ms: number = 60000 * 121;
        expect(OFFLINE_COMPONENT.FORMAT_MILLI_SECONDS_TO_VALID_RANGE(ms, TEST_CONTEXT.translate)).toBe("2 Stunden");
    });
    it("-formatMilliSecondsToValidRange - 1 day", () => {
        const ms: number = 3 * 24 * 60 * 60 * 1000;
        expect(OFFLINE_COMPONENT.FORMAT_MILLI_SECONDS_TO_VALID_RANGE(ms, TEST_CONTEXT.translate)).toBe("3 Tage");
    });
});
