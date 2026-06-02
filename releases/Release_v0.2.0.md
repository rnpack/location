## Release v0.2.0 (2026-06-02)

### New feature:

- **native**: rewrite @rnpack/location using native code([`a945111`](https://github.com/rnpack/location/commit/a9451110e75ab47c379f7fcb34b73e4ddd999c82)) (by Abiraman K)

### BREAKING CHANGES:

- The JavaScript API has changed. Methods like `getLastLocation()` are now asynchronous and return a Promise.