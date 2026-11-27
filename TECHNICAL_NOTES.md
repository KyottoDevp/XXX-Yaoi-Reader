# Technical notes

## Data flow

`Compose UI -> MainViewModel -> XXXYaoiClient -> OkHttp -> 3xyaoi.com`

- Jsoup parses returned HTML.
- Coil renders cover/page URLs.
- Gson stores the local library and reading history in private SharedPreferences.
- Chapter downloads are saved beneath `getExternalFilesDir(null)/downloads/`.

## Known integration gap

The supplied extension APK 1.6.75 contains JavaScript that watches `PerformanceObserver` entries for `/WP-manga/data/` requests, waits for the page reader to replace its image slots with `blob:` URLs, and maps those images back to the original network URLs by response size. That runtime is browser-dependent.

The official extension change merged on 26 November 2026 also uses a native WebView runtime for the reader session and captures request headers for image URLs. This project intentionally does not copy that WebView runtime because the product requirement is no embedded WebView. Therefore the native parser currently supports image URLs exposed in HTML attributes or inline script text, but may not support a reader that only materializes pages after browser JavaScript execution.

## Next engineering verification

1. Test `/biblioteca/` and `/bl/{slug}/` responses on a real Android network.
2. Confirm the current markup for chapter URLs and cover selectors.
3. Confirm whether reader page URLs are present in server HTML or only in browser-created requests.
4. If pages require interactive Turnstile or browser-derived `X-*` request headers, decide whether the no-WebView requirement remains absolute. A native-only implementation may not be technically possible without a site-supported API or an authorized server endpoint.
5. Add parser fixtures captured from pages the user is authorized to access, then write regression tests before changing selectors.

## Source-specific notes

The source domain and page patterns are configurable in `XXXYaoiClient.kt`. The application currently applies a conservative request interval and does not rotate identities, proxy requests, solve challenges, or evade rate limits.
