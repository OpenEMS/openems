interface CurrentDataArguments {
    soc: number;
}

export class CurrentData {
    soc: number;
    constructor(args: CurrentDataArguments) {
        this.soc = args.soc;
    }
}
