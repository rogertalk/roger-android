# Streams Documentation

## Infinite scrolling

Infinite scrolling implementation on the Android client works the following way:

StreamCacheRepo holds the `nextCursor` and `reachedListEnd`. StreamCacheRepo is responsible for updating the value of `reachedListEnd`. 
It becomes true if we ever try to write 'null' to `nextCursor`.

These values are not persisted to storage, so they'll expire with the app lifecycle.
 
If `nextCursor` is null, it gets immediately updated by a regular stream call. Otherwise it is only updated explicitly by requesting `next streams`.

The first set of streams are persisted locally, and further streams are only persisted in memory.

Streams should always be merged with existing ones. 
