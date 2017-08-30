import { DefaultTypes } from '../service/defaulttypes';

export class ConfigUtils {

    /**
      * Return ChannelAddresses of power channels
      */
    public static getPowerChannels(config: DefaultTypes.Config): DefaultTypes.ChannelAddresses {
        // let natures = this._meta.natures;
        // let ignoreNatures = { EssClusterNature: true };
        let result = {}
        // for (let thing in natures) {
        //     let i = natures[thing].implements;
        //     let channels = [];

        //     // ESS
        //     if (!i.includes("EssClusterNature")) { // ignore cluster
        //         if (i.includes("AsymmetricEssNature")) {
        //             channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
        //         } else if (i.includes("SymmetricEssNature")) {
        //             channels.push("ActivePower", "ReactivePower");
        //         }
        //         if (i.includes("FeneconCommercialEss")) { // workaround to ignore asymmetric meter for commercial
        //             ignoreNatures["AsymmetricMeterNature"] = true;
        //         }
        //     }

        //     // Meter
        //     if (i.includes("AsymmetricMeterNature") && !ignoreNatures["AsymmetricMeterNature"]) {
        //         channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
        //     } else if (i.includes("SymmetricMeterNature")) {
        //         channels.push("ActivePower", "ReactivePower");
        //     }
        //     // Charger
        //     if (i.includes("ChargerNature")) {
        //         channels.push("ActualPower");
        //     }
        //     result[thing] = channels;
        // }
        return result;
    }

    /**
     * Returns ChannelAddresses of ESS Soc channels
     */
    public static getEssSocChannels(): DefaultTypes.ChannelAddresses {
        let channels: DefaultTypes.ChannelAddresses = {};
        // this.storageThings.forEach(device => channels[device] = ['Soc']);
        return channels;
    }

    /**
     * Return ChannelAddresses of power and soc channels
     */
    public static getImportantChannels(config: DefaultTypes.Config): DefaultTypes.ChannelAddresses {
        let channels: DefaultTypes.ChannelAddresses = ConfigUtils.getPowerChannels(config);
        let essChannels = this.getEssSocChannels();
        for (let thing in essChannels) {
            if (thing in channels) {
                let arr = essChannels[thing];
                channels[thing] = channels[thing].concat(arr);
            } else {
                channels[thing] = essChannels[thing];
            }
        }
        return channels;
    }
}