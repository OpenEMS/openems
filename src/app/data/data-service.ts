import { CurrentData } from "./current-data";

export abstract class DataService {
    abstract getCurrentData(): Promise<CurrentData>;
}
