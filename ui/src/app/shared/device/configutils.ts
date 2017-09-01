import { DefaultTypes } from '../service/defaulttypes';

export class ConfigUtils {

    /**
     * Gets all things that implement EssNature
     * 
     * @param config 
     */
    public static getEssNatures(config: DefaultTypes.Config): string[] {
        let things = [];
        for (let thingId in config.things) {
            let i = config.things[thingId].class;
            if (i.includes("EssNature") && !i.includes("EssClusterNature") /* ignore cluster */) {
                things.push(thingId);
            }
        }
        return things;
    }

    /**
     * Gets all things that implement MeterNature
     * 
     * @param config 
     */
    public static getMeterNatures(config: DefaultTypes.Config): string[] {
        let things = [];
        for (let thingId in config.things) {
            let i = config.things[thingId].class;
            if (i.includes("MeterNature")) {
                things.push(thingId);
            }
        }
        return things;
    }

    /**
      * Return ChannelAddresses of power channels
      */
    public static getPowerChannels(config: DefaultTypes.Config): DefaultTypes.ChannelAddresses {
        let ignoreNatures = { EssClusterNature: true };
        let result: DefaultTypes.ChannelAddresses = {}
        for (let thingId in config.things) {
            let i = config.things[thingId].class;
            let channels = [];
            // ESS
            if (i.includes("EssNature") && !i.includes("EssClusterNature") /* ignore cluster */) {
                if (i.includes("AsymmetricEssNature")) {
                    channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
                } else if (i.includes("SymmetricEssNature")) {
                    channels.push("ActivePower", "ReactivePower");
                }
                if (i.includes("FeneconCommercialEss")) { // workaround to ignore asymmetric meter for commercial
                    ignoreNatures["AsymmetricMeterNature"] = true;
                }
            }
            // Meter
            if (i.includes("MeterNature")) {
                if (i.includes("AsymmetricMeterNature") && !ignoreNatures["AsymmetricMeterNature"]) {
                    channels.push("ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3");
                } else if (i.includes("SymmetricMeterNature")) {
                    channels.push("ActivePower", "ReactivePower");
                }
            }
            // Charger
            if (i.includes("ChargerNature")) {
                channels.push("ActualPower");
            }
            // store result
            if (channels.length > 0) {
                result[thingId] = channels;
            }
        }
        return result;
    }

    /**
     * Returns ChannelAddresses of ESS Soc channels
     */
    public static getEssSocChannels(config: DefaultTypes.Config): DefaultTypes.ChannelAddresses {
        let result: DefaultTypes.ChannelAddresses = {}
        for (let thingId in ConfigUtils.getEssNatures(config)) {
            let i = config.things[thingId].class;
            let channels = [];
            // ESS
            channels.push("Soc");
            // store result
            if (channels.length > 0) {
                result[thingId] = channels;
            }
        }
        return result;
    }

    /**
     * Return ChannelAddresses of power and soc channels
     */
    public static getImportantChannels(config: DefaultTypes.Config): DefaultTypes.ChannelAddresses {
        let channels: DefaultTypes.ChannelAddresses = ConfigUtils.getPowerChannels(config);
        let essChannels = this.getEssSocChannels(config);
        // join/merge both results
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