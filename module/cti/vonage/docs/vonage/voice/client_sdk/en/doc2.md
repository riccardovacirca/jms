# Overview

In this guide you learn how to add the Client SDK to your client-side JavaScript app.

## Prerequisites

The Client SDK requires [Node.js](https://nodejs.org) and [NPM](https://www.npmjs.com/).

## To add the Client SDK to your project

### Navigate to your app

Open your terminal. If you have an existing app, navigate to its root. Otherwise, create a new directory for your project.

### Add the Client SDK to your project

### Using npm

```javascript
import { VonageClient } from "@vonage/client-sdk";
```

```html
<script src="./node_modules/@vonage/client-sdk/dist/vonageClientSDK.min.js"></script>
```

```bash
$ npm init -y
```

```bash
$ npm install @vonage/client-sdk -s
```

### Using CDN

```html
<!-- ******* Load vonageClientSDK from a CDN ****** -->
<script src="https://cdn.jsdelivr.net/npm/@vonage/client-sdk@latest/dist/vonageClientSDK.min.js"></script>
```

```javascript
//********* Get a reference to vonageClientSDK **********
const vonageClientSDK = window.vonageClientSDK;
```

## Using the Client SDK in your app

### Creating Users and JWTs

A JSON Web Token (JWT) is necessary to log in to your Vonage Application. The Client SDK cannot manage users nor generate JWTs, so you must choose a method of handling it on the backend:

*   For onboarding or testing purposes, you can get your client-side app working before setting up a backend by [generating a test JWT from the command line](https://developer.vonage.com/en/vonage-client-sdk/create-your-application) and hard-coding it in your client-side JavaScript.
    
*   For real world usage, you can deliver JWTs from the server using the Node or PHP [backend SDKs](https://developer.vonage.com/en/tools), and set the `jwt` variable in your code by fetching that data:
    
    ```javascript
    fetch("/getJwt")
      .then(results => results.json())
      .then(data => {
        jwt = data.token;
      })
      .catch(err => console.error(err));
    ```
    
*   Read more on generating JWTs [in this article](https://developer.vonage.com/en/getting-started/concepts/authentication#json-web-tokens)
    

### Instantiate VonageClient and create a session

Instantiating the `VonageClient` varies on how you load the Vonage Client SDK.

If loaded with a `<script>` tag:

```javascript
const client = new vonageClientSDK.VonageClient();
```

If loaded via `import`:

```javascript
const client = new VonageClient();
```

To create a session, pass your JWT as the argument to `createSession()`.

```javascript
client.createSession(jwt)
  .then(sessionId => {
    console.log("Id of created session: ", sessionId);
  })
  .catch(error => {
    console.error("Error creating session: ", error);
  });
```

### Session Status

If there are any errors with the session after it has been successfully created, you will receive them on the `sessionError` event listener on the instantiated `VonageClient`.

```javascript
// After creating a session
client.on("sessionError", (reason) => {
  console.error("Session error reason: ", reason);
});
```

## Conclusion

You added the Client SDK to your client-side JavaScript app, and created a session. You can now use the `VonageClient` client in your app, and use the Client SDK functionality.

## See also

*   [Data Center Configuration](https://developer.vonage.com/en/vonage-client-sdk/configure-data-center) - this is an advanced optional configuration you can carry out after adding the SDK to your application.