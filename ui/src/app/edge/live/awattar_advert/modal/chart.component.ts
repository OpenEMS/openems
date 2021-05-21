import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Service, Utils } from 'src/app/shared/shared';
import { ChartOptions, ChartType, ChartDataSets } from 'chart.js';
import { Label } from 'ng2-charts';

@Component({
    selector: 'awattarChart',
    templateUrl: './chart.component.html'
})
export class AwattarAdvertChartComponent {

    public barChartColors = [
        { backgroundColor: 'rgba(255,53,0, 1)' },
        { backgroundColor: 'rgba(0,191,255, 1)' },
        {
            backgroundColor: 'rgba(255,53,0, 1)',
            borderColor: 'rgba(0,0,0, 1)',
            borderWidth: '2'
        },
    ]

    public barChartOptions: ChartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        // We use these empty structures as placeholders for dynamic theming.
        scales: {
            xAxes: [{}], yAxes: [{
                scaleLabel: {
                    display: true,
                    labelString: 'Strompreis (Cent/kWh)'
                },
                ticks: {
                    stepSize: 0.5,
                }

            }]
        },
        plugins: {
            datalabels: {
                anchor: 'end',
                align: 'end',
            }
        },
        legend: {
            position: 'bottom'
        }
    };


    constructor(
        protected service: Service,
        private route: ActivatedRoute,
        private translate: TranslateService
    ) { }

    public barChartLabels: Label[] = ['0:00', '1:00', '2:00', '3:00', '4:00', '5:00', '6:00', '7:00', '8:00', '9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00', '23:00'];
    public barChartType: ChartType = 'bar';
    public barChartLegend = true;

    public barChartData: ChartDataSets[] = [
        { data: [null, 1.22, -0.39, -0.47, -0.02, null, null, null, null, null, null, null, null, 0.80, null, null, null, null, null, null, null, null, 1.40, 0.25], label: 'Gesperrt', stack: 'a' },
        { data: [2.81, null, null, null, null, 2.78, 3.31, 3.81, 3.47, 2.91, 2.17, null, 2.90, null, 1.90, 1.68, 1.89, 2.93, 4.26, 4.31, 2.9, 2.29, null, null], label: 'Freigegeben', stack: 'a' },
        { data: [null, null, null, null, null, null, null, null, null, null, null, 0.88, null, null, null, null, null, null, null, null, null, null, null, null], label: 'aktive Stunde', stack: 'a' },
    ];

    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.2;
    }
}