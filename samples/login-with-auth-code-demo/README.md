# Login via Uber without SDK - Demo App

This app demonstrates how a third party app can integrate Login via Uber to their app without using
the rides-android-sdk using proof-key code exchange,
RFC-7636 (https://datatracker.ietf.org/doc/html/rfc7636).

Here are the main components of the app -

- `AuthUriAssembler` - to assemble the a launch uri which would launch an Uber app (rides or eats)
  if installed or a browser app otherwise
- `PkceUtil` - Generates code challenge and code verifier pair
- `AuthService` - A retrofit service which sends request to token endpoint
- `AuthorizationCodeGrantFlow` - Sends an async request to token endpoint using AuthService
- `TokenRequestFlowCallback` - Callback to send back the tokens back to the client, if request is
  successful, or error otherwise
- `DemoActivity` - A basic android activity that contains a button to login using app link

The launch uri contains a code challenge query parameter (generated as part of code challenge and
code verifier pair, a.k.a pkce pair) along with other relevant query parameters (like `client_id`
, `redirect_uri`,`response_type` etc.). The launch uri is basically an applink on android which can
be handled in 3 ways -

1. When one of Uber app or Eats app is installed
   It will launch the specific app and show an authorization web page to the user to allow a third
   party app to use
   Uber's credentials to login. If the user grants permission, auth code is returned to the 3p app.
   If not, the SSO flow is canceled
2. When both Uber and Eats app are installed
   User will be shown a disambiguation dialogue to choose the app they want to use for logging in.
   Once app is chosen it's same as #1
3. When both apps are not installed
   Uber auth flow is launched in a custom tab, if available, or system browser. User completes the
   flow and auth code is returned to the 3P app

Then, we make a request to Uber's backend (token endpoint) with `client_id`, `grant_type`
, `redirect_uri`, `code_verifier` (generated as part of pkce pair) along with the
received `auth_code`. In successful response we would get the OAuth tokens (access token and refresh
token) which are saved in the app's private shared preferences; or failure exception is propagated
back to the caller.

## Should I use this same configuration pattern in my own apps?

We (the rides-android-sdk maintainers) have no strong opinion on this one way or another. The design
considerations are at the discretion of the app developer.
With this demo, we are merely presenting
a new way of authentication supported by Uber for third parties. Previously, we
supported `auth_code` flow with oauth secret and now, we added support for pkce flow as well which
does not require the third party backend to maintain the oauth secret.
