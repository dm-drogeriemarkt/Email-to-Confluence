# Formatting

You can set the preferred content type (plain text or HTML)
and the allowed HTML elements in the formatting section on the configuration page.
You will get very different results depending on the settings chosen.
The following examples should help you to choose the right settings for your use case:

#### Example 1: Preferred format HTML, all elements allowed and HTML macro disabled

These are the default settings. This works great for most HTML mails,
but complex newsletters are usually not rendered flawlessly.
The generated blog posts can be easily edited with the Confluence WYSIWYG-editor.

| Display                               |
| ------------------------------------- |
| ![Display](example1_display.png)  |

| Editor                             |
| ---------------------------------- |
| ![Editor](example1_editor.png) |

#### Example 2: Preferred format HTML, all elements allowed and HTML macro enabled

Using the HTML macro even complex newsletters are usually displayed flawlessly,
but you can't edit the message with the Confluence WYSIWYG-editor.
To use this option you need to enable the HTML macro.

**Warning:** Activating the HTML macro in Confluence is a security issue
if non trusted users have permissions to create pages/blog posts

| Display                              |
| ------------------------------------ |
| ![Display](example2_display.png) |

| Editor                             |
| ---------------------------------- |
| ![Editor](example2_editor.png) |
   
#### Example 3: Preferred format text, all elements disallowed and HTML macro disabled

When setting plain text as preferred format the plugin will use the text version of a mail if available.
In HTML only mails all HTML will be stripped if all elements are disallowed.

| Display                              | 
| ------------------------------------ | 
| ![Display](example3_display.png) | 

| Editor                             |
| ---------------------------------- |
| ![Editor](example3_editor.png) |
