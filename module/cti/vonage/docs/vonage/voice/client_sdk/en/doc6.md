# Configure your Backend Server

When working with the Vonage Client SDK, your backend server will need to handle a few key components, mainly responding to webhooks from Vonage, creating [Users](https://developer.vonage.com/en/conversation/concepts/user) and generating [JWTs](https://jwt.io/) for those users. You can learn more about these different components and how they work together in the [Create your Application guide](https://developer.vonage.com/en/vonage-client-sdk/create-your-application).

To see a prebuilt implementation of the concepts discussed in the guides, you can edit or deploy the [Client SDK Sample Server template](https://developer.vonage.com/en/cloud-runtime/6b041470-0b95-4d90-bbd7-ac18946edd6a_client-sdk-voice-sample-server) with Code Hub.

This guide will be using Node.JS, the [Vonage JWT SDK](https://github.com/Vonage/vonage-node-sdk/blob/3.x/packages/jwt/README.md), and the [Vonage Server SDK](https://github.com/Vonage/vonage-node-sdk). But your Client SDK backend server can be written in any language. You can install the SDKs using NPM:

```text
npm install @vonage/jwt @vonage/server-sdk
```

## Creating Users

Now that you have the SDKs installed, you can to create Users:

```javascript
import { Vonage } from "@vonage/server-sdk";
import { tokenGenerate } from '@vonage/jwt';

const vonage = new Vonage(
    {
      applicationId: process.env.API_APPLICATION_ID,
      privateKey: process.env.PRIVATE_KEY
    }
);

async function createUser(username) {
    try {
        const userResponse = await vonage.users.createUser({
            name: username,
            displayName: username
        });
    } catch (e) {
        console.log(e);
    }
}
```

You can read more about the request parameters and responses for the Users entity in the [API specification](https://developer.vonage.com/en/api/application.v2#User).

## Generate JWTs for the Client SDK

With users created, you can now create a JWT for that user. This is the JWT the Client SDK is expecting when you call `createSession`. Creating a JWT for a user will require ACL paths and a `sub` claim. The ACL paths are used to restrict which endpoints the JWT can access.

> NOTE: ACLs control what actions the user can perform. You can alter the ACL paths to restrict permissions.

```javascript
function generateUserJWT(username) {
    const aclPaths =  {
        "/*/users/**": {},
        "/*/conversations/**": {},
        "/*/sessions/**": {},
        "/*/devices/**": {},
        "/*/image/**": {},
        "/*/media/**": {},
        "/*/push/**": {},
        "/*/knocking/**": {},
        "/*/legs/**": {}
    };

    return tokenGenerate(applicationId, privateKey, {
        subject: username,
        acl: aclPaths
    });
}
```

You can expose an endpoint on your server which will return this generated token to your Android, iOS and Web Client SDK applications:

```javascript
app.post('/getJWT', async (req, res) => {
    const username = req.body.username;
    try {
        // Perform authorization and validation
        res.json(
            {
                jwt: generateUserJWT(username)
            }
        );
    } catch(e) {
        console.log(e);
    }
});
```

## Handling Incoming Webhooks

When the `serverCall` function is used in a Android, iOS and Web Client SDK application, the URL that has been set as the "answer URL" on your Vonage Application will get an incoming request. You can learn more about the process in the ["Make a Call" guide](https://developer.vonage.com/en/vonage-client-sdk/in-app-voice/guides/make-call).

Your server should respond with a [Call Control Object](https://developer.vonage.com/en/voice/voice-api/ncco-reference) to control the flow of the call. For example to connect an incoming call to a user called Alice you would return a Call Control Object similar to the one below.

```json
[
  {
    "action": "connect",
    "from": "447700900000",
    "endpoint": [
      {
        "type": "app",
        "user": "Alice"
      }
    ]
  }
]
```

## Have Questions?

Should you have any further questions, issues or feedback, please contact us on `devrel@vonage.com` or the [Vonage Developer Community Slack](https://developer.vonage.com/en/community/slack).