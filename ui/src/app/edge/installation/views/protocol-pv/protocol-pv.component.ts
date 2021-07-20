import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { InstallationData } from '../../installation.component';

export type DcPv = {
  isSelected: boolean,
  alias: string,
  value: number,
  orientation: string,
  moduleType: string,
  modulesPerString: number
}

@Component({
  selector: ProtocolPv.SELECTOR,
  templateUrl: './protocol-pv.component.html'
})
export class ProtocolPv implements OnInit {

  private static readonly SELECTOR = "protocol-pv";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<InstallationData>();

  public formMppt1: FormGroup;
  public fieldsMppt1: FormlyFieldConfig[];
  public modelMppt1;

  public formMppt2: FormGroup;
  public fieldsMppt2: FormlyFieldConfig[];
  public modelMppt2;

  constructor() { }

  public ngOnInit() {

    this.formMppt1 = new FormGroup({});
    this.fieldsMppt1 = this.getFields(1);
    this.modelMppt1 = this.installationData.pv.dc1 ?? {};

    this.formMppt2 = new FormGroup({});
    this.fieldsMppt2 = this.getFields(2);
    this.modelMppt2 = this.installationData.pv.dc2 ?? {};

  }

  public onPreviousClicked() {

    this.previousViewEvent.emit();

  }

  public onNextClicked() {

    if (this.formMppt1.invalid || this.formMppt2.invalid) {
      return;
    }

    this.installationData.pv.dc1 = this.modelMppt1;
    this.installationData.pv.dc2 = this.modelMppt2;

    this.nextViewEvent.emit(this.installationData);

  }

  public getFields(forMpptNr: 1 | 2): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "isSelected",
      type: "checkbox",
      templateOptions: {
        label: "PV-Anlage " + forMpptNr + " (beschriftet mit ''PV" + forMpptNr + "'')",
        required: true
      }
    });

    fields.push({
      key: "alias",
      type: "input",
      templateOptions: {
        label: "Bezeichnung",
        description: "wird im Online-Monitoring angezeigt, z. B. ''Garage''",
        required: true
      },
      hideExpression: model => !model.isSelected
    });

    fields.push({
      key: "value",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Installierte Leistung [Wₚ]",
        min: 1000,
        required: true
      },
      parsers: [Number],
      hideExpression: model => !model.isSelected
    });

    fields.push({
      key: "orientation",
      type: "select",
      templateOptions: {
        label: "Ausrichtung",
        options: [
          { label: "", value: undefined },
          { label: "Nord", value: "n" },
          { label: "Nordost", value: "no" },
          { label: "Ost", value: "o" },
          { label: "Südost", value: "so" },
          { label: "Süd", value: "s" },
          { label: "Südwest", value: "sw" },
          { label: "West", value: "w" },
          { label: "Nordwest", value: "nw" },
        ]
      },
      hideExpression: model => !model.isSelected
    });

    fields.push({
      key: "moduleType",
      type: "input",
      templateOptions: {
        label: "Modultyp",
        description: "z. B. Hersteller und Leistung"
      },
      hideExpression: model => !model.isSelected
    });

    fields.push({
      key: "modulesPerString",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Anzahl Module"
      },
      parsers: [Number],
      hideExpression: model => !model.isSelected
    });

    return fields;

  }

}