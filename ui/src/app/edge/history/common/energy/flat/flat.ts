// @ts-strict-ignore
import { Component, Inject } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { format, isSameDay, isSameMonth, isSameYear } from "date-fns";
import { saveAs } from "file-saver-es";
import { v4 as uuidv4 } from "uuid";
import { PlatFormService } from "src/app/platform.service";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { ViewUtils } from "src/app/shared/components/navigation/view/shared/shared";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { QueryHistoricTimeseriesExportXlxsRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs";
import { Base64PayloadResponse } from "src/app/shared/jsonrpc/response/base64PayloadResponse";
import { UserService } from "src/app/shared/service/user.service";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { ChannelAddress, CurrentData, Service, Utils, Websocket } from "../../../../../shared/shared";

@Component({
    selector: "energy",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    private static readonly EXCEL_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8";
    private static readonly EXCEL_EXTENSION = ".xlsx";
    protected spinnerId: string = uuidv4();
    protected autarchyValue: number | null = null;
    protected readonly isSmartphoneResolution = this.service.isSmartphoneResolution;
    protected readonly isApp: boolean = PlatFormService.platform !== "web";
    protected chartHeight: number | null = null;

    constructor(@Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController) protected override modalController: ModalController,
        @Inject(TranslateService) protected override translate: TranslateService,
        protected override dataService: DataService,
        protected override formBuilder: FormBuilder,
        protected override router: Router,
        protected override userService: UserService,
        private navigationService: NavigationService,
    ) {
        super(websocket, route, service, modalController, translate, dataService, formBuilder, router, userService);
        this.chartHeight = this.getChartHeight();
    }

    public getChartHeight(): number {
        return ViewUtils.getChartContentHeightInVh(window.innerHeight, this.navigationService.position());
    }

    protected override onCurrentData(currentData: CurrentData) {
        this.autarchyValue =
            Utils.calculateAutarchy(
                currentData.allComponents["_sum/GridBuyActiveEnergy"] / 1000,
                currentData.allComponents["_sum/ConsumptionActiveEnergy"] / 1000);
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress("_sum", "GridBuyActiveEnergy"),
            new ChannelAddress("_sum", "ConsumptionActiveEnergy"),
        ];
    }

    /**
   * Export historic data to Excel file.
    */
    protected exportToXlxs() {

        if (this.isApp) {
            this.service.toast(this.translate.instant("APP.FUNCTIONALITY_TEMPORARILY_NOT_AVAILABLE"), "warning");
            return;
        }

        this.service.getCurrentEdge().then(edge => {
            this.service.startSpinner(this.spinnerId);
            edge.sendRequest(this.websocket, new QueryHistoricTimeseriesExportXlxsRequest(DateUtils.maxDate(this.service.historyPeriod.value.from, this.edge?.firstSetupProtocol), this.service.historyPeriod.value.to)).then(response => {
                const r = response as Base64PayloadResponse;
                const binary = atob(r.result.payload.replace(/\s/g, ""));
                const len = binary.length;
                const buffer = new ArrayBuffer(len);
                const view = new Uint8Array(buffer);
                for (let i = 0; i < len; i++) {
                    view[i] = binary.charCodeAt(i);
                }
                const data: Blob = new Blob([view], {
                    type: FlatComponent.EXCEL_TYPE,
                });

                let fileName = "Export-" + edge.id + "-";
                const dateFrom = this.service.historyPeriod.value.from;
                const dateTo = this.service.historyPeriod.value.to;
                if (isSameDay(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.MM.yyyy");
                } else if (isSameMonth(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.") + "-" + format(dateTo, "dd.MM.yyyy");
                } else if (isSameYear(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.MM.") + "-" + format(dateTo, "dd.MM.yyyy");
                } else {
                    fileName += format(dateFrom, "dd.MM.yyyy") + "-" + format(dateTo, "dd.MM.yyyy");
                }
                fileName += FlatComponent.EXCEL_EXTENSION;
                saveAs(data, fileName);

            }).catch(reason => {
                console.warn(reason);
            }).finally(() => {
                this.service.stopSpinner(this.spinnerId);
            }
            );
        });
    }
}

