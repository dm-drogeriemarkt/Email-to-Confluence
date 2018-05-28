<h1 align="center">Email to Confluence</h1>

<p align="center">
    <img src="doc/mail2blog-logo.jpg" alt="Logo" width="33%">
    <br>
    <a href="https://travis-ci.org/dm-drogeriemarkt/Email-to-Confluence"><img src="https://travis-ci.org/dm-drogeriemarkt/Email-to-Confluence.svg?branch=master" alt="Build Status"></a>
    <a href="https://codecov.io/gh/dm-drogeriemarkt/Email-to-Confluence"><img src="https://img.shields.io/codecov/c/github/dm-drogeriemarkt/Email-to-Confluence.svg" alt="Build Status"></a>
</p>

Send email to Confluence. This add-on converts emails to Confluence pages or blog posts. Store and reference your weekly/daily/monthly newsletters in Confluence.

<p align="center">
    <img width="33%" src="doc/example-blog-post.jpg">
</p>

#### Features

- Supports POP3(s) and IMAP(s)
- Supports plain text, HTML and attachments
- Can handle MIME-mails and allows setting a preferred format (HTML or plain text)
- Supports complex HTML newsletters
- Customizable HTML filters
- Supports filtering sender addresses
- Supports blocking attachments with dangerous extensions
- Can post into different spaces

## First Steps

This add-on is available on the [Atlassian Marketplace](https://marketplace.atlassian.com/plugins/de.dm.mail2blog.mail2blog/server/overview).

Manual Installation:

1. Download the latest release from the release tab.
2. [Install the plug-in by uploading it to your Confluence instance](https://confluence.atlassian.com/upm/installing-add-ons-273875715.html#Installingadd-ons-Installingbyfileupload).

After installation:

1. Go to `admin/plugins/mail2blog/editmailconfiguration.action`
2. Fill in the fields for the mail configuration
3. Choose a default [space](https://confluence.atlassian.com/doc/spaces-139459.html) into which to post
4. Save the configuration

From now on **every 5 minutes** Confluence will pull messages from your **INBOX**
and post them as blog posts into the default space.

The configuration is explained on the configuration page.
The advanced configuration options are explained [here](doc/advanced_configuration.md).

## Further Information
- [Formatting](doc/formatting.md)
- [Technical Documentation](doc/technical_documentation.md)
- [Security Guide](doc/security_guide.md)
- [FAQ](doc/faq.md)
- [Konzeption und Entwicklung des Confluence-Add-ons Mail2Blog (German)](doc/konzeption_und_entwicklung_des_confluence_add_ons_mail2blog.pdf)
- [Code Of Conduct](CODE_OF_CONDUCT.md)
- [License](LICENSE.txt)

## Origins

The project started off as a fork of the [Confluence-Mail-to-News-Plugin](https://github.com/stimmt/Confluence-Mail-to-News-Plugin) by Liip AG.
But the source code has been completely refactored and we added plenty of new features, bugfixes and tests.
The plugins are not compatible to another anymore. Therefore we decided to publish this project on its own.

Formerly this plugin was named Mail2Blog, but since version 2.0 this plugin is capable of creating pages and blog posts
and was renamed to Email to Confluence.
