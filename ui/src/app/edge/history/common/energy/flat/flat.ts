// @ts-strict-ignore
import { Component } from "@angular/core";
import { format, isSameDay, isSameMonth, isSameYear } from "date-fns";
import { saveAs } from "file-saver-es";
import { PlatFormService } from "src/app/PLATFORM.SERVICE";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { QueryHistoricTimeseriesExportXlxsRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs";
import { Base64PayloadResponse } from "src/app/shared/jsonrpc/response/base64PayloadResponse";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { ChannelAddress, CurrentData, Utils } from "../../../../../shared/shared";

@Component({
    selector: "energy",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    private static readonly EXCEL_TYPE = "application/VND.OPENXMLFORMATS-OFFICEDOCUMENT.SPREADSHEETML.SHEET;charset=UTF-8";
    private static readonly EXCEL_EXTENSION = ".xlsx";
    protected autarchyValue: number | null;
    protected readonly isSmartphoneResolution = THIS.SERVICE.IS_SMARTPHONE_RESOLUTION;
    protected readonly isApp: boolean = PLAT_FORM_SERVICE.PLATFORM !== "web";

    public getChartHeight(): number {
        return THIS.SERVICE.DEVICE_HEIGHT / 2;
    }

    protected override onCurrentData(currentData: CurrentData) {
        THIS.AUTARCHY_VALUE =
            UTILS.CALCULATE_AUTARCHY(
                CURRENT_DATA.ALL_COMPONENTS["_sum/GridBuyActiveEnergy"] / 1000,
                CURRENT_DATA.ALL_COMPONENTS["_sum/ConsumptionActiveEnergy"] / 1000);
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

        if (THIS.IS_APP) {
            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("APP.FUNCTIONALITY_TEMPORARILY_NOT_AVAILABLE"), "warning");
            return;
        }

        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            EDGE.SEND_REQUEST(THIS.WEBSOCKET, new QueryHistoricTimeseriesExportXlxsRequest(DATE_UTILS.MAX_DATE(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.EDGE?.firstSetupProtocol), THIS.SERVICE.HISTORY_PERIOD.VALUE.TO)).then(response => {
                const r = response as Base64PayloadResponse;
                const binary = atob(R.RESULT.PAYLOAD.REPLACE(/\s/g, ""));
                const len = BINARY.LENGTH;
                const buffer = new ArrayBuffer(len);
                const view = new Uint8Array(buffer);
                for (let i = 0; i < len; i++) {
                    view[i] = BINARY.CHAR_CODE_AT(i);
                }
                const data: Blob = new Blob([view], {
                    type: FlatComponent.EXCEL_TYPE,
                });

                let fileName = "Export-" + EDGE.ID + "-";
                const dateFrom = THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM;
                const dateTo = THIS.SERVICE.HISTORY_PERIOD.VALUE.TO;
                if (isSameDay(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "DD.MM.YYYY");
                } else if (isSameMonth(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.") + "-" + format(dateTo, "DD.MM.YYYY");
                } else if (isSameYear(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "DD.MM.") + "-" + format(dateTo, "DD.MM.YYYY");
                } else {
                    fileName += format(dateFrom, "DD.MM.YYYY") + "-" + format(dateTo, "DD.MM.YYYY");
                }
                fileName += FlatComponent.EXCEL_EXTENSION;
                saveAs(data, fileName);

            }).catch(reason => {
                CONSOLE.WARN(reason);
            });
        });
    }
}

