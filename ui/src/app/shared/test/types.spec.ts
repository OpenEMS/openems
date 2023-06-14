import { OeFormlyView } from "../genericComponents/shared/oe-formly-component";
import { Edge, EdgeConfig } from "../shared";

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
  public static createEdgeConfig(edge: Edge, components: { [id: string]: EdgeConfig.Component }, factories: { [id: string]: EdgeConfig.Factory }): EdgeConfig {
    return new EdgeConfig(edge, {
      components: components,
      factories: factories
    } as EdgeConfig
    );
  }

  public setConfig(components: { [id: string]: EdgeConfig.Component }, factories: { [id: string]: EdgeConfig.Factory }) {
    this.config = AbstractSystem.createEdgeConfig(this.edge, components, factories);
  }

  public setEdge(edge: Edge) {
    this.edge = edge;
  }
}

export class Modal {
  public name: string;
  public views: Map<number, OeFormlyView[]> = new Map();

  constructor(name: string, views: Map<number, OeFormlyView[]>) {
    this.name = name;
    this.views = views;
  }
}

export abstract class Widget {
  public abstract key: string;
  public abstract modal: Modal;
}

export class GridWidget extends Widget {

  public key: string = "Grid";
  public config: EdgeConfig;

  constructor(public modal: Modal) {
    super();
  }
}