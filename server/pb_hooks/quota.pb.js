/// <reference path="../pb_data/types.d.ts" />

// Storage is bounded per account: nobody can fill the Raspberry's disk with puzzles.
// (PocketBase runs each handler in its own scope: a constant outside it is not visible inside.)
onRecordCreateRequest((e) => {
  const max = 50
  const count = $app.countRecords("puzzles", $dbx.hashExp({ owner: e.record.get("owner") }))
  if (count >= max) throw new BadRequestError("limit of " + max + " puzzles per account")
  e.next()
}, "puzzles")
