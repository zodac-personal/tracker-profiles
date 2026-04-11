## New value 'REMOVE' for option `REDACTION_TYPE`

Added the option to include **REMOVE** in `REDACTION_TYPE`, which will fully remove any sensitive information.

## New option `REDACTION_TEXT`

When selecting **TEXT** redaction in `REDACTION_TYPE` there is a new option to define the text used in redaction. Note that it will be truncated if the text to be redacted is shorter than the value defined in this option.

## Refinement of 'TEXT' value for option `REDACTION_TYPE`

Previously the **TEXT** option would replace the entire sensitive data with the redaction text, but this often caused the layout of the page to change. Now the width will be padded to retain the size(ish) of the element after redaction.

## New trackers

Added another 5 trackers and some minor tweaks to others.

## Dockerfile LABEL metadata

I've updated the Dockerfile to include OCI LABEL metadata.
