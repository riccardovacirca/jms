# Getting Started with Vonage CLI

The Vonage CLI has many commands that can bootstrap with Vonage's APIs. While you can use the dashboard to accomplish many CLI tasks, you can also programmatically set up and configure your application on your server. Already have a Vonage application configured? The CLI can help you quickly export the configuration into JSON or YAML to consume in your program.

## What's New in Version 3

Those new to the CLI can skip this section. The latest version 3 of the CLI offers a ground-up, pragmatic design to address some of the shortcomings of versions past.

### Standardized Flags

All flags in the Vonage CLI now use **kebab-case**, ensuring consistency and making commands more straightforward.

### JSON and YAML Output Support

You can now specify the output format of commands using JSON or YAML, providing flexibility to integrate with your tools and workflows.

### Grouped Commands by Action

Commands are organized by action to enhance usability and make it easier to find the functionality you need.

### Built with yargs

Version 3 is built from the ground up using the `yargs` [link](https://yargs.js.org/) package, providing a robust and user-friendly experience.

### Easier configuration

Configuration for the CLI has been simplified to make it easier to work within a Vonage application or Vonage Account. [See below](https://developer.vonage.com/en/getting-started/tools/cli#configuration) for more information on how to configure the CLI

### Automatic updates

Version 3 will periodically check for new updates and inform you when to upgrade. This will ensure that you are calling the Vonage APIs correctly and that the tool is bug-free.

## Installation

The Vonage CLI is written with [NodeJS](https://nodejs.org) and utilizes the `@vonage/server-sdk` [link](https://github.com/vonage/vonage-node-sdk) package. The CLI will always work with the lowest Long Term Supported (LTS) [version](https://nodejs.org/en/about/previous-releases) of NodeJS (currently 18.20).

### Installing with npm

To install the Vonage CLI using `npm`, run the following command:

### Powershell (Windows)

```powershell
npm install -g @vonage/cli
```

### CMD (Windows)

```cmd
npm install -g @vonage/cli
```

### Bash

```bash
$ npm install -g @vonage/cli
```

### Installing with yarn

To install the Vonage CLI using `yarn`,

### Powershell (Windows)

```powershell
yarn global add @vonage/cli
```

### CMD (Windows)

```cmd
yarn global add @vonage/cli
```

### Bash

```bash
$ yarn global add @vonage/cli
```

## Global Flags

The Vonage CLI provides a set of global flags that are available for all commands:

*   `--verbose`: Print more information.
*   `--debug`: Print debug information.
*   `--no-color`: Toggle color output off.
*   `--help`: Show help.

`verbose` and `debug` information will be written to `STDERR` to allow piping output into other programs

## Authentication

The Vonage CLI uses a flexible configuration system to manage your API credentials. It supports local or global configuration files and command-line flags for overriding these values, allowing you to tailor your setup based on your project needs or personal preferences.

### Configuration

The CLI will load the configuration in the following order:

1.  Command line flags `--api-key`, `--api-secret`, `--private-key`, and `--app-id`.
2.  A local configuration file in the current working directory `.vonagerc`.
3.  A global configuration file in the `.vonage` folder in your home directory `$HOME/.vonage/config.json`.

> Note: Only the CLI will read these values from .vonagerc. The Vonage SDKs requires separate initialization with its own credentials.

> Note: The contents of the private key will be stored inside the configuration file. This is be design to help ensure the key is not overwritten when new keys are generated

**Flags**:

*   `--api-key`: The API key found in the "API settings" section of your dashboard .
*   `--api-secret`: The API secret found in the "API Settings" section of your dashboard.
*   `--app-id`: The ID of the application to use. This is found in the "Applications" section of the dashboard or outputted with `vonage apps`.
*   `--private-key`: The path or contents of the private key. The private key can only be accessed when the application is created or when you regenerate the keys in the dashboard.

### Set Authentication

While you can use the CLI without configuring it, you will be required to pass in the flags when running a command. Using the `vonage auth set` command is recommended to save you from typing them every time you run a command.

**Flags**:

This command uses the [global authentication flags](https://developer.vonage.com/en/getting-started/tools/cli#configuration)

**Examples**:

Configure your Vonage API credentials:

### Powershell (Windows)

```powershell
vonage auth set `
--api-key='your-api-key' `
--api-secret='your-api-secret' `
--app-id='your-application-id' `
--private-key=C:\path\to\private.key

API Key: your-api-key
API Secret: your-**************
App ID: your-application-id
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### CMD (Windows)

```cmd
vonage auth set ^
--api-key='your-api-key' ^
--api-secret='your-api-secret' ^ 
--app-id='your-application-id' ^
--private-key=C:\path\to\private.key

API Key: your-api-key
API Secret: your-**************
App ID: your-application-id
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### Bash

```bash
$ vonage auth set \
--api-key='your-api-key' \
--api-secret='your-api-secret' \
$ --app-id='your-application-id' \
--private-key=/path/to/private.key
$ API Key: your-api-key
$ API Secret: your-**************
$ App ID: your-application-id
$ Private Key: Is Set
$ ✅ Checking API Key Secret
$ ✅ Checking App ID and Private Key
```

> Note: running vonage auth set will not remove current values. Therefore, you can set just the API Key/Secret or App ID/Private Key individually. However, you will not be able to set the App ID and Private key without having the API key and secret set. This is due to how the command checks the credentials are valid.

> Note: This command will also check the credentials are correct before committing.

### Check Authentication

Verify that your authentication details are valid. By default, this will use the global configuration file. Checking credentials works as follows:

1.  The API Key and secret are checked by making a call to list the applications using the [Applications API](https://developer.vonage.com/en/application/overview).
2.  The App ID and Private key are validated by fetching the application information and using the public key along with the private key to ensure they are paired correctly.

> Note: This command will not use the command line arguments. It will only check the configuration files

**Flags**:

*   `--local`: Use the local configuration file (`.vonagerc`).

**Examples**:

Check the global configuration:

### Powershell (Windows)

```powershell
vonage auth check 

Global credentials found at: C:\Users\bob\.vonage\config.json

API Key: abcd1234
API Secret: abc**************
App ID: 00000000-0000-0000-0000-000000000000
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### CMD (Windows)

```cmd
vonage auth check 

Global credentials found at: C:\Users\bob\.vonage\config.json

API Key: abcd1234
API Secret: abc**************
App ID: 00000000-0000-0000-0000-000000000000
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### Bash

```bash
$ vonage auth check
$ Global credentials found at: /Users/bob/.vonage/config.json
$ API Key: abcd1234
$ API Secret: abc**************
$ App ID: 00000000-0000-0000-0000-000000000000
$ Private Key: Is Set
$ ✅ Checking API Key Secret
$ ✅ Checking App ID and Private Key
```

Check the local configuration:

### Powershell (Windows)

```powershell
vonage auth check --local

Global credentials found at: .vonagerc

API Key: abcd1234
API Secret: abc**************
App ID: 00000000-0000-0000-0000-000000000000
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### CMD (Windows)

```cmd
vonage auth check --local

Global credentials found at: .vonagerc

API Key: abcd1234
API Secret: abc**************
App ID: 00000000-0000-0000-0000-000000000000
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### Bash

```bash
$ vonage auth check --local
$ Global credentials found at: .vonagerc
$ API Key: abcd1234
$ API Secret: abc**************
$ App ID: 00000000-0000-0000-0000-000000000000
$ Private Key: Is Set
$ ✅ Checking API Key Secret
$ ✅ Checking App ID and Private Key
```

### Show Authentication

Display your current authentication configuration. This follows the configuration loading mentioned [above](https://developer.vonage.com/en/getting-started/tools/cli#configuration) and lets you know which configuration file the CLI is using.

> Note: This command will also check the credentials are correct.

**Flags**:

*   `--show-all`: Show non-redacted private key and API secret.
*   `--yaml`: Output in YAML format.
*   `--json`: Output in JSON format.

**Examples**:

Show the configuration

### Powershell (Windows)

```powershell
vonage auth show

Global credentials found at: C:\Users\bob\.vonage\config.json

API Key: your-api-key
API Secret: your-**************
App ID: your-application-id
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### CMD (Windows)

```cmd
vonage auth show

Global credentials found at: C:\Users\bob\.vonage\config.json

API Key: your-api-key
API Secret: your-**************
App ID: your-application-id
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### Bash

```bash
$ vonage auth show
$ Global credentials found at: /Users/bob/.vonage/config.json
$ API Key: your-api-key
$ API Secret: your-**************
$ App ID: your-application-id
$ Private Key: Is Set
$ ✅ Checking API Key Secret
$ ✅ Checking App ID and Private Key
```

## Using the CLI

### Viewing Available Commands

Commands are grouped by product or action. To view a list of available commands, just run `vonage` without any arguments:

### Powershell (Windows)

```powershell
vonage 

vonage 

Commands:
  vonage apps [command]           Manage applications
  vonage auth [command]           Manage authentication information
  vonage balance                  Check your account balance
  vonage conversations [command]  Manage conversations
  vonage jwt             Manage JWT tokens
  vonage members [command]        Manage applications
  vonage numbers [command]        Manage numbers
  vonage users [command]          Manage users

Options:
      --version   Show version number                                                                                                                                                  [boolean]
  -v, --verbose   Print more information                                                                                                                                               [boolean]
  -d, --debug     Print debug information                                                                                                                                              [boolean]
      --no-color  Toggle color output off                                                                                                                                              [boolean]
  -h, --help      Show help                                                                                                                                                            [boolean]
```

### CMD (Windows)

```cmd
vonage 

vonage 

Commands:
  vonage apps [command]           Manage applications
  vonage auth [command]           Manage authentication information
  vonage balance                  Check your account balance
  vonage conversations [command]  Manage conversations
  vonage jwt             Manage JWT tokens
  vonage members [command]        Manage applications
  vonage numbers [command]        Manage numbers
  vonage users [command]          Manage users

Options:
      --version   Show version number                                                                                                                                                  [boolean]
  -v, --verbose   Print more information                                                                                                                                               [boolean]
  -d, --debug     Print debug information                                                                                                                                              [boolean]
      --no-color  Toggle color output off                                                                                                                                              [boolean]
  -h, --help      Show help                                                                                                                                                            [boolean]
```

### Bash

```bash
$ vonage
$ vonage 
$ Commands:
$ vonage apps [command] Manage applications
$ vonage auth [command] Manage authentication information
$ vonage balance Check your account balance
$ vonage conversations [command] Manage conversations
$ vonage jwt  Manage JWT tokens
$ vonage members [command] Manage applications
$ vonage numbers [command] Manage numbers
$ vonage users [command] Manage users
$ Options:
$ --version Show version number [boolean]
$ -v, --verbose Print more information [boolean]
$ -d, --debug Print debug information [boolean]
$ --no-color Toggle color output off [boolean]
$ -h, --help Show help [boolean]
```

## Need Help?

If you encounter any issues or need help, please join our [community Slack channel](https://developer.vonage.com/en/community/slack).
      
    