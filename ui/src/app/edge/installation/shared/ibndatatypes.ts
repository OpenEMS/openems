import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { Category } from "./category";

export type ComponentData = {
    label: string;
    value: any;
};

export type SerialNumberFormData = {
    formTower: FormGroup;
    fieldSettings: FormlyFieldConfig[];
    model: any;
    header: string;
};

export type TableData = {
    header: Category;
    rows: ComponentData[]
};

export type DcPv = {
    isSelected: boolean,
    alias?: string,
    value?: number,
    orientation?: string,
    moduleType?: string,
    modulesPerString?: number
};

export type dcForm = {
    formGroup: FormGroup<any>;
    fields: FormlyFieldConfig[];
    model: any;
};
