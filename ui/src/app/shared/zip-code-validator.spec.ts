import { COUNTRY_PARTNER_ZIP_CODE_REGEX } from "./shared.module";

describe("#COUNTRY_PARTNER_ZIP_CODE_REGEX", () => {
    const regex = COUNTRY_PARTNER_ZIP_CODE_REGEX;

    const testCases = [
        // Germany (5 digits)
        { country: "Germany", input: "10115", expected: true },
        { country: "Germany", input: "ABCDE", expected: false },
        { country: "Germany", input: "101", expected: false },

        // Austria (4 digits)
        { country: "Austria", input: "1010", expected: true },
        { country: "Austria", input: "101", expected: false },

        // Switzerland (4 digits)
        { country: "Switzerland", input: "8001", expected: true },
        { country: "Switzerland", input: "800A", expected: false },

        // Sweden (3 digits followed by a space and 2 digits)
        { country: "Sweden", input: "123 45", expected: true },
        { country: "Sweden", input: "123 4A", expected: false },

        // Czech Republic (5 digits)
        { country: "Czech Republic", input: "11000", expected: true },
        { country: "Czech Republic", input: "ABCDE", expected: false },

        // Netherlands (4 digits followed by 2 uppercase letters)
        { country: "Netherlands", input: "1234 AB", expected: true },
        { country: "Netherlands", input: "1234AB", expected: true },
        { country: "Netherlands", input: "1234 A", expected: false },

        // Lithuania (5 digits)
        { country: "Lithuania", input: "12345", expected: true },

        // Greece (5 digits)
        { country: "Greece", input: "12345", expected: true },
        { country: "Greece", input: "ABCDE", expected: false },

        // Edge cases
        { country: "Edge Case", input: "", expected: false },
        { country: "Edge Case", input: " ", expected: false },
        { country: "Edge Case", input: "123", expected: false },
        { country: "Edge Case", input: "99999-9999", expected: true }, // US-style zip+4
    ];

    testCases.forEach(({ country, input, expected }) => {
        it(`should ${expected ? "accept" : "reject"} '${input}' as a valid postal code for ${country}`, () => {
            expect(regex.test(input)).toEqual(expected);
        });
    });
});
