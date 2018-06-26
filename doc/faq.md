# FAQ

### Why am I only getting plain text or HTML?

1. Check your [formatting settings](formatting.md)
2. Some mail servers like Exchange can be
[configured](https://technet.microsoft.com/en-us/library/aa997869%28v=exchg.150%29.aspx)
to only return plain text or HTML and not to deliver mails with alternative contents.
Please connect to your mailbox with a desktop client with the same protocol and check that
the messages contain the version you want to display in confluence. If you can, try to switch from
POP3 to IMAP this might be enough.

### Can I use multiple Mailboxes?

No. But you can setup a redirect on your mail provider and use TO/CC setting in space rules to distingish between mailboxes.

### Can I also create pages instead of blog posts?

This is possible since version 2.0

### All my emails are gone, what can I do?

If you’re using POP3 the default behavior is to delete processed emails. This is why there is a warning label above the selection and a warning pop-up on submission (JS required). If you used POP3 there really is little you can do. Unless you have a second email app on your mobile or some where else that mirrors emails. It may be worthwhile to log into the web interface of your email and look into the trash, many providers will move deleted email into a trash folder instead of permanently deleting them.

If you used IMAP the emails get moved to different folders below your INBOX: INBOX/Processed and INBOX/Invalid, some E-Mail clients won’t show those folders, unless you add them manually or they are difficult to spot.

Also see this [issue report](https://github.com/dm-drogeriemarkt/Email-to-Confluence/issues/4).
