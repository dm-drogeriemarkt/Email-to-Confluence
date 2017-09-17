## Technical Documentation

Further information, **in German**, on the code structure of this project is available [here](konzeption_und_entwicklung_des_confluence_add_ons_mail2blog.pdf).

## Compiling

Download and install the [atlassian SDK](https://developer.atlassian.com/docs/getting-started/downloads).
Change the confluence version in the `pom.xml` to your version. Run the `atlas-package` command in the plugin directory.
After successfully compiling the plugin, it will be available in `target/` as `mail2blog-$version.jar`.

## Integration tests

The source code comes with one integration test, that tests the whole process of converting mails to blog posts.
You can run the test with the command `atlas-integration` that will set up a confluence instance in the background
and will run the test in that instance. The whole test takes several minutes to finish. During development
it makes sense to run the test in a development environment, that you can start with `atlas-debug`, then you can start
the integration test via a REST call to:
`http://localhost:1990/confluence/rest/atlassiantestrunner/1.0/runtest/IntegrationTest`.

## The Main Process

Every 5 minutes the plugin polls the mail server for new mails.
The HTML included in the mail gets filtered and a new blog post with the contained content gets created.
When using IMAP the plugin will move the message into a folder called Processed
after successfully handling the message, in case of an error the message is moved into a folder called Invalid.
When using POP3 the message gets deleted, because POP3 doesn't support folders.

![Process](workflow_confluence_to_mail.jpg)

![Class Diagram](classdiagram.jpg)

## The Configuration Process

When using the configuration page the user communicates with the controller
`ConfigurationAction`. Initially the active used configuration is cloned,
then all operations are performed on the cloned configuration. After submitting the form successfully,
the new configuration is saved to storage and the global configuration gets set to the new one. This way we ensure
that an incorrect state during form validation isn't used to process messages. The `CheckboxTracker` class is a
workaround for the [issue](https://answers.atlassian.com/questions/120352/check-box-in-velocity) that checkboxes
only submit a value when getting checked and not when getting unchecked.

![Class Diagram](configuration-classdiagram.jpg)
