import { formatNumber } from '@angular/common';
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { Converter } from 'src/app/shared/genericComponents/shared/converter';
import { DataService } from 'src/app/shared/genericComponents/shared/dataservice';

import { ChannelAddress, CurrentData, Utils } from '../../../shared/shared';
import { LiveDataService } from '../../live/livedataservice';

@Component({
  selector: HomeServiceAssistentComponent.SELECTOR,
  templateUrl: './homeassistent.html',
  providers: [{
    useClass: LiveDataService,
    provide: DataService
  }]
})
export class HomeServiceAssistentComponent extends AbstractFlatWidget {

  private static readonly SELECTOR = "homeserviceassistent";

  protected cellVoltageDifference: number | null = null;
  protected cellTemperatureDifference: number | null = null;

  protected date: string = this.service?.historyPeriod?.value?.getText(this.translate) ?? "";

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress('battery0', 'MaxCellVoltage'),
      new ChannelAddress('battery0', 'MinCellVoltage'),
      new ChannelAddress('battery0', 'MinCellTemperature'),
      new ChannelAddress('battery0', 'MinCellTemperature')
    ];
  }

  protected override onCurrentData(currentData: CurrentData): void {
    this.cellVoltageDifference = Utils.subtractSafely(currentData.allComponents['battery0/MaxCellVoltage'], currentData.allComponents['battery0/MinCellVoltage']);
    this.cellTemperatureDifference = Utils.subtractSafely(currentData.allComponents['battery0/MaxCellTemperature'], currentData.allComponents['battery0/MinCellTemperature']);
  }
  protected VOLTAGE_TO_VOLT_WITH_3_DECIMALS: Converter = (raw): string => {
    return Converter.IF_NUMBER(raw, value => formatNumber(value, 'de', '1.0-3') + " V");
  };

  protected VOLTAGE_IN_MILLI_VOLT_TO_VOLT_WITH_3_DECIMALS: Converter = (raw): string => {
    return Converter.IF_NUMBER(raw, value =>
      formatNumber(Utils.divideSafely(value, 1000), 'de', '1.0-3') + " V");
  };

  protected VOLTAGE_TO_MILLI_VOLT_WITH_3_DECIMALS: Converter = (raw): string => {
    return Converter.IF_NUMBER(raw, value =>
      formatNumber(value, 'de', '1.0-0') + " mV");
  };

  protected AMPERE_IN_MILLI_AMPERE_TO_AMPERE_WITH_3_DECIMALS: Converter = (raw): string => {
    return Converter.IF_NUMBER(raw, value =>
      formatNumber(Utils.divideSafely(value, 1000), 'de', '1.0-3') + " A"
    );
  };
}