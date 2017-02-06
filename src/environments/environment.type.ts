export type BackendType = "femsserver" | "openems";

export interface Environment {
    production: boolean,
    backend: BackendType,
    url: string
}