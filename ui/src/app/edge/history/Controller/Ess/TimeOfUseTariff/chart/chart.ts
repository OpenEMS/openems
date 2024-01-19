import { formatNumber } from '@angular/common';
import { Component, Input } from '@angular/core';
import { ChronoUnit, Data, Resolution, TooltipItem, calculateResolution } from 'src/app/edge/history/shared';
import { AbstractHistoryChart, ChartType } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress, Currency, EdgeConfig } from "src/app/shared/shared";

@Component({
    selector: 'scheduleChart',
    templateUrl: '../../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {

    @Input() public override component: EdgeConfig.Component;

    private TimeOfUseTariffState = TimeOfUseTariffUtils.TimeOfUseTariffState;
    private currencyLabel: Currency.Label; // Default

    protected override getChartData(): HistoryUtils.ChartData {
        const components: EdgeConfig.Component[] = this.config.getComponentsByFactory('Controller.Ess.Time-Of-Use-Tariff');

        // Assiging the component to be able to use the id.
        // There will always be only one controller enabled for Time-of-Use-Tariff. So finding the controller which is enabled.
        this.component = components.length > 1
            ? components.find(component => component.isEnabled)
            : components[0];

        const currency = this.config.components['_meta'].properties.currency;
        this.currencyLabel = Currency.getCurrencyLabelByCurrency(currency);
        this.chartType = ChartType.BAR;

        return {
            input: [
                {
                    name: 'QuarterlyPrice',
                    powerChannel: ChannelAddress.fromString(this.component.id + '/QuarterlyPrices'),
                },
                {
                    name: 'StateMachine',
                    powerChannel: ChannelAddress.fromString(this.component.id + '/StateMachine'),
                },
                {
                    name: 'Soc',
                    powerChannel: ChannelAddress.fromString('_sum/EssSoc'),
                },
            ],
            output: (data: HistoryUtils.ChannelData) => {
                return [{
                    name: this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.BALANCING'),
                    converter: () => {
                        return this.getDataset(data, this.TimeOfUseTariffState.Balancing);
                    },
                    color: 'rgb(51,102,0)',
                    stack: 1,
                },
                {
                    name: this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.CHARGE'),
                    converter: () => {
                        return this.getDataset(data, this.TimeOfUseTariffState.Charge);
                    },
                    color: 'rgb(0, 204, 204)',
                    stack: 1,
                },
                {
                    name: this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE'),
                    converter: () => {
                        return this.getDataset(data, this.TimeOfUseTariffState.DelayDischarge);
                    },
                    color: 'rgb(0,0,0)',
                    stack: 1,
                },
                {
                    name: this.translate.instant('General.soc'),
                    converter: () => {
                        return data['Soc']?.map(value => Utils.multiplySafely(value, 1000));
                    },
                    color: 'rgb(189, 195, 199)',
                    borderDash: [10, 10],
                    yAxisId: ChartAxis.RIGHT,
                    customType: ChartType.LINE,
                    customUnit: YAxisTitle.PERCENTAGE,
                }];
            },
            tooltip: {
                formatNumber: '1.1-2',
            },
            yAxes: [{
                unit: YAxisTitle.ENERGY,
                position: 'left',
                yAxisId: ChartAxis.LEFT,
            },
            {
                unit: YAxisTitle.PERCENTAGE,
                position: 'right',
                yAxisId: ChartAxis.RIGHT,
                displayGrid: false,
            }],
        };
    }

    protected override loadChart(): void {
        this.labels = [];
        this.errorResponse = null;

        const unit: Resolution = { unit: ChronoUnit.Type.MINUTES, value: 15 };

        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to, unit)
            .then((dataResponse) => {
                const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, dataResponse);
                this.datasets = displayValues.datasets;
                this.colors = displayValues.colors;
                this.legendOptions = displayValues.legendOptions;
                this.labels = displayValues.labels;
                this.setChartLabel();
            }).finally(() => {
                this.options.scales.xAxes[0].time.unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;
                this.options.scales.xAxes[0].ticks.source = 'auto';
                this.options.tooltips.mode = 'index';
                this.options.scales.xAxes[0].ticks.maxTicksLimit = 31;
                this.options.scales.yAxes[0].ticks.min = this.getMinimumAxisValue(this.datasets);
                this.options.scales.xAxes[0].offset = false;

                this.options.tooltips.callbacks.label = (tooltipItem: TooltipItem, data: Data) => {
                    let label = data.datasets[tooltipItem.datasetIndex].label;
                    let value = tooltipItem.value;
                    let tooltipsLabel = this.currencyLabel;

                    if (value === undefined || value === null || Number.isNaN(Number.parseInt(value.toString()))) {
                        return;
                    }

                    // Show floating point number for values between 0 and 1
                    // TODO find better workaround for legend labels
                    return label.split(":")[0] + ": " + formatNumber(value, 'de', '1.0-4') + " " + tooltipsLabel;
                };

                this.options.scales.yAxes[0].scaleLabel.labelString = this.currencyLabel;
            });
    }

    /**
     * Returns only the desired state data extracted from the whole dataset.
     * 
     * @param data The historic data.
     * @param desiredState The desired state data from the whole dataset.
     * @returns the desired state array data.
     */
    private getDataset(data: HistoryUtils.ChannelData, desiredState): any[] {
        var prices = data['QuarterlyPrice']
            .map(val => TimeOfUseTariffUtils.formatPrice(Utils.multiplySafely(val, 1000)));
        var states = data['StateMachine']
            .map(val => Utils.multiplySafely(val, 1000));
        var length = prices.length;
        var dataset = Array(length).fill(null);

        for (let index = 0; index < length; index++) {
            const quarterlyPrice = prices[index];
            const state = states[index];

            if (state !== null && state === desiredState) {
                dataset[index] = quarterlyPrice;
            }
        }

        return dataset;
    }

    /**
     * Returns the minimum value the chart should be scaled to.
     * 
     * @param datasets The chart datasets.
     * @returns the minumum axis value.
     */
    private getMinimumAxisValue(datasets: Chart.ChartDataSets[]): number {

        const labels = [
            this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.BALANCING'),
            this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.CHARGE'),
            this.translate.instant('Edge.Index.Widgets.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE'),
        ];

        const finalArray: number[] = labels
            .map(label => {
                const dataArray = datasets.find(dataset => dataset.label === label)?.data as number[];
                return dataArray ? dataArray.filter(price => price !== null) as number[] : [];
            })
            .reduce((acc, curr) => acc.concat(curr), []);

        return finalArray.length > 0 ? Math.floor(Math.min(...finalArray)) : 0;
    }
}
