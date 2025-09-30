import { StringUtils } from "./STRING.UTILS";

describe("StringUtils", () => {

    it("+getSubstringInBetween - valid", () => {
        const msg = "(valid)";
        expect(STRING_UTILS.GET_SUBSTRING_IN_BETWEEN("(", ")", msg)).toEqual("valid");
    });
    it("+getSubstringInBetween - valid with white space", () => {
        const msg = "( valid )";
        expect(STRING_UTILS.GET_SUBSTRING_IN_BETWEEN("(", ")", msg)).toEqual(" valid ");
    });
    it("+getSubstringInBetween - invalid", () => {
        const msg = "invalid";
        expect(STRING_UTILS.GET_SUBSTRING_IN_BETWEEN("(", ")", msg)).toEqual(null);
    });
    it("+getSubstringInBetween - invalid input string, start and end character", () => {
        expect(() => STRING_UTILS.GET_SUBSTRING_IN_BETWEEN(null, null, null)).toThrow(new Error(StringUtils.INVALID_STRING));
    });
    it("+getSubstringInBetween - valid string, invalid start and end character", () => {
        const msg = "(valid)";
        expect(() => STRING_UTILS.GET_SUBSTRING_IN_BETWEEN(null, null, msg)).toThrow(new Error(StringUtils.INVALID_STRING));;
    });
});
