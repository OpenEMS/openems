import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Service } from 'src/app/shared/shared';
import { COUNTRY_OPTIONS, InstallationData } from '../../installation.component';
import { FeedInSetting } from '../protocol-dynamic-feed-in-limitation/protocol-dynamic-feed-in-limitation.component';

@Component({
  selector: ConfigurationSummaryComponent.SELECTOR,
  templateUrl: './configuration-summary.component.html'
})
export class ConfigurationSummaryComponent implements OnInit {

  private static readonly SELECTOR = "configuration-summary";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<any>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  public tableData: { header: string, rows: { label: string, value: any }[] }[] = [];

  constructor(private service: Service) { }

  public ngOnInit(): void {

    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = {};

    this.generateTableData();

  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    if (!this.model.isAgbAccepted || !this.model.isGuaranteeConditionsAccepted) {
      this.service.toast("Akzeptieren Sie die AGB und Garantiebedingungen um fortzufahren.", "warning");
      return;
    }

    if (!this.model.isDevicesActiveChecked) {
      this.service.toast("Prüfen Sie, ob Batterie und Wechselrichter eingeschaltet sind und bestätigen Sie dies.", "warning");
      return;
    }

    this.nextViewEvent.emit();
  }

  public getFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "isAgbAccepted",
      type: "checkbox",
      templateOptions: {
        label: "AGB akzeptieren",
        required: true
      }
    });

    fields.push({
      key: "isGuaranteeConditionsAccepted",
      type: "checkbox",
      templateOptions: {
        label: "Garantiebedingungen akzeptieren",
        required: true
      }
    });

    fields.push({
      key: "isDevicesActiveChecked",
      type: "checkbox",
      templateOptions: {
        label: "Batterie und Wechselrichter eingeschaltet",
        required: true
      }
    });

    return fields;

  }

  public generateTableData() {

    //#region General

    let lineSideMeterFuse = this.installationData.misc.lineSideMeterFuse;
    let generalData: { label: string, value: any }[] = [
      { label: "Zeitpunkt der Installation", value: (new Date()).toLocaleString() },
      { label: "FEMS-Nummer", value: this.installationData.edge.id }
    ];

    if (lineSideMeterFuse.fixedValue === -1) {
      generalData.push({ label: "Zählervorsicherung", value: lineSideMeterFuse.otherValue })
    } else {
      generalData.push({ label: "Zählervorsicherung", value: lineSideMeterFuse.fixedValue })
    }

    this.tableData.push({
      header: "Allgemein",
      rows: generalData
    });

    //#endregion

    //#region Installer

    let installer = this.installationData.installer;

    this.tableData.push({
      header: "Installateur",
      rows: [
        { label: "Firma", value: installer.companyName },
        { label: "Nachname", value: installer.lastName },
        { label: "Vorname", value: installer.firstName },
        { label: "Straße", value: installer.street },
        { label: "PLZ", value: installer.zip },
        { label: "Ort", value: installer.city },
        { label: "Land", value: this.getCountryLabel(installer.country) },
        { label: "E-Mail", value: installer.email },
        { label: "Telefonnummer", value: installer.phone }
      ]
    });

    //#endregion

    //#region Customer

    let customer = this.installationData.customer;
    let customerData: { label: string, value: any }[] = customer.isCorporateClient ? [{ label: "Firma", value: customer.companyName }] : [];

    this.tableData.push({
      header: "Kunde",
      rows: customerData.concat([
        { label: "Nachname", value: customer.lastName },
        { label: "Vorname", value: customer.firstName },
        { label: "Straße", value: customer.street },
        { label: "PLZ", value: customer.zip },
        { label: "Ort", value: customer.city },
        { label: "Land", value: this.getCountryLabel(customer.country) },
        { label: "E-Mail", value: customer.email },
        { label: "Telefonnummer", value: customer.phone }
      ])
    });

    //#endregion

    //#region Location

    let location = this.installationData.customer;
    let locationData: { label: string, value: any }[] = location.isCorporateClient ? [{ label: "Firma", value: location.companyName }] : [];

    this.tableData.push({
      header: "Standort",
      rows: locationData.concat([
        { label: "Nachname", value: location.lastName },
        { label: "Vorname", value: location.firstName },
        { label: "Straße", value: location.street },
        { label: "PLZ", value: location.zip },
        { label: "Ort", value: location.city },
        { label: "Land", value: this.getCountryLabel(location.country) },
        { label: "E-Mail", value: location.email },
        { label: "Telefonnummer", value: location.phone }
      ])
    });

    //#endregion

    //#region Battery

    let battery = this.installationData.battery;
    let batteryData: { label: string, value: any }[] = [
      { label: "Typ", value: battery.type },
      { label: "Notstromfunktion aktiviert?", value: battery.emergencyReserve.isEnabled ? "ja" : "nein" }
    ]

    if (battery.emergencyReserve.isEnabled) {
      batteryData.push({ label: "Notstromfunktion Wert", value: battery.emergencyReserve.value });
    }

    this.tableData.push({
      header: "Batterie",
      rows: batteryData
    });

    //#endregion

    //#region Battery-Inverter

    let batteryInverter = this.installationData.batteryInverter;
    let batteryInverterData: { label: string, value: any }[] = [
      { label: "Maximale Einspeiseleistung", value: batteryInverter.dynamicFeedInLimitation.maximumFeedInPower }
    ]

    let feedInSetting = batteryInverter.dynamicFeedInLimitation.feedInSetting;

    batteryInverterData.push({ label: "Typ", value: feedInSetting });

    if (feedInSetting === FeedInSetting.FixedPowerFactor) {
      batteryInverterData.push({ label: "Cos ɸ Festwert ", value: batteryInverter.dynamicFeedInLimitation.fixedPowerFactor });
    }

    this.tableData.push({
      header: "Wechselrichter",
      rows: batteryInverterData
    });

    //#endregion

  }

  public getCountryLabel(countryValue: string) {
    return COUNTRY_OPTIONS.find((country) => { return country.value === countryValue }).label;
  }

}
