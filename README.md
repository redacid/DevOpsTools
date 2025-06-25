
https://docs.github.com/en/rest/releases/assets?apiVersion=2022-11-28

curl -L -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/mikefarah/yq/releases

curl -I -L -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/mikefarah/yq/releases
HTTP/2 200
date: Wed, 25 Jun 2025 07:16:19 GMT
content-type: application/json; charset=utf-8
cache-control: public, max-age=60, s-maxage=60
vary: Accept,Accept-Encoding, Accept, X-Requested-With
etag: W/"34b9f1f158d3ea6771fda85fc3004518e788826d1e37d88ae7ba63ed416a2dae"
x-github-media-type: github.v3; format=json
link: <https://api.github.com/repositories/43225113/releases?page=2>; rel="next", <https://api.github.com/repositories/43225113/releases?page=6>; rel="last"
x-github-api-version-selected: 2022-11-28
access-control-expose-headers: ETag, Link, Location, Retry-After, X-GitHub-OTP, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Used, X-RateLimit-Resource, X-RateLimit-Reset, X-OAuth-Scopes, X-Accepted-OAuth-Scopes, X-Poll-Interval, X-GitHub-Media-Type, X-GitHub-SSO, X-GitHub-Request-Id, Deprecation, Sunset
access-control-allow-origin: *
strict-transport-security: max-age=31536000; includeSubdomains; preload
x-frame-options: deny
x-content-type-options: nosniff
x-xss-protection: 0
referrer-policy: origin-when-cross-origin, strict-origin-when-cross-origin
content-security-policy: default-src 'none'
server: github.com
accept-ranges: bytes
x-ratelimit-limit: 60
x-ratelimit-remaining: 59
x-ratelimit-reset: 1750839378
x-ratelimit-resource: core
x-ratelimit-used: 1
x-github-request-id: EF88:1413CD:D6D2904:DCA9E49:685BA242


curl -L -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/jqlang/jq/releases