Ripple Blob Vault
=================

A reactive Play2 application to store ripple wallets, backed by MongoDB.

This is a 100% PayWard-compatible application to store wallets used by
ripple-client. Simply replace the "Blob vault URL" client setting,
pointing to your ripple-blobvault installation, to make the switch.

This application performs additional checks and has additional features
compared to default ripple-blobvault offered by PayWard, in particular:

* basic access control
* basic blob/wallet validation
* ability to query on wallet meta-data
* enhanced logging
* 100% fully-asynchronous & scalable architecture


Configuration
-------------

In addition to mongodb.uri setting, you can customize a couple of
parameters in your `application.conf` file:

* `vault.allow_origin` -- allows to restrict access to a specific `Origin`
* `vault.log` -- enable/disable logging of every access
