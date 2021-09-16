import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Service } from 'src/app/shared/shared';
import { COUNTRY_OPTIONS, InstallationData } from '../../installation.component';
import { FeedInSetting } from '../protocol-dynamic-feed-in-limitation/protocol-dynamic-feed-in-limitation.component';
import { EmsApp, EmsAppId } from '../heckert-app-installer/heckert-app-installer.component';
import { environment } from 'src/environments';

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

    let generalData: { label: string, value: any }[] = [
      { label: "Zeitpunkt der Installation", value: (new Date()).toLocaleString() },
      { label: "FEMS-Nummer", value: this.installationData.edge.id }
    ];

    let lineSideMeterFuse = this.installationData.lineSideMeterFuse;

    if (lineSideMeterFuse.fixedValue === -1) {
      generalData.push({ label: "Vorsicherung Hausanschlusszähler", value: lineSideMeterFuse.otherValue })
    } else {
      generalData.push({ label: "Vorsicherung Hausanschlusszähler", value: lineSideMeterFuse.fixedValue })
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

    let location = this.installationData.location;
    let locationData: { label: string, value: any }[] = location.isCorporateClient ? [{ label: "Firma", value: location.companyName }] : [];

    if (!location.isEqualToCustomerData) {
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
    }

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

    let dynamicFeedInLimitation = this.installationData.dynamicFeedInLimitation;
    let batteryInverterData: { label: string, value: any }[] = [
      { label: "Maximale Einspeiseleistung", value: dynamicFeedInLimitation.maximumFeedInPower }
    ]

    batteryInverterData.push({ label: "Typ", value: dynamicFeedInLimitation.feedInSetting });

    if (dynamicFeedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor) {
      batteryInverterData.push({ label: "Cos ɸ Festwert ", value: dynamicFeedInLimitation.fixedPowerFactor });
    }

    let safetyCountry;

    if (location.isEqualToCustomerData) {
      safetyCountry = customer.country;
    } else {
      safetyCountry = location.country;
    }

    batteryInverterData.push({ label: "Ländereinstellung", value: this.getCountryLabel(safetyCountry) })

    this.tableData.push({
      header: "Wechselrichter",
      rows: batteryInverterData
    });

    //#endregion

    //#region Producers

    let pv = this.installationData.pv;
    let pvData: { label: string, value: any }[] = [];

    // DC
    let dcNr = 1;
    for (let dc of [pv.dc1, pv.dc2]) {
      if (dc.isSelected) {
        pvData = pvData.concat([
          { label: "Alias MPPT" + dcNr, value: dc.alias },
          { label: "Wert MPPT" + dcNr, value: dc.value }
        ]);
        if (dc.orientation) pvData.push({ label: "Ausrichtung MPPT" + dcNr, value: dc.orientation });
        if (dc.moduleType) pvData.push({ label: "Modultyp MPPT" + dcNr, value: dc.moduleType });
        if (dc.modulesPerString) pvData.push({ label: "Anzahl PV-Module MPPT" + dcNr, value: dc.modulesPerString });

        dcNr++;
      }
    }

    // AC
    let acNr = 1;
    for (let ac of pv.ac) {
      pvData = pvData.concat([
        { label: "Alias AC" + acNr, value: ac.alias },
        { label: "Wert AC" + acNr, value: ac.value }
      ]);
      if (ac.orientation) pvData.push({ label: "Ausrichtung AC" + acNr, value: ac.orientation });
      if (ac.moduleType) pvData.push({ label: "Modultyp AC" + acNr, value: ac.moduleType });
      if (ac.modulesPerString) pvData.push({ label: "Anzahl PV-Module AC" + acNr, value: ac.modulesPerString });
      pvData = pvData.concat([
        { label: "Zählertyp AC" + acNr, value: ac.meterType },
        { label: "Modbus Kommunikationsadresse AC" + acNr, value: ac.modbusCommunicationAddress }
      ]);
      acNr++;
    }

    if (pvData.length > 0) {
      this.tableData.push({
        header: "Erzeuger",
        rows: pvData
      });
    }

    //#endregion

    //#region [Heckert] Free App

    if (environment.theme === "Heckert") {
      let selectedFreeApp: EmsApp = this.installationData.selectedFreeApp;

      if (selectedFreeApp.id !== EmsAppId.None) {
        this.tableData.push({
          header: "Apps",
          rows: [{ label: "Ihre gewählte kostenlose App", value: selectedFreeApp.alias }]
        });
      }
    }

    //#endregion
  }

  public getCountryLabel(countryValue: string) {
    return COUNTRY_OPTIONS.find((country) => { return country.value === countryValue }).label;
  }
}
