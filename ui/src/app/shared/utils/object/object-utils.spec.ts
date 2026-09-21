import { ObjectUtils } from "./object-utils";

describe("ObjectUtils", () => {
    const validObj = {
        _property1: "",
        _property2: "",
    };
    describe("+excludeProperties", () => {
        it("valid obj", () => {
            expect(ObjectUtils.excludeProperties(validObj, ["_property1"])).toEqual({ _property2: "" });
        });
        it("obj -> null", () => {
            const obj = null;
            expect(ObjectUtils.excludeProperties(obj, ["_property1"])).toEqual(null);
        });
    });
    describe("+pickProperties", () => {
        it("valid obj", () => {
            expect(ObjectUtils.pickProperties(validObj, ["_property1"])).toEqual({ _property1: "" });
        });
        it("valid obj, keys are empty", () => {
            expect(ObjectUtils.pickProperties(validObj, [])).toEqual({});
        });
        it("obj -> null", () => {
            expect(ObjectUtils.pickProperties(null, ["_property1"])).toEqual(null);
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
            expect(ObjectUtils.parseFromString<{ _property1: string }>(obj)).toEqual(null);
        });
        it("inputStr -> null", () => {
            const obj = null;
            expect(ObjectUtils.parseFromString<{ _property1: string }>(obj)).toEqual(null);
        });
    });
    describe("+omitNullOrUndefinedValues", () => {
        it("removes null and undefined values", () => {
            const obj = {
                a: 1,
                b: null,
                c: undefined,
                d: "value",
            };
            expect(ObjectUtils.omitNullOrUndefinedValues(obj)).toEqual({
                a: 1,
                d: "value",
            });
        });
        it("keeps falsy values that are not null/undefined", () => {
            const obj = {
                a: 0,
                b: false,
                c: "",
            };
            expect(ObjectUtils.omitNullOrUndefinedValues(obj)).toEqual({
                a: 0,
                b: false,
                c: "",
            });
        });
        it("obj without null/undefined values -> unchanged", () => {
            expect(ObjectUtils.omitNullOrUndefinedValues(validObj)).toEqual(validObj);
        });
        it("empty obj -> empty obj", () => {
            expect(ObjectUtils.omitNullOrUndefinedValues({})).toEqual({});
        });
        it("obj with only null/undefined values -> empty obj", () => {
            expect(
                ObjectUtils.omitNullOrUndefinedValues({
                    a: null,
                    b: undefined,
                }),
            ).toEqual({});
        });
        it("removes values failing additionalCheck predicate", () => {
            const obj = {
                a: 1,
                b: NaN,
                c: "value",
            };
            expect(ObjectUtils.omitNullOrUndefinedValues(obj, (val) => !Number.isNaN(val))).toEqual({
                a: 1,
                c: "value",
            });
        });
    });
});
