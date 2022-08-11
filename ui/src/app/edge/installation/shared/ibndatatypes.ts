import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";

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
    header: 'Allgemein' | 'Installateur' | 'Kunde'
    | 'Standort' | 'Batterie' | 'Wechselrichter'
    | 'Erzeuger' | 'Lastspitzenkappung' | 'Apps';
    rows: ComponentData[]
};
