import { NgModule } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "../components/shared/dataservice";

@NgModule({
    providers: [
        {
            provide: DataService,
            useClass: LiveDataService,
        },
    ],
})
export class LiveDataServiceProvider { }
