import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Edge, Service } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { COUNTRY_OPTIONS } from '../../../../shared/type/country';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { Category } from '../../shared/category';
import { ComponentData, TableData } from '../../shared/ibndatatypes';
import { EmsApp, EmsAppId } from '../heckert-app-installer/heckert-app-installer.component';
import { Meter } from '../../shared/meter';
import { WebLinks } from '../../shared/enums';
import { System } from '../../shared/system';

@Component({
  selector: ConfigurationSummaryComponent.SELECTOR,
  templateUrl: './configuration-summary.component.html'
})
export class ConfigurationSummaryComponent implements OnInit {

  private static readonly SELECTOR = 'configuration-summary';

  @Input() public ibn: AbstractIbn;
  @Input() public edge: Edge;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<any>();

  protected form: FormGroup;
  protected fields: FormlyFieldConfig[];
  protected model;
  protected tableData: { header: string; rows: ComponentData[] }[] = [];

  constructor(
    private service: Service,
    private translate: TranslateService
  ) { }

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
      props: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.GTC_ACCEPT'),
        required: true,
        description: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.ACCEPT'),
        url: WebLinks.getLink(this.ibn.gtcAndWarrantyLinks.gtcLink)
      },
      wrappers: ['form-field-checkbox-hyperlink']
    });

    fields.push({
      key: 'isGuaranteeConditionsAccepted',
      type: 'checkbox',
      props: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.WARRANTY_TERMS'),
        required: true,
        defaultValue: false,
        description: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.ACCEPT'),
        url: WebLinks.getLink(this.ibn.gtcAndWarrantyLinks.warrantyLink)
      },
      wrappers: ['form-field-checkbox-hyperlink']
    });

    fields.push({
      key: 'isDevicesActiveChecked',
      type: 'checkbox',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.DEVICE_ACTIVE_CHECKED'),
        required: true
      }
    });

    return fields;
  }

  /**
   * Collect all the data for summary.
   */
  public generateTableData() {
    const tableData: TableData[] = [];
    const edgeData = this.edge.id;
    const generalData: ComponentData[] = [
      { label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.TIME_OF_INSTALLATION'), value: (new Date()).toLocaleString() },
      { label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.EDGE_NUMBER', { edgeShortName: environment.edgeShortName }), value: edgeData }
    ];

    const lineSideMeterFuseTitle: string = Category.toTranslatedString(this.ibn.lineSideMeterFuse.category, this.translate);
    if (this.ibn.lineSideMeterFuse.otherValue) {
      generalData.push({ label: lineSideMeterFuseTitle, value: this.ibn.lineSideMeterFuse.otherValue });
    } else {
      generalData.push({ label: lineSideMeterFuseTitle, value: this.ibn.lineSideMeterFuse.fixedValue });
    }

    tableData.push({
      header: Category.GENERAL,
      rows: generalData
    });

    const installer = this.ibn.installer;
    tableData.push({
      header: Category.INSTALLER,
      rows: [
        { label: this.translate.instant('Register.Form.company'), value: installer.companyName },
        { label: this.translate.instant('Register.Form.lastname'), value: installer.lastName },
        { label: this.translate.instant('Register.Form.firstname'), value: installer.firstName },
        { label: this.translate.instant('Register.Form.street'), value: installer.street },
        { label: this.translate.instant('Register.Form.zip'), value: installer.zip },
        { label: this.translate.instant('Register.Form.city'), value: installer.city },
        { label: this.translate.instant('Register.Form.country'), value: this.getCountryLabel(installer.country) },
        { label: this.translate.instant('Register.Form.email'), value: installer.email },
        { label: this.translate.instant('Register.Form.phone'), value: installer.phone }
      ]
    });

    const customer = this.ibn.customer;
    const customerData: ComponentData[] = customer.isCorporateClient ? [{ label: this.translate.instant('Register.Form.company'), value: customer.companyName }] : [];
    tableData.push({
      header: Category.CUSTOMER,
      rows: customerData.concat([
        { label: this.translate.instant('Register.Form.lastname'), value: customer.lastName },
        { label: this.translate.instant('Register.Form.firstname'), value: customer.firstName },
        { label: this.translate.instant('Register.Form.street'), value: customer.street },
        { label: this.translate.instant('Register.Form.zip'), value: customer.zip },
        { label: this.translate.instant('Register.Form.city'), value: customer.city },
        { label: this.translate.instant('Register.Form.country'), value: this.getCountryLabel(customer.country) },
        { label: this.translate.instant('Register.Form.email'), value: customer.email },
        { label: this.translate.instant('Register.Form.phone'), value: customer.phone }
      ])
    });

    const location = this.ibn.location;
    const locationData: ComponentData[] = location.isCorporateClient ? [{ label: this.translate.instant('Register.Form.company'), value: location.companyName }] : [];
    if (!location.isEqualToCustomerData) {
      tableData.push({
        header: Category.BATTERY_LOCATION,
        rows: locationData.concat([
          { label: this.translate.instant('Register.Form.lastname'), value: location.lastName },
          { label: this.translate.instant('Register.Form.firstname'), value: location.firstName },
          { label: this.translate.instant('Register.Form.street'), value: location.street },
          { label: this.translate.instant('Register.Form.zip'), value: location.zip },
          { label: this.translate.instant('Register.Form.city'), value: location.city },
          { label: this.translate.instant('Register.Form.country'), value: this.getCountryLabel(location.country) },
          { label: this.translate.instant('Register.Form.email'), value: location.email },
          { label: this.translate.instant('Register.Form.phone'), value: location.phone }
        ])
      });
    }

    const batteryData: ComponentData[] = [];
    batteryData.push(
      { label: this.translate.instant('Index.TYPE'), value: System.getSystemTypeLabel(this.ibn.type) }
    );

    tableData.push({
      header: Category.BATTERY,
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
    batteryInverterData.push({ label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.COUNTRY_SETTING'), value: this.getCountryLabel(safetyCountry) });
    tableData.push({
      header: Category.INVERTER,
      rows: batteryInverterData
    });

    const pv = this.ibn.pv ?? {};
    let pvData: ComponentData[] = [];

    // DC
    pvData = this.ibn.addCustomPvData(pvData);

    // AC
    let acNr = 1;
    const label = 'AC';
    for (const ac of pv.ac) {
      pvData = pvData.concat([
        { label: this.translate.instant('INSTALLATION.ALIAS_WITH_LABEL', { label: label, number: acNr }), value: ac.alias },
        { label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.VALUE_AC') + acNr, value: ac.value }
      ]);

      if (ac.orientation) {
        pvData.push({
          label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ORIENTATION_WITH_LABEL', { label: label, number: acNr }),
          value: ac.orientation
        });
      }
      if (ac.moduleType) {
        pvData.push({
          label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_WITH_LABEL', { label: label, number: acNr }),
          value: ac.moduleType
        });
      }
      if (ac.modulesPerString) {
        pvData.push({
          label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES_WITH_LABEL', { label: label, number: acNr }),
          value: ac.modulesPerString
        });
      }

      pvData = pvData.concat([
        {
          label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.METER_TYPE_WITH_LABEL', { label: label, number: acNr }),
          value: Meter.toLabelString(ac.meterType)
        },
        {
          label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODBUS_WITH_LABEL', { label: label, number: acNr }),
          value: ac.modbusCommunicationAddress
        }
      ]);
      acNr++;
    }

    if (pvData.length > 0) {
      tableData.push({
        header: Category.PRODUCER,
        rows: pvData
      });
    }

    let peakShavingData: ComponentData[] = [];
    peakShavingData = this.ibn.addPeakShavingData(peakShavingData);

    if (peakShavingData.length > 0) {
      tableData.push({
        header: Category.PEAK_SHAVING,
        rows: peakShavingData
      });
    }

    if (environment.theme === 'Heckert') {
      const selectedFreeApp: EmsApp = this.ibn.selectedFreeApp;

      if (selectedFreeApp.id !== EmsAppId.None) {
        tableData.push({
          header: Category.APPS,
          rows: [{ label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.HECKERT'), value: selectedFreeApp.alias }]
        });
      }
    }

    // Deepcopy to local tableData to the this.tabledata by repalcing category with translated string.
    this.tableData = tableData.map((element) => {
      return {
        header: Category.toTranslatedString(element.header, this.translate),
        rows: element.rows
      };
    });
  }

  /**
   * Returns the country label selected.
   *
   * @param countryValue the country value.
   * @returns country label.
   */
  public getCountryLabel(countryValue: string) {
    return COUNTRY_OPTIONS(this.translate).find((country) => country.value === countryValue)?.label ?? '';
  }
}
