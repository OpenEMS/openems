import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { Category } from "./category";
import { Meter } from "./meter";

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

export type AcPv = {
    alias: string,
    value: number,
    orientation: string,
    moduleType: string,
    modulesPerString: number,
    meterType: Meter,
    modbusCommunicationAddress: number
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
