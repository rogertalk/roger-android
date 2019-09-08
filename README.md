# Roger Android

![RogerStory](img/grocery.gif)

# Developer notes

## Development tasks

For information on URI profile and referral codes see [Development Tasks](../docs/DEVELOPMENT_TASKS.md)

## Stream documentation

For insight on some parts on the way streams are handled internally consult [Streams Documentation](../docs/STREAM_DOCS.md)

## Code Style

Refer to the [Code Style document](CODE_STYLE.md) for complete guide on code styling.

## Android M+ Permissions

The application behaves in slightly different ways when permissions can be enforced at install time VS
at runtime. We made the decision to request only essential permissions at Runtime. For example _READ\_SMS_ 
was only used in minor non-critical situations, therefore it is only available by default on install-based permission
models.

## Backwards Compatibility

 * Secret code automatically login via SMS only work from Android version 19 to 22
 
## Other things of notice
 
Fictional number formatForDisplay used for testing: `+1 (XXX) 555-01XX` (X can be anything).
Secret code will appear on Slack's DEV channel.