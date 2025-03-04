import { StringUtils } from "./string.utils";

describe("StringUtils", () => {

    it("+getSubstringInBetween - valid", () => {
        const msg = "(valid)";
        expect(StringUtils.getSubstringInBetween("(", ")", msg)).toEqual("valid");
    });
    it("+getSubstringInBetween - valid with white space", () => {
        const msg = "( valid )";
        expect(StringUtils.getSubstringInBetween("(", ")", msg)).toEqual(" valid ");
    });
    it("+getSubstringInBetween - invalid", () => {
        const msg = "invalid";
        expect(StringUtils.getSubstringInBetween("(", ")", msg)).toEqual(null);
    });
    it("+getSubstringInBetween - invalid input string, start and end character", () => {
        expect(() => StringUtils.getSubstringInBetween(null, null, null)).toThrow(new Error(StringUtils.INVALID_STRING));
    });
    it("+getSubstringInBetween - valid string, invalid start and end character", () => {
        const msg = "(valid)";
        expect(() => StringUtils.getSubstringInBetween(null, null, msg)).toThrow(new Error(StringUtils.INVALID_STRING));;
    });
});
