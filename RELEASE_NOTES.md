# Backwards Incompatible Changes

## Removal of `JAVA_XMS` and `JAVA_XMX`

I've added more flexible options `JAVA_OPTS` and `JAVA_ADDITIONAL_OPTS` to override or extend JVM settings, so these specific options are no longer needed.
