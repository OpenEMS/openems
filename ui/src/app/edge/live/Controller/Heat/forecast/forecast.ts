// @ts-strict-ignore

import { ChangeDetectorRef, Component, Inject, ChangeDetectionStrategy } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule, ModalController } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ComponentsBaseModule } from "../../../../../shared/components/components.module";
import { SchedulePowerChartComponent } from "./chart/power.chart";
import { ScheduleChartComponent } from "./chart/schedule.chart";

@Component({
    selector: "oe-controller-heat-forecast",
    templateUrl: "./forecast.html",
    standalone: true,
    styles: [
        `
            .ion-justify-with-space-between {
                ion-row > ion-col:nth-child(2) {
                    text-align: right;
                }
            }

            form {
                align-content: center !important;
            }
        `,
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [IonicModule, TranslateModule, ComponentsBaseModule, SchedulePowerChartComponent, ScheduleChartComponent],
})
export class HeatForecastComponent extends AbstractModal {
    constructor(
        @Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController)
        public override modalController: ModalController,
        @Inject(TranslateService)
        protected override translate: TranslateService,
        @Inject(FormBuilder) public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
    }

    override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params
                .pipe(
                    filter((params) => params != null),
                    take(1),
                )
                .subscribe((params) => {
                    this.component = config.getComponentSafely(params.componentId);
                    res();
                });
        });
    }
}
