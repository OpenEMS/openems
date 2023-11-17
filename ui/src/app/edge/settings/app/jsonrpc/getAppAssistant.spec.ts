import { GetAppAssistant } from "./getAppAssistant";
import { FormlyFieldConfig } from "@ngx-formly/core";

describe('GetAppAssistent', () => {
    let fields: FormlyFieldConfig[];

    beforeEach(() => {
        fields = [
            {
                key: 'a',
                type: 'input',
                props: {
                    type: 'text'
                },
                fieldGroup: [
                    {
                        key: 'b',
                        type: 'input',
                        props: {
                            type: 'number'
                        }
                    }
                ]
            },
            {
                key: 'c',
                type: 'input',
                props: {
                    type: 'number'
                }
            }
        ];
    });

    it('#findField should find a field by a path', () => {
        expect(GetAppAssistant.findField(fields, ['a'])).toBeDefined();
        expect(GetAppAssistant.findField(fields, ['a', 'b'])).toBeDefined();
        expect(GetAppAssistant.findField(fields, ['c'])).toBeDefined();
    });

    it('#setInitialModel should set the initial model on every field', () => {
        expect(GetAppAssistant.findField(fields, ['a'])['initialModel']).toBeUndefined();
        expect(GetAppAssistant.findField(fields, ['a', 'b'])['initialModel']).toBeUndefined();
        expect(GetAppAssistant.findField(fields, ['c'])['initialModel']).toBeUndefined();
        GetAppAssistant.setInitialModel(fields, {});
        expect(GetAppAssistant.findField(fields, ['a'])['initialModel']).toBeDefined();
        expect(GetAppAssistant.findField(fields, ['a', 'b'])['initialModel']).toBeDefined();
        expect(GetAppAssistant.findField(fields, ['c'])['initialModel']).toBeDefined();
    });

    it('#convertStringExpressions should parse number inputs to numbers', () => {
        const expression = 'model.a < 1 || model.a.b < 1 && [1,2].every(i => i < initialModel.c)';
        const converted = GetAppAssistant.convertStringExpressions(fields, fields[0], expression);
        expect(converted).toBe('model.a < 1 || +model.a.b < 1 && [1,2].every(i => i < +initialModel.c)');
    });

});
