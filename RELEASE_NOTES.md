# Backwards Incompatible Changes

## Removal of `JAVA_XMS` and `JAVA_XMX`

I've added more flexible options `JAVA_OPTS` and `JAVA_ADDITIONAL_OPTS` to override or extend JVM settings, so these specific options are no longer needed.

## Removal of REMOVE and TEXT Redaction

Removing the **REMOVE** and **TEXT** options for `REDACTION_TYPE` (and the associated `REDACTION_TEXT` option). I feel the **BOX** and **BLUR** redaction options are stable and better to rely on, so I'm removing these legacy options.
