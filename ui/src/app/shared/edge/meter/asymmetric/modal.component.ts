import { Component, Input, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { GridMode, Service, Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';
import { EdgeConfig } from '../../edgeconfig';

@Component({
    selector: 'oe-asymmetricMeter',
    templateUrl: './modal.component.html'
})
export class AsymmetricMeterComponent implements OnInit {

    @Input() protected component: EdgeConfig.Component;
    @Input() protected type?: 'GRID' | null = null;
    protected readonly Role = Role;
    protected readonly Utils = Utils;
    public readonly TextIndentation = TextIndentation;
    protected grid: { mode: GridMode, buyFromGrid: number, sellToGrid: number, phases?: { name: string, power: number, current: number, voltage: number }[] } =
        {
            mode: GridMode.UNDEFINED,
            buyFromGrid: 0,
            sellToGrid: 0,
            phases: []
        }

    constructor(public service: Service, public translate: TranslateService) { }

    ngOnInit() {
        if (this.type == "GRID") {
            this.service.getCurrentEdge().then((edge) => {
                edge.currentData.pipe(
                    filter(currentData => currentData != null))
                    .subscribe((currentData) => {

                        this.grid.mode = currentData.channel["_sum/GridMode"]
                        this.grid.phases.forEach((element, index) => {
                            element.name = "Phase L" + (index + 1) + " " + this.translate.instant(currentData.channel[this.component.id + "/ActivePowerL" + (index + 1)] > 0 ? "General.gridBuyAdvanced" : "General.gridSellAdvanced");
                            element.power = Math.abs(currentData.channel[this.component.id + "/ActivePowerL" + (index + 1)]) ?? 0;
                            element.current = currentData.channel[this.component.id + '/CurrentL' + (index + 1)];
                            element.voltage = currentData.channel[this.component.id + '/VoltageL' + (index + 1)];
                        });
                    });
            });
        }
    }
}