import { FormlyFieldConfig } from "@ngx-formly/core";

export type OptionConfig = {
    value: string,
    title?: string,
    hide?: boolean,
    disabled?: boolean,
    expressions?: {
        hide?: (field: FormlyFieldConfig) => boolean,
        title?: (field: FormlyFieldConfig) => string,
        disabled?: (field: FormlyFieldConfig) => boolean,
    }
}

export type OptionGroupConfig = {
    group: string,
    title: string,
    options: OptionConfig[]
}

export type OptionGroup = {
    group: string,
    title: string,
    options: Option[]
}
export type Option = {
    value: string,
    title: string,
    disabled: boolean,
    selected: boolean,
}

/**
 * Gets the title of an OptionConfig that should be display.
 *
 * @param option the config to get the title from
 * @param field
 * @returns the title of the option
 */
export function getTitleFromOptionConfig(option: OptionConfig, field: FormlyFieldConfig): string {
    return option.expressions?.title?.(field) ?? option.title ?? option.value;
}
