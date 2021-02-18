import { Component, Input, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { ChartDataSets } from 'chart.js';
import { add, setMilliseconds, setMinutes, setSeconds } from 'date-fns';
import { Color } from 'ng2-charts';
import { ComponentJsonApiRequest } from '../../../../shared/jsonrpc/request/componentJsonApiRequest';
import { Get24HoursPredictionRequest } from '../../../../shared/jsonrpc/request/get24HoursPredictionRequest';
import { Get24HoursPredictionResponse } from '../../../../shared/jsonrpc/response/get24HoursPredictionResponse';
import { ChannelAddress, Edge, Service, Utils } from '../../../../shared/shared';
import { ChartOptions, DEFAULT_TIME_CHART_OPTIONS } from '../../../history/shared';

@Component({
    selector: 'prediction-modal',
    templateUrl: './modal.component.html'
})
export class PredictionModalComponent implements OnInit {

    private static SUM_PRODUCTION_ACTIVE_POWER = new ChannelAddress("_sum", "ProductionActivePower");
    private static SUM_CONSUMPTION_ACTIVE_POWER = new ChannelAddress("_sum", "ConsumptionActivePower");

    public options;
    public labels: Date[] = [...Array(96).keys()].map(i => add(setMilliseconds(setSeconds(setMinutes(new Date(), 0), 0), 0), { minutes: i * 15 }));
    public datasets: ChartDataSets[];
    public colors: Color[] = [{
        backgroundColor: 'rgba(0,191,255,0.05)',
        borderColor: 'rgba(0,191,255,1)',
    }];

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) {
        this.options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        this.options.maintainAspectRatio = true;
    }

    ngOnInit() {
        let channels = [
            PredictionModalComponent.SUM_PRODUCTION_ACTIVE_POWER,
            // PredictionModalComponent.SUM_CONSUMPTION_ACTIVE_POWER,
            // new ChannelAddress("pvInverter2", "ActivePower"),
            new ChannelAddress("pvInverter0", "ActivePower"),
            // new ChannelAddress("charger0", "ActualPower"),
            // new ChannelAddress("charger1", "ActualPower"),
        ]
        this.service.getCurrentEdge().then(edge => {
            edge.sendRequest(this.service.websocket,
                new ComponentJsonApiRequest({
                    componentId: '_predictorManager',
                    payload: new Get24HoursPredictionRequest(channels)
                })).then(response => {
                    let result = (response as Get24HoursPredictionResponse).result;
                    let datasets = [];
                    for (const [key, value] of Object.entries(result)) {
                        datasets.push({
                            label: key,
                            data: value
                        });
                    }
                    this.datasets = datasets;
                }).catch(reason => {
                    console.warn(reason);
                })
        });
    }
}