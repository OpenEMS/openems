// @ts-strict-ignore
import { FormlyFieldConfig } from "@ngx-formly/core";
import { GetAppAssistant } from "./getAppAssistant";

describe("GetAppAssistant", () => {
    let fields: FormlyFieldConfig[];

    beforeEach(() => {
        fields = [
            {
                key: "a",
                type: "input",
                props: {
                    type: "text",
                },
                fieldGroup: [
                    {
                        key: "b",
                        type: "input",
                        props: {
                            type: "number",
                        },
                    },
                ],
            },
            {
                key: "c",
                type: "input",
                props: {
                    type: "number",
                },
            },
        ];
    });

    it("#findField should find a field by a path", () => {
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["a"])).toBeDefined();
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["a", "b"])).toBeDefined();
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["c"])).toBeDefined();
    });

    it("#setInitialModel should set the initial model on every field", () => {
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["a"])["initialModel"]).toBeUndefined();
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["a", "b"])["initialModel"]).toBeUndefined();
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["c"])["initialModel"]).toBeUndefined();
        GET_APP_ASSISTANT.SET_INITIAL_MODEL(fields, {});
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["a"])["initialModel"]).toBeDefined();
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["a", "b"])["initialModel"]).toBeDefined();
        expect(GET_APP_ASSISTANT.FIND_FIELD(fields, ["c"])["initialModel"]).toBeDefined();
    });

    it("#convertStringExpressions should parse number inputs to numbers", () => {
        const expression = "MODEL.A < 1 || MODEL.A.B < 1 && [1,2].every(i => i < INITIAL_MODEL.C)";
        const converted = GET_APP_ASSISTANT.CONVERT_STRING_EXPRESSIONS(fields, fields[0], expression);
        expect(converted).toBe("MODEL.A < 1 || +MODEL.A.B < 1 && [1,2].every(i => i < +INITIAL_MODEL.C)");
    });

});
