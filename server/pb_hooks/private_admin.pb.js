/// <reference path="../pb_data/types.d.ts" />

// The admin API is not reachable from the internet. Cloudflare adds CF-Connecting-IP to every request that comes through the tunnel;
// those requests only reach the collections the app uses (each limited by its rules to the caller's own records). Admin work goes through
// the local port, with an SSH tunnel: `ssh -L 8090:127.0.0.1:8090 <host>` then http://localhost:8090/_/ (see README).
routerUse((e) => {
  if (e.request.header.get("CF-Connecting-IP") && $os.getenv("ADMIN_PUBLIC") !== "1") {
    const path = e.request.url.path
    // By name or by id (`_pb_users_auth_`): superusers answer to both.
    const open = ["users", "_pb_users_auth_", "puzzles", "scans", "progress_photos", "deletions"]
    const coll = path.startsWith("/api/collections") ? path.split("/")[3] || "" : null
    const adminApi = ["/api/settings", "/api/backups", "/api/logs", "/api/crons", "/api/batch"]
    if (adminApi.some((p) => path.startsWith(p)) || (coll !== null && !open.includes(coll))) {
      return e.json(404, { message: "Not found" })
    }
  }
  return e.next()
})
