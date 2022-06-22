import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Edge, Service } from 'src/app/shared/shared';
import { environment } from 'src/environments';

import { ComponentData } from 'src/app/shared/type/componentData';
import { Ibn } from '../../installation-systems/abstract-ibn';
import { COUNTRY_OPTIONS } from '../../installation.component';
import { EmsApp, EmsAppId } from '../heckert-app-installer/heckert-app-installer.component';

@Component({
  selector: ConfigurationSummaryComponent.SELECTOR,
  templateUrl: './configuration-summary.component.html'
})
export class ConfigurationSummaryComponent implements OnInit {

  private static readonly SELECTOR = 'configuration-summary';

  @Input() public ibn: Ibn;
  @Input() public edge: Edge;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<any>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  public tableData: { header: string; rows: ComponentData[] }[] = [];

  constructor(private service: Service) { }

  public ngOnInit(): void {
    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = {};

    this.tableData = this.generateTableData();
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (!this.model.isAgbAccepted || !this.model.isGuaranteeConditionsAccepted) {
      this.service.toast('Akzeptieren Sie die AGB und Garantiebedingungen um fortzufahren.', 'warning');
      return;
    }
    if (!this.model.isDevicesActiveChecked) {
      this.service.toast('Prüfen Sie, ob Batterie und Wechselrichter eingeschaltet sind und bestätigen Sie dies.', 'warning');
      return;
    }
    this.nextViewEvent.emit();
  }

  public getFields(): FormlyFieldConfig[] {

    const fields: FormlyFieldConfig[] = [];
    fields.push({
      key: 'isAgbAccepted',
      type: 'checkbox',
      templateOptions: {
        label: 'AGB akzeptieren',
        required: true
      }
    });

    fields.push({
      key: 'isGuaranteeConditionsAccepted',
      type: 'checkbox',
      templateOptions: {
        label: 'Garantiebedingungen akzeptieren',
        required: true
      }
    });

    fields.push({
      key: 'isDevicesActiveChecked',
      type: 'checkbox',
      templateOptions: {
        label: 'Batterie und Wechselrichter eingeschaltet',
        required: true
      }
    });
    return fields;
  }

  /**
   * Collect all the data for summary.
   */
  public generateTableData() {
    const tableData: { header: string; rows: ComponentData[] }[] = [];
    const edgeData = this.edge.id;
    const generalData: ComponentData[] = [
      { label: 'Zeitpunkt der Installation', value: (new Date()).toLocaleString() },
      { label: 'FEMS-Nummer', value: edgeData }
    ];

    const lineSideMeterFuse = this.ibn.lineSideMeterFuse;
    if (lineSideMeterFuse.fixedValue === -1) {
      generalData.push({ label: 'Vorsicherung Hausanschlusszähler', value: lineSideMeterFuse.otherValue });
    } else {
      generalData.push({ label: 'Vorsicherung Hausanschlusszähler', value: lineSideMeterFuse.fixedValue });
    }

    tableData.push({
      header: 'Allgemein',
      rows: generalData
    });

    const installer = this.ibn.installer;
    tableData.push({
      header: 'Installateur',
      rows: [
        { label: 'Firma', value: installer.companyName },
        { label: 'Nachname', value: installer.lastName },
        { label: 'Vorname', value: installer.firstName },
        { label: 'Straße', value: installer.street },
        { label: 'PLZ', value: installer.zip },
        { label: 'Ort', value: installer.city },
        { label: 'Land', value: this.getCountryLabel(installer.country) },
        { label: 'E-Mail', value: installer.email },
        { label: 'Telefonnummer', value: installer.phone }
      ]
    });

    const customer = this.ibn.customer;
    const customerData: ComponentData[] = customer.isCorporateClient ? [{ label: 'Firma', value: customer.companyName }] : [];
    tableData.push({
      header: 'Kunde',
      rows: customerData.concat([
        { label: 'Nachname', value: customer.lastName },
        { label: 'Vorname', value: customer.firstName },
        { label: 'Straße', value: customer.street },
        { label: 'PLZ', value: customer.zip },
        { label: 'Ort', value: customer.city },
        { label: 'Land', value: this.getCountryLabel(customer.country) },
        { label: 'E-Mail', value: customer.email },
        { label: 'Telefonnummer', value: customer.phone }
      ])
    });

    const location = this.ibn.location;
    const locationData: ComponentData[] = location.isCorporateClient ? [{ label: 'Firma', value: location.companyName }] : [];
    if (!location.isEqualToCustomerData) {
      tableData.push({
        header: 'Standort',
        rows: locationData.concat([
          { label: 'Nachname', value: location.lastName },
          { label: 'Vorname', value: location.firstName },
          { label: 'Straße', value: location.street },
          { label: 'PLZ', value: location.zip },
          { label: 'Ort', value: location.city },
          { label: 'Land', value: this.getCountryLabel(location.country) },
          { label: 'E-Mail', value: location.email },
          { label: 'Telefonnummer', value: location.phone }
        ])
      });
    }

    const batteryData: ComponentData[] = [];
    batteryData.push(
      { label: 'Typ', value: this.ibn.type },
    );

    tableData.push({
      header: 'Batterie',
      rows: this.ibn.addCustomBatteryData(batteryData)
    });

    let batteryInverterData: ComponentData[] = [];
    let safetyCountry;
    if (location.isEqualToCustomerData) {
      safetyCountry = customer.country;
    } else {
      safetyCountry = location.country;
    }

    batteryInverterData = this.ibn.addCustomBatteryInverterData(batteryInverterData);
    batteryInverterData.push({ label: 'Ländereinstellung', value: this.getCountryLabel(safetyCountry) });
    tableData.push({
      header: 'Wechselrichter',
      rows: batteryInverterData
    });

    const pv = this.ibn.pv ?? {};
    let pvData: ComponentData[] = [];

    // DC
    pvData = this.ibn.addCustomPvData(pvData);

    // AC
    if (pv.ac) {
      let acNr = 1;
      for (const ac of pv.ac) {
        pvData = pvData.concat([
          { label: 'Alias AC' + acNr, value: ac.alias },
          { label: 'Wert AC' + acNr, value: ac.value }
        ]);

        if (ac.orientation) { pvData.push({ label: 'Ausrichtung AC' + acNr, value: ac.orientation }); }
        if (ac.moduleType) { pvData.push({ label: 'Modultyp AC' + acNr, value: ac.moduleType }); }
        if (ac.modulesPerString) { pvData.push({ label: 'Anzahl PV-Module AC' + acNr, value: ac.modulesPerString }); }

        pvData = pvData.concat([
          { label: 'Zählertyp AC' + acNr, value: ac.meterType },
          { label: 'Modbus Kommunikationsadresse AC' + acNr, value: ac.modbusCommunicationAddress }
        ]);
        acNr++;
      }
    }

    if (pvData.length > 0) {
      tableData.push({
        header: 'Erzeuger',
        rows: pvData
      });
    }

    if (environment.theme === 'Heckert') {
      const selectedFreeApp: EmsApp = this.ibn.selectedFreeApp;

      if (selectedFreeApp.id !== EmsAppId.None) {
        tableData.push({
          header: 'Apps',
          rows: [{ label: 'Ihre gewählte kostenlose App', value: selectedFreeApp.alias }]
        });
      }
    }
    return tableData;
  }

  /**
   * Returns the country label selected.
   *
   * @param countryValue the country value.
   * @returns country label.
   */
  public getCountryLabel(countryValue: string) {
    return COUNTRY_OPTIONS.find((country) => country.value === countryValue)?.label ?? '';
  }
}
