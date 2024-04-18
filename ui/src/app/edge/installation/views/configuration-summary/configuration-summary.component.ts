import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Edge, Service } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { COUNTRY_OPTIONS } from '../../../../shared/type/country';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { Category } from '../../shared/category';
import { WebLinks } from '../../shared/enums';
import { ComponentData, TableData } from '../../shared/ibndatatypes';

@Component({
  selector: ConfigurationSummaryComponent.SELECTOR,
  templateUrl: './configuration-summary.component.html',
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
    private translate: TranslateService,
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
        url: WebLinks.getLink(this.ibn.gtcAndWarrantyLinks.gtcLink),
      },
      wrappers: ['form-field-checkbox-hyperlink'],
    });

    fields.push({
      key: 'isGuaranteeConditionsAccepted',
      type: 'checkbox',
      props: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.WARRANTY_TERMS'),
        required: true,
        defaultValue: false,
        description: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.ACCEPT'),
        url: WebLinks.getLink(this.ibn.gtcAndWarrantyLinks.warrantyLink),
      },
      wrappers: ['form-field-checkbox-hyperlink'],
    });

    fields.push({
      key: 'isDevicesActiveChecked',
      type: 'checkbox',
      templateOptions: {
        label: this.ibn.isDevicesActiveCheckedLabel,
        required: true,
      },
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
      { label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.EDGE_NUMBER', { edgeShortName: environment.edgeShortName }), value: edgeData },
    ];

    const lineSideMeterFuseTitle: string = Category.toTranslatedString(this.ibn.lineSideMeterFuse.category, this.translate);
    if (this.ibn.lineSideMeterFuse.otherValue) {
      generalData.push({ label: lineSideMeterFuseTitle, value: this.ibn.lineSideMeterFuse.otherValue });
    } else {
      generalData.push({ label: lineSideMeterFuseTitle, value: this.ibn.lineSideMeterFuse.fixedValue });
    }

    tableData.push({
      header: Category.GENERAL,
      rows: generalData,
    });

    tableData.push({
      header: Category.INSTALLER,
      rows: this.fillData(this.ibn.installer, Category.INSTALLER),
    });

    tableData.push({
      header: Category.CUSTOMER,
      rows: this.fillData(this.ibn.customer, Category.CUSTOMER),
    });

    if (!this.ibn.location.isEqualToCustomerData) {
      tableData.push({
        header: Category.BATTERY_LOCATION,
        rows: this.fillData(this.ibn.location, Category.BATTERY_LOCATION),
      });
    }

    const batteryData: ComponentData[] = this.ibn.addCustomBatteryData();
    batteryData.push({ label: this.translate.instant('Index.TYPE'), value: this.ibn.type });

    tableData.push({
      header: Category.BATTERY,
      rows: batteryData,
    });

    const batteryInverterData: ComponentData[] = this.ibn.addCustomBatteryInverterData();
    const safetyCountry = this.ibn.location.isEqualToCustomerData ? this.ibn.customer.country : this.ibn.location.country;
    batteryInverterData.push({ label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.COUNTRY_SETTING'), value: this.getCountryLabel(safetyCountry) });

    tableData.push({
      header: Category.INVERTER,
      rows: batteryInverterData,
    });

    // DC
    const pvData = this.ibn.addCustomPvData();
    if (pvData.length > 0) {
      tableData.push({
        header: Category.PRODUCER,
        rows: pvData,
      });
    }

    const meterData = this.ibn.addCustomMeterData();
    if (meterData.length > 0) {
      tableData.push({
        header: Category.GRID_METER_CATEGORY,
        rows: meterData,
      });
    }

    // Deepcopy to local tableData to the this.tabledata by repalcing category with translated string.
    this.tableData = tableData.map((element) => {
      return {
        header: Category.toTranslatedString(element.header, this.translate),
        rows: element.rows,
      };
    });
  }

  /**
   * Fills data based on the provided category.
   *
   * @param data  The data object containing information to be filled.
   * @param category The category determining which data fields to include.
   * @returns An array of ComponentData objects filled based on the provided data and category.
   */
  private fillData(data: any, category: Category.CUSTOMER | Category.INSTALLER | Category.BATTERY_LOCATION): ComponentData[] {
    const rows: ComponentData[] = [];

    if ((category === Category.CUSTOMER && data.isCorporateClient) || (category === Category.INSTALLER)) {
      rows.push({ label: this.translate.instant('Register.Form.company'), value: data.companyName });
    }

    rows.push(
      { label: this.translate.instant('Register.Form.lastname'), value: data.lastName },
      { label: this.translate.instant('Register.Form.firstname'), value: data.firstName },
      { label: this.translate.instant('Register.Form.street'), value: data.street },
      { label: this.translate.instant('Register.Form.zip'), value: data.zip },
      { label: this.translate.instant('Register.Form.city'), value: data.city },
      { label: this.translate.instant('Register.Form.country'), value: this.getCountryLabel(data.country) },
      { label: this.translate.instant('Register.Form.email'), value: data.email },
    );

    if (data.phone) {
      rows.push({ label: this.translate.instant('Register.Form.phone'), value: data.phone });
    }

    return rows;
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
