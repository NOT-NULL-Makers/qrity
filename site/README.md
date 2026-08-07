# QRity demonstration site

A static, self-contained page that generates QR symbols from free text
(`qrity.encode/encode-text`) and reads uploaded pictures
(`qrity.image-canvas` → `qrity.scan`), showing each symbol's properties
through `qrity.inspect`. Everything runs in the visitor's browser; no
upload leaves the page.

## Build

    clojure -M:site

compiles `site/src/qrity/site.cljs` with advanced optimizations into
`site/public/main.js` (git-ignored). The deployable artifact is the
`site/public/` directory — two files, no runtime dependencies — and any
static host serves it as-is.

## Local preview

    python3 -m http.server --directory site/public 8000

then open http://localhost:8000. A plain `file://` open also works in
browsers that allow canvas reads from same-origin object URLs.
