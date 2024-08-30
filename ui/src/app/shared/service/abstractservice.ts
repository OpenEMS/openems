// @ts-strict-ignore
import { ErrorHandler } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { BehaviorSubject } from "rxjs";
import { Edge } from "../components/edge/edge";
import { EdgeConfig } from "../components/edge/edgeconfig";
import { QueryHistoricTimeseriesEnergyResponse } from "../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress } from "../shared";
import { Language } from "../type/language";
import { DefaultTypes } from "./defaulttypes";

export abstract class AbstractService extends ErrorHandler {

  /**
   * Holds the currently selected Edge.
   */
  public abstract currentEdge: BehaviorSubject<Edge>;

  /**
   * Set the application language
   */
  abstract setLang(language: Language): void;

  /**
   * Returns the configured language for docs.fenecon.de
   */
  abstract getDocsLang(): string;

  /**
   * Shows a nofication using toastr
   */
  abstract notify(notification: DefaultTypes.Notification);

  /**
   * Parses the route params and sets the current edge
  */
  abstract setCurrentComponent(currentPageTitle: string, activatedRoute: ActivatedRoute): Promise<Edge>;

  /**
   * Gets the current Edge - or waits for a Edge if it is not available yet.
   */
  abstract getCurrentEdge(): Promise<Edge>;

  /**
   * Gets the EdgeConfig of the current Edge - or waits for Edge and Config if they are not available yet.
   */
  abstract getConfig(): Promise<EdgeConfig>;

  /**
   * Handles being logged out.
   */
  abstract onLogout();

  /**
   * Gets the ChannelAddresses for cumulated values that should be queried.
   *
   * @param edge the current Edge
   */
  abstract getChannelAddresses(edge: Edge, channels: ChannelAddress[]): Promise<ChannelAddress[]>;

  /**
   * Sends the Historic Timeseries Data Query and makes sure the result is not empty.
   *
   * @param fromDate the From-Date
   * @param toDate   the To-Date
   * @param edge     the current Edge
   * @param ws       the websocket
   */
  abstract queryEnergy(fromDate: Date, toDate: Date, channels: ChannelAddress[]): Promise<QueryHistoricTimeseriesEnergyResponse>;

  /**
   * Start NGX-Spinner
   *
   * The spinner has a transparent background set
   * and the spinner color is the primary environment color
   * Spinner will appear inside html tag only
   *
   * @example <ngx-spinner name="YOURSELECTOR"></ngx-spinner>
   *
   * @param selector selector for specific spinner
   */
  abstract startSpinnerTransparentBackground(selector: string);

  /**
   * Start NGX-Spinner
   *
   * Spinner will appear inside html tag only
   *
   * @example <ngx-spinner name="YOURSELECTOR"></ngx-spinner>
   *
   * @param selector selector for specific spinner
   */
  abstract startSpinner(selector: string);

  /**
   * Stop NGX-Spinner
   * @param selector selector for specific spinner
   */
  abstract stopSpinner(selector: string);

  abstract toast(message: string, level: "success" | "warning" | "danger");

}
