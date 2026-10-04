/// <reference path="../pb_data/types.d.ts" />

// Accounts are only created through an OAuth2 provider (Google): a direct POST to /api/collections/users/records (password sign-up) is refused.
// The local integration tests sign up by password: run them against a server that also has a temporary migration putting `createRule` back to "".
migrate((app) => {
  const users = app.findCollectionByNameOrId("users")
  users.createRule = "@request.context = 'oauth2'"
  app.save(users)
}, (app) => {
  const users = app.findCollectionByNameOrId("users")
  users.createRule = ""
  app.save(users)
})
