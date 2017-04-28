export type BackendType = "femsserver" | "openems";

export interface Environment {
  production: boolean,
  websockets: [{
    name: string,
    url: string,
    backend: BackendType
  }]
}