import { FormlyFieldConfig } from "@ngx-formly/core";
import { prepareProperties } from "./shared";

describe("shared", () => {
    let fields: FormlyFieldConfig[];

    beforeEach(() => {
        fields = [
            {
                key: "a",
                type: "input",
                props: {
                    type: "text",
                },
            },
            {
                key: "b",
                type: "input",
                props: {
                    type: "number",
                },
            },
            {
                key: "c",
                type: "input",
                props: {
                    type: "number",
                },
            },
            {
                key: "d",
                type: "input",
                props: {
                    type: "number",
                },
            },
        ];
    });

    it("#prepareProperties should replace all ',' with '.' in german language", () => {
        const result = prepareProperties(
            {
                ALIAS: "test",
                a: "test,test",
                b: "2,23",
                c: "12.5",
                d: 11,
            },
            fields,
        );

        expect(result).toEqual({
            a: "test,test",
            b: "2.23",
            c: "12.5",
            d: 11,
        });
    });
});
