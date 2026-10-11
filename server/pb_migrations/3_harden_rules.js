/// <reference path="../pb_data/types.d.ts" />

// Closes two gaps in the rules of 1_puzzleit.js:
// - an update could change `owner` and hand a record to someone else;
// - a scan or a progress photo could be attached to a puzzle that belongs to another user.
migrate((app) => {
  const own = "owner = @request.auth.id"
  const keepOwner = `${own} && @request.body.owner:isset = false`
  for (const name of ["puzzles", "scans", "progress_photos", "deletions"]) {
    const c = app.findCollectionByNameOrId(name)
    if (name !== "deletions") c.updateRule = keepOwner
    if (name === "scans" || name === "progress_photos") {
      c.createRule = "@request.auth.id != '' && @request.body.owner = @request.auth.id && @request.body.puzzle.owner = @request.auth.id"
    }
    app.save(c)
  }
}, (app) => {
  const own = "owner = @request.auth.id"
  for (const name of ["puzzles", "scans", "progress_photos"]) {
    const c = app.findCollectionByNameOrId(name)
    c.updateRule = own
    if (name !== "puzzles") c.createRule = "@request.auth.id != '' && @request.body.owner = @request.auth.id"
    app.save(c)
  }
})
