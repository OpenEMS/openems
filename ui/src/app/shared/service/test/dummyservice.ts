import { ActivatedRoute } from "@angular/router";
import { BehaviorSubject } from "rxjs";
import { SumState } from "src/app/index/shared/sumState";

import { QueryHistoricTimeseriesEnergyResponse } from "../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge, EdgeConfig } from "../../shared";
import { Language } from "../../type/language";
import { Role } from "../../type/role";
import { AbstractService } from "../abstractservice";
import { DefaultTypes } from "../defaulttypes";

export class DummyService extends AbstractService {

    public readonly edge = new Edge("edge0", "comment", "productype"
        , "1234.56.78", Role.ADMIN, true, new Date(), SumState.OK, new Date());

    public currentEdge: BehaviorSubject<Edge> = new BehaviorSubject(this.edge);

    private readonly edgeConfig = new EdgeConfig(this.edge, undefined);

    setLang(id: Language) {
        throw new Error("Method not implemented.");
    }
    getDocsLang(): string {
        throw new Error("Method not implemented.");
    }
    notify(notification: DefaultTypes.Notification) {
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
            accept(this.edgeConfig);
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
    override handleError(error: any): void {
        throw new Error("Method not implemented.");
    }

}
