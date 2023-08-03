import { OnInit } from '@angular/core';
import { Component } from '@angular/core';
import { AbstractHistoryChartOverview } from 'src/app/shared/genericComponents/chart/abstractHistoryChartOverview';

@Component({
    templateUrl: './overview.html'
})
export class OverviewComponent extends AbstractHistoryChartOverview implements OnInit {

    public override ngOnInit(): void {
        this.componentId = this.route.snapshot.params.componentId;
        super.ngOnInit();
    }
}  