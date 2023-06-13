import { FormlyFieldConfig } from "@ngx-formly/core";
import { Edge, EdgeConfig } from "../../shared";

export abstract class AbstractSystem {
  public edge: Edge | null = null;
  public config: EdgeConfig;

  public abstract key: string;
  public abstract widgets: Widget[]
  public abstract components: { [id: string]: EdgeConfig.Component };

  /**
   * Creates a EdgeConfig
   * 
   * @param edge the edge
   * @param components the EdgeConfig Components
   * @param factories the EdgeConfig Factories
   * @returns a valid EdgeConfig
   */
  public createEdgeConfig(edge: Edge, components: { [id: string]: EdgeConfig.Component }, factories: { [id: string]: EdgeConfig.Factory }): EdgeConfig {
    return new EdgeConfig(edge, {
      components: components,
      factories: factories
    } as EdgeConfig
    );
  }

  public setConfig(components: { [id: string]: EdgeConfig.Component }, factories: { [id: string]: EdgeConfig.Factory }) {
    this.config = this.createEdgeConfig(this.edge, components, factories);
  }

  public setEdge(edge: Edge) {
    this.edge = edge;
  }
}

export class Modal {
  public name: string;
  public fieldsWithRoles: Map<number, FormlyFieldConfig[]> = new Map();

  constructor(name: string, fieldsWithRoles: Map<number, FormlyFieldConfig[]>) {
    this.name = name;
    this.fieldsWithRoles = fieldsWithRoles;
  }
}

export abstract class Widget {
  public abstract key: string;
  public abstract modal: Modal;
}

export class Grid extends Widget {

  public key: string = "Grid";
  public modal: Modal;
  public config: EdgeConfig;

  constructor(modal: Modal) {
    super();
    this.modal = modal;
  }
}