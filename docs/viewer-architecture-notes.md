# Viewer Architecture Notes

Carrot PDF is now scoped as a local Google Drive-style PDF viewer with tabs.
The interface can be rebuilt freely, but these viewer behavior rules should stay intact.

## Preserved Fixes

- Zoom is visual-first, not layout-first.
- Pinch gestures update `PdfViewportState.visualScale` and pan offset.
- Releasing a pinch must not resize LazyColumn items.
- Render quality is refined after a debounce, not during every pinch frame.
- Existing bitmaps stay visible while sharper bitmaps render.
- Loading placeholders are only acceptable when a page has no bitmap yet.
- Render keys are tied to render quality buckets, not transient gesture frames.
- Page surfaces do not own bitmap lifecycle; the scheduler/cache does.
- User gestures must not restart pointer input by keying on changing zoom or bitmap values.
- Current page tracking should not fight active zoom, pan, or programmatic scroll.
- Heavy PDFs should favor stable interaction over aggressive rerendering.

## Product Scope

The main branch direction is intentionally narrow:

- Open local PDFs.
- Switch between multiple open PDFs.
- Read continuously with smooth zoom and pan.
- Search inside the current PDF.
- Show transient Drive-like reader chrome.
- Keep everything local and offline.

The following are out of scope until explicitly requested:

- Category/library management.
- Persistent document organization.
- Annotation editing.
- Bookmark systems beyond simple viewer affordances.
- Cloud sync or telemetry.

## Android Search

Android framework `PdfRenderer` exposes text/search APIs only on newer Android versions.
For Android 11 support, this branch adds `PdfBox-Android` as a text-search fallback.
That fallback can find matches and page numbers offline, but it does not currently provide
precise highlight bounds. Android 15+ framework search can provide bounds for yellow page
overlays.
