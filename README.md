<h1 align="center">Mail2Blog</h1>

<p align="center">
    <img src="doc/mail2blog-logo.jpg" alt="Logo" width="33%">
    <br>
    <a href="https://travis-ci.org/dm-drogeriemarkt/Mail2Blog"><img src="https://travis-ci.org/dm-drogeriemarkt/Mail2Blog.svg?branch=master" alt="Build Status"></a>
    <a href="https://codecov.io/gh/dm-drogeriemarkt/Mail2Blog"><img src="https://img.shields.io/codecov/c/github/dm-drogeriemarkt/Mail2Blog.svg" alt="Build Status"></a>
</p>

A Confluence plugin that converts emails to confluence blog posts.

#### Features

- Supports POP3(s) and IMAP(s)
- Supports plain text, HTML and attachments
- Can handle MIME-mails and allows setting a preferred format (HTML or plain text)
- Supports complex HTML newsletters
- Customizable HTML filters
- Supports filtering sender addresses
- Supports blocking attachments with dangerous extensions
- Can post into different spaces based on the receivers address or the subject line

## First Steps

Installation:

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
- [Advanced Configuration](doc/advanced_configuration.md)
- [Technical Documentation](doc/technical_documentation.md)
- [FAQ](doc/faq.md)
- [Code Of Conduct](CODE_OF_CONDUCT.md)
- [License](LICENSE.txt)

## Origins

The project started off as a fork of the [Confluence-Mail-to-News-Plugin](https://github.com/stimmt/Confluence-Mail-to-News-Plugin) by Liip AG.
But the source code has been completely refactored and we added plenty of new features, bugfixes and tests.
The plugins aren't compatible to another anymore. Therefore we decided to publish this project on its own.
