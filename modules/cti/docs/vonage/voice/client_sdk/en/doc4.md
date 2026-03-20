# Managing Sessions

Sessions are an integral part of using the Vonage Client SDKs. A Session is a live communication stream between the Client SDK and the Vonage servers.

## Creating a Session

To start making or receiving calls with the Vonage Client SDK you need to create a session. A prerequisite for creating a session is to have a User and a JWT for that User. You can learn more about Users and JWTs in the [Create your Application](https://developer.vonage.com/en/vonage-client-sdk/create-your-application#create-a-user) guide. Once you have a User and associated JWT you can call the `createSession` function on the SDK. The examples in this guide will be in JavaScript but the function names are the same across all 3 platforms.

```javascript
client.createSession(jwt)
 .then(sessionId => {
    console.log("Id of created session: ", sessionId);
 })
 .catch(error => {
    console.error("Error creating session: ", error);
 });
```

If successful, you will get a Session ID. This allows you to reconnect to this specific Session again if needed. Sessions have a TTL of 15 minutes.

### Reconnecting a Session

If you want to reconnect to an existing session, the `createSession` function optionally takes a Session ID as a parameter.

```javascript
client.createSession(jwt, existingSessionId)
 .then(sessionId => {
    console.log("Id of session: ", sessionId);
 })
 .catch(error => {
    console.error("Error: ", error);
 });
```

### Troubleshooting Session Creation

If you cannot create a Session successfully the error message returned will give you some insight into the issue. Here are some general troubleshooting steps

1.  Check the Vonage User exists by making a GET request to the [Users API](https://developer.vonage.com/en/api/application.v2#getUsers)
2.  If the User does exist, ensure that the User is in the same [region](https://developer.vonage.com/en/vonage-client-sdk/configure-data-center?source=vonage-client-sdk) that the Vonage Client SDK is attempting to connect to. The `user._links` object from the Users API call will have the User's region listed.
3.  Enter your JWT into [jwt.io](https://jwt.io) to ensure that the JWT has the correct username on the `sub` claim and is not expired. Here you can also check that the [ACL paths](https://developer.vonage.com/en/getting-started/concepts/authentication#access-control-list-acl) are correct and the object has the correct nesting.

## Refreshing a Session

If you currently have an active session and know the JWT used to create the session will be expiring soon, you can refresh the session with a new JWT.

```javascript
client.refreshSession(jwt)
 .then(() => {
    console.log("Session refreshed");
 })
 .catch(error => {
    console.error("Error refreshing session: ", error);
 });
```

## Deleting a Session

You can also delete a session. You would do this as part of a logout flow for example.

```javascript
client.deleteSession(jwt)
 .then(() => {
    console.log("Session deleted");
 })
 .catch(error => {
    console.error("Error deleting session: ", error);
 });
```

## Have Questions?

Should you have any further questions, issues or feedback, please contact us at `devrel@vonage.com` or the [Vonage Developer Community Slack](https://developer.vonage.com/en/community/slack).