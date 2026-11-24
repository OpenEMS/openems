import { FormlyFieldConfig } from "@ngx-formly/core";

export function prepareProperties(
    formValue: Record<string, unknown>,
    fields: FormlyFieldConfig[],
): Record<string, unknown> {
    const properties: Record<string, unknown> = {};

    for (const item in formValue) {
        if (item === "ALIAS") {
            continue;
        }

        const field = fields?.find((field) => field.key === item) ?? null;
        const value = formValue[item];
        const type = field?.props?.type ?? field?.templateOptions?.type ?? null;

        if (typeof value !== "string" || type !== "number") {
            properties[item] = value;
            continue;
        }

        properties[item] = value.replace(",", ".");
    }

    return properties;
}
