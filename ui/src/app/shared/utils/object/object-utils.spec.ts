import { ObjectUtils } from "./object-utils";

describe("ObjectUtils", () => {
    const validObj = {
        _property1: "",
        _property2: "",
    };
    describe("+excludeProperties", () => {
        it("valid obj", () => {
            expect(
                ObjectUtils.excludeProperties(validObj, ["_property1"]),
            ).toEqual({ _property2: "" });
        });
        it("obj -> null", () => {
            const obj = null;
            expect(ObjectUtils.excludeProperties(obj, ["_property1"])).toEqual(
                null,
            );
        });
    });
    describe("+pickProperties", () => {
        it("valid obj", () => {
            expect(
                ObjectUtils.pickProperties(validObj, ["_property1"]),
            ).toEqual({ _property1: "" });
        });
        it("valid obj, keys are empty", () => {
            expect(ObjectUtils.pickProperties(validObj, [])).toEqual({});
        });
        it("obj -> null", () => {
            expect(ObjectUtils.pickProperties(null, ["_property1"])).toEqual(
                null,
            );
        });
    });
    describe("+parseFromString", () => {
        it("inputStr -> obj", () => {
            const obj = '{"_property1": "","_property2": ""}';
            expect(
                ObjectUtils.parseFromString<{
                    _property1: string;
                    _property2: string;
                }>(obj),
            ).toEqual({ _property1: "", _property2: "" });
        });
        it("inputStr -> non parsable object", () => {
            const obj = '{_property1: "",_property2: ';
            expect(
                ObjectUtils.parseFromString<{ _property1: string }>(obj),
            ).toEqual(null);
        });
        it("inputStr -> null", () => {
            const obj = null;
            expect(
                ObjectUtils.parseFromString<{ _property1: string }>(obj),
            ).toEqual(null);
        });
    });
});
