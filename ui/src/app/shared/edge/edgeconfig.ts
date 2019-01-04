export class EdgeConfig {

    public components: { [id: string]: EdgeConfig.Component[] } = {};

    public factories: { [id: string]: EdgeConfig.Factory[] } = {};

    public isValid(): boolean {
        return this.components != {} && this.factories != {};
    }

}

export module EdgeConfig {

    export class Component {
        constructor(
            public readonly factoryPid: string = ""
        ) { }
    }

    export class Factory {
        constructor(
            public readonly natures: string[] = []
        ) { }
    }

}
