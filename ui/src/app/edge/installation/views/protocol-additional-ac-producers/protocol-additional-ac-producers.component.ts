import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Service, Utils } from 'src/app/shared/shared';
import { Ibn } from '../../installation-systems/abstract-ibn';

export type AcPv = {
  alias: string,
  value: number,
  orientation: string,
  moduleType: string,
  modulesPerString: number,
  meterType: string,
  modbusCommunicationAddress: number
}

@Component({
  selector: "protocol-additional-ac-producers",
  templateUrl: './protocol-additional-ac-producers.component.html'
})
export class ProtocolAdditionalAcProducersComponent implements OnInit {
  private readonly LINK_SOCOMEC_MANUAL = "https://www.fenecon.de/download/fems-app-socomec-zaehler/";

  @Input() public ibn: Ibn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<Ibn>();
  @Output() public setIbnEvent = new EventEmitter<Ibn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  public insertModeEnabled: boolean;

  constructor(private service: Service) { }

  public ngOnInit() {

    // Initialize PV-Object
    this.ibn.pv ??= {};
    this.ibn.pv.ac ??= [];
    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = {};
    this.insertModeEnabled = false;
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.insertModeEnabled) {
      this.service.toast("Speichern Sie zuerst Ihre Eingaben um fortzufahren.", "warning");
      return;
    }

    this.setIbnEvent.emit(this.ibn);
    this.nextViewEvent.emit();
  }

  public getFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "alias",
      type: "input",
      templateOptions: {
        label: "Bezeichnung",
        description: "z. B. ''PV Hausdach''",
        required: true
      }
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
      parsers: [Number]
    });

    fields.push({
      key: "orientation",
      type: "select",
      templateOptions: {
        label: "Ausrichtung",
        options: [
          { label: "Süd", value: "s" },
          { label: "Südwest", value: "sw" },
          { label: "West", value: "w" },
          { label: "Südost", value: "so" },
          { label: "Ost", value: "o" },
          { label: "Nordwest", value: "nw" },
          { label: "Nordost", value: "no" },
          { label: "Nord", value: "n" },
          { label: "", value: undefined }
        ]
      }
    });

    fields.push({
      key: "moduleType",
      type: "input",
      templateOptions: {
        label: "Modultyp",
        description: "z. B. Hersteller und Leistung"
      }
    });

    fields.push({
      key: "modulesPerString",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Anzahl PV-Module"
      },
      parsers: [Number]
    });

    fields.push({
      key: "meterType",
      type: "select",
      templateOptions: {
        label: "Zählertyp",
        required: true,
        options: [
          { label: "SOCOMEC", value: "socomec" }
        ]
      },
      defaultValue: "socomec"
    });

    fields.push({
      key: "modbusCommunicationAddress",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Modbus Kommunikationsadresse",
        description: "Der Zähler muss mit den folgenden Parametern konfiguriert werden: Kommunikationsgeschwindigkeit (bAud) ''9600'', Kommunikationsparität (PrtY) ''n'', Kommunikations-Stopbit (StoP) ''1''",
        required: true,
        min: 6
      },
      parsers: [Number],
      defaultValue: 6
    });

    return fields;

  }

  public switchMode() {

    if (this.insertModeEnabled) {

      // Test if form is valid
      if (this.form.invalid) {
        this.service.toast("Geben Sie gültige Daten ein um zu Speichern.", "danger");
        return;
      }

      // Push data into array and reset the form
      this.ibn.pv.ac.push(Utils.deepCopy(this.model));
      this.form.reset();

    }

    // Switch
    this.insertModeEnabled = !this.insertModeEnabled;
  }

  public editElement(element) {
    this.model = element;

    if (!this.insertModeEnabled) {
      this.switchMode();
    }

    this.removeElement(element);
  }

  public removeElement(element) {
    let ac = this.ibn.pv.ac;
    ac.splice(ac.indexOf(element), 1);
  }

  public openManual() {
    window.open(this.LINK_SOCOMEC_MANUAL);
  }
}