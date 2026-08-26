import { UrlMatchResult, UrlMatcher, UrlSegment } from "@angular/router";

/**
 * Creates a UrlMatcher that matches any route whose last path segment equals the given suffix.
 *
 * @param suffix the required last path segment.
 * @param baseParam the name of the positional parameter that will contain the base path.
 *
 * @returns an angular UrlMatcher function for use in route definitions.
 */
export function suffixMatcher(
    suffix: string,
    baseParam: string = "base"
): UrlMatcher {
    return (segments): UrlMatchResult | null => {
        if (segments.length === 0) {
            return null;
        }
        if (segments[segments.length - 1].path !== suffix) {
            return null;
        }

        const base = segments.slice(0, -1).map(s => s.path).join("/");

        return {
            consumed: segments,
            posParams: {
                [baseParam]: new UrlSegment(base, {}),
            },
        };
    };
}
