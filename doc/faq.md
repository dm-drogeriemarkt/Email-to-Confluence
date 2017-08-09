# FAQ

### Why am I only getting plain text or HTML?

1. Check your formatting settings in the [advanced configuration](advanced_configuration.md)
2. Some mail servers like Exchange can be
[configured](https://technet.microsoft.com/en-us/library/aa997869%28v=exchg.150%29.aspx)
to only return plain text or HTML and not to deliver mails with alternative contents.
Please connect to your mailbox with a desktop client with the same protocol and check that
the messages contain the version you want to display in confluence. If you can, try to switch from
POP3 to IMAP this might be enough.

### Can I redirect messages into different spaces depending on the sender?

This feature is not implemented. However you could create an INBOX rule
that writes the space key into the subject line and uses the space in subject option in
the [advanced configuration](advanced_configuration.md).

### Can I use multiple Mailboxes?

No
