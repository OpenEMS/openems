import { signal, WritableSignal } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { SumState } from "src/app/index/shared/sumState";
import { QueryHistoricTimeseriesEnergyResponse } from "../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge, EdgeConfig } from "../../shared";
import { DefaultTypes } from "../../type/defaulttypes";
import { Language } from "../../type/language";
import { Role } from "../../type/role";
import { AbstractService } from "../abstractservice";

export class DummyService extends AbstractService {

    public readonly edge = new Edge("edge0", "comment", "productype"
        , "1234.56.78", ROLE.ADMIN, true, new Date(), SUM_STATE.OK, new Date());

    public currentEdge: WritableSignal<Edge> = signal(THIS.EDGE);

    private readonly edgeConfig = new EdgeConfig(THIS.EDGE, undefined);

    setLang(id: Language) {
        throw new Error("Method not implemented.");
    }
    getDocsLang(): string {
        throw new Error("Method not implemented.");
    }
    notify(notification: DEFAULT_TYPES.NOTIFICATION) {
        throw new Error("Method not implemented.");
    }
    setCurrentComponent(currentPageTitle: string, activatedRoute: ActivatedRoute): Promise<Edge> {
        throw new Error("Method not implemented.");
    }
    getCurrentEdge(): Promise<Edge> {
        throw new Error("Method not implemented.");
    }
    getConfig(): Promise<EdgeConfig> {
        return new Promise((accept, reject) => {
            accept(THIS.EDGE_CONFIG);
        });
    }
    onLogout() {
        throw new Error("Method not implemented.");
    }
    getChannelAddresses(edge: Edge, channels: ChannelAddress[]): Promise<ChannelAddress[]> {
        throw new Error("Method not implemented.");
    }
    queryEnergy(fromDate: Date, toDate: Date, channels: ChannelAddress[]): Promise<QueryHistoricTimeseriesEnergyResponse> {
        throw new Error("Method not implemented.");
    }
    startSpinnerTransparentBackground(selector: string) {
        throw new Error("Method not implemented.");
    }
    startSpinner(selector: string) {
        throw new Error("Method not implemented.");
    }
    stopSpinner(selector: string) {
        throw new Error("Method not implemented.");
    }
    toast(message: string, level: "success" | "warning" | "danger") {
        throw new Error("Method not implemented.");
    }
    isPartnerAllowed(edge: Edge): boolean {
        throw new Error("Method not implemented.");
    }
    // https://V16.ANGULAR.IO/api/core/ErrorHandler#errorhandler

    override handleError(error: any): void {
        throw new Error("Method not implemented.");
    }

}
