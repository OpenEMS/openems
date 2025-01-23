export interface SystemLog {
    time: string,
    level: "ERROR" | "WARN" | "INFO",
    source: string,
    message: string
}
