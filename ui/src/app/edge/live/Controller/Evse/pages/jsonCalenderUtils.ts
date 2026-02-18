export function computeIsoDuration(startDate: Date, endDate: Date): string {
    const durationMs = endDate.getTime() - startDate.getTime();
    const totalMinutes = Math.floor(durationMs / 60000);
    const days = Math.floor(totalMinutes / (24 * 60));
    const hours = Math.floor((totalMinutes % (24 * 60)) / 60);
    const minutes = totalMinutes % 60;

    let duration = "P";
    if (days > 0) {
        duration += `${days}D`;
    }
    if (hours > 0 || minutes > 0) {
        duration += "T";
    }
    if (hours > 0) {
        duration += `${hours}H`;
    }
    if (minutes > 0) {
        duration += `${minutes}M`;
    }
    return duration;
}

export function formatIsoLocalDateTime(date: Date): string {
    return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, "0")}-${date.getDate().toString().padStart(2, "0")}T${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}:00`;
}

export function parseISODuration(isoDuration: string): { days: number, hours: number, minutes: number, seconds: number } | null {
    const match = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$/.exec(isoDuration);

    if (!match) {
        return null;
    }

    const [_, days, hours, minutes, seconds] = match;

    return {
        days: days ? Number(days) : 0,
        hours: hours ? Number(hours) : 0,
        minutes: minutes ? Number(minutes) : 0,
        seconds: seconds ? Number(seconds) : 0,
    };
}
