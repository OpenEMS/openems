// @ts-strict-ignore
import { TestContext, TestingUtils } from "../../components/shared/testing/utils.spec";
import { TimeUtils } from "./timeutils";

describe("TimeUtils", () => {

    const locale: Intl.LocalesArgument = "de";
    let TEST_CONTEXT: TestContext;
    beforeEach(async () => {
        TEST_CONTEXT = await TestingUtils.sharedSetup();
    });
    it("#formatSecondsToDuration", () => {
        expect(TimeUtils.formatSecondsToDuration(12000, locale)).toEqual("3h 20m");
        expect(TimeUtils.formatSecondsToDuration(null, locale)).toEqual(null);
        expect(TimeUtils.formatSecondsToDuration(undefined, locale)).toEqual(null);
        expect(TimeUtils.formatSecondsToDuration(12000, null)).toEqual("3h 20m");
    });

    it("+CONVERT_MINUTE_TO_TIME_OF_DAY", () => {
        expect(TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(TEST_CONTEXT.translate, locale)(1000)).toEqual("16:40 Uhr");
        expect(TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(TEST_CONTEXT.translate, locale)(0)).toEqual("00:00 Uhr");
        expect(TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(TEST_CONTEXT.translate, locale)(null)).toEqual("-");
        expect(TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(TEST_CONTEXT.translate, locale)(undefined)).toEqual("-");
    });

    //  Note: both formats "en" & "en-US" are accepted and supported.
    it("+CONVERT_MINUTE_TO_TIME_OF_DAY (en-US locale)", () => {
        expect(TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(TEST_CONTEXT.translate, "en-US")(1000)).toEqual("04:40 PM");
        expect(TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(TEST_CONTEXT.translate, "en-US")(0)).toEqual("12:00 AM");
        expect(TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(TEST_CONTEXT.translate, "en-US")(null)).toEqual("-");
        expect(TimeUtils.CONVERT_MINUTE_TO_TIME_OF_DAY(TEST_CONTEXT.translate, "en-US")(undefined)).toEqual("-");
    });
});
