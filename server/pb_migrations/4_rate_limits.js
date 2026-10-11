/// <reference path="../pb_data/types.d.ts" />

// Rate limits per IP; behind the Cloudflare Tunnel the real IP is in CF-Connecting-IP.
// The general limit is wide: one sync lists records and fetches a file token per photo.
migrate((app) => {
  const s = app.settings()
  s.trustedProxy.headers = ["CF-Connecting-IP"]
  s.rateLimits.enabled = true
  s.rateLimits.rules = [
    { label: "*:auth", duration: 60, maxRequests: 20 },
    { label: "/api/", duration: 10, maxRequests: 300 },
  ]
  app.save(s)
}, (app) => {
  const s = app.settings()
  s.rateLimits.enabled = false
  app.save(s)
})
