import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { BaseMode, CONVERT_TO_BASE_MODE_LABEL, SharedControllerIoHeatpump } from "./shared";

describe("SharedControllerIoHeatpump", () => {
    let TEST_CONTEXT: TestContext;

    beforeEach(async () => {
        TEST_CONTEXT = await TestingUtils.sharedSetup();
    });

    describe("getFormGroup", () => {
        it("should create a form group with all required controls", () => {
            const formGroup = SharedControllerIoHeatpump.getFormGroup();

            expect(formGroup.get("mode")).toBeDefined();
            expect(formGroup.get("manualState")).toBeDefined();
            expect(formGroup.get("baseMode")).toBeDefined();
            expect(formGroup.get("automaticRecommendationCtrlEnabled")).toBeDefined();
            expect(formGroup.get("automaticForceOnSurplusPower")).toBeDefined();
            expect(formGroup.get("minimumSwitchingTime")).toBeDefined();
        });

        it("should initialize all controls with null values", () => {
            const formGroup = SharedControllerIoHeatpump.getFormGroup();
            expect(formGroup.get("mode")?.value).toBeNull();
            expect(formGroup.get("baseMode")?.value).toBeNull();
        });
    });

    describe("getConsumptionMeter", () => {
        it("should return consumption meter when available", () => {
            const mockConfig = {
                getComponentFromOtherComponentsProperty: jasmine
                    .createSpy("getComponentFromOtherComponentsProperty")
                    .and.returnValue({ id: "meter0", alias: "Meter" }),
            } as any;

            const result = SharedControllerIoHeatpump.getConsumptionMeter(mockConfig, { id: "heatpump0" } as any);

            expect(result?.id).toBe("meter0");
        });

        it("should return null when consumption meter is not available", () => {
            const mockConfig = {
                getComponentFromOtherComponentsProperty: jasmine.createSpy().and.returnValue(null),
            } as any;

            const result = SharedControllerIoHeatpump.getConsumptionMeter(mockConfig, { id: "heatpump0" } as any);

            expect(result).toBeNull();
        });
    });

    describe("CONVERT_TO_BASE_MODE_LABEL", () => {
        it("should convert BaseMode.AUTOMATIC correctly", () => {
            spyOn(TEST_CONTEXT.translate, "instant").and.returnValue("Automatic");
            const converter = CONVERT_TO_BASE_MODE_LABEL(TEST_CONTEXT.translate);

            expect(converter(BaseMode.AUTOMATIC)).toBe("Automatic");
        });

        it("should return hidden value for null input", () => {
            spyOn(TEST_CONTEXT.translate, "instant").and.returnValue("");
            const converter = CONVERT_TO_BASE_MODE_LABEL(TEST_CONTEXT.translate);

            expect(converter(null)).toBe("");
        });
    });
});
