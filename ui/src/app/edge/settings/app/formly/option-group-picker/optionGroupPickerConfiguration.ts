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
}
