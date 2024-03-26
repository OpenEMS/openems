export enum GridMode {
    UNDEFINED = 0,
    ON_GRID = 1,
    OFF_GRID = 2,
}
export enum Mode {
    MANUAL_ON = 'MANUAL_ON',
    MANUAL_OFF = 'MANUAL_OFF',
    AUTOMATIC = 'AUTOMATIC'
}
export enum BatteryMode {
    UNDEFINED = -1, //
    NO_BATTERY = 0, //
    STANDBY = 1, //
    DISCHARGING = 2, //
    CHARGING = 3,
}
export enum GoodWe {
    UNDEFINED = -1, //
    GOODWE_GW10K_BT = 10, //
    GOODWE_GW8K_BT = 11, //
    GOODWE_GW5K_BT = 12, //
    GOODWE_GW10K_ET = 20, //
    GOODWE_GW8K_ET = 21, //
    GOODWE_GW5K_ET = 22,  //
    FENECON_FHI_10_DAH = 30, //
    FENECON_FHI_20_DAH = 120, //
    FENECON_FHI_29_9_DAH = 130,
}
export enum WorkMode {
    TIME = 'TIME',
    NONE = 'NONE'
}
export enum BackupEnable {
    AKTIVIERT = 1,
    DEAKTIVIERT = 2,
}
export enum BatteryStateMachine {
    UNDEFINED = -1, //
    GO_RUNNING = 10, //
    RUNNING = 11, //
    GO_STOPPED = 20, //
    ERROR = 30, //
}
export enum DredCmd {
    UNDEFINED = -1, //
    DRED0 = 0x00FF, //
    DRED1 = 0x0001, //
    DRED2 = 0x0002, //
    DRED3 = 0x0004, //
    DRED4 = 0x0008, //
    DRED5 = 0x0010, //
    DRED6 = 0x0020, //
    DRED7 = 0x0040, //
    DRED8 = 0x0080, //
}
export enum SafetyCountryCode {
    UNDEFINED = -1, //
    ITALY = 0,//
    CZECH = 1,//
    GERMANY = 2, //
    SPAIN = 3,
    GREECE_MAINLAND = 4, //
    DENMARK = 5, //
    BELGIUM = 6, //
    ROMANIA = 7, //
    G83_G59 = 8, //
    AUSTRALIA = 9, //
    FRANCE = 0x0A,//
    CHINA = 0x0B,//
    GRID_DEFAULT_60HZ = 0x0C, //
    POLAND = 0x0D,//
    SOUTH_AFRICA = 0x0E, //
    AUSTRALIA_L = 0x0F, //
    BRAZIL = 0x10,//
    THAILAND_MEA = 0x11, //
    THAILAND_PEA = 0x12, //
    MAURITIUS = 0x13, //
    HOLLAND = 0x14, //
    NORTHERN_IRELAND = 0x15,//
    CHINESE_STANDARD_HIGHER = 0x16, //
    FRENCH_50HZ = 0x17, //
    FRENCH_60HZ = 0x18, //
    AUSTRALIA_ERGON = 0x19, //
    AUSTRALIA_ENERGEX = 0x1A,//
    HOLLAND_16_20A = 0x1B, //
    KOREA = 0x1C,//
    CHINA_STATION = 0x1D, //
    AUSTRIA = 0x1E, //
    INDIA = 0x1F,//
    GRID_DEFAULT_50HZ = 0x20, //
    WAREHOUSE = 0x21, //
    PHILIPPINES = 0x22, //
    IRELAND = 0x23, //
    TAIWAN = 0x24,//
    BULGARIA = 0x25, //
    BARBADOS = 0x26, //
    G59_3 = 0x28,//
    SWEDEN = 0x29,//
    CHILE = 0x2A,//
    BRAZIL_LV = 0x2B, //
    NEWZEALAND = 0x2C, //
    IEEE1547_208VAC = 0x2D, //
    IEEE1547_220VAC = 0x2E, //
    IEEE1547_240VAC = 0x2F, //
    DEFAULT_60_HZ_LV = 0x30, //
    DEFAULT_50_HZ_LV = 0x31, //
    AUSTRALIA_WESTERN = 0x32,//
    AUSTRALIA_MICRO_GRID = 0x33,//
    JP_50_HZ = 0x34, //
    JP_60_HZ = 0x35, //
    INDIA_HIGHER = 0x36, //
    DEWA_LV = 0x37, //
    DEWA_MV = 0x38, //
    SLOVAKIA = 0x39, //
    GREEN_GRID = 0x3A, //
    HUNGARY = 0x3B, //
    SRILANKA = 0x3C, //
    SPAIN_ISLANDS = 0x3D, //
    ERAGON_30_K = 0x3E, //
    ENERGE_30_K = 0x3F, //
    IEEE1547_230VAC = 0x40, //
    IEC61727_60HZ = 0x41, //
    SWITZERLAND = 0x42, //
    CEI_016 = 0x43, //
    AUSTRALIA_HORIZON = 0x44, //
    CYPRUS = 0x45,//
    AUSTRALIA_SAPN = 0x46, //
    AUSTRALIA_AUSGRID = 0x47, //
    AUSTRALIA_ESSENTIAL = 0x48, //
    AUSTRALIA_PWCORE_CITI_PW = 0x49,//
}
