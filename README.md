// Приклад використання
val logger = Logger.getInstance()

// Логування на різних рівнях
logger.d("TasksManager", "Завантаження завдань...")
logger.i("TasksManager", "Завдання успішно завантажені: ${tasks.size} елементів")
logger.w("TasksManager", "Деякі завдання не мають обов'язкових полів")

try {
// Якийсь код, що може викликати помилку
} catch (e: Exception) {
logger.e("TasksManager", "Помилка при завантаженні завдань", e)
}




https://docs.github.com/en/rest/releases/assets?apiVersion=2022-11-28

curl -L -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/mikefarah/yq/releases

curl -I -L -H "Accept: application/vnd.github+json" -H "Authorization: token " -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/mikefarah/yq/releases

HTTP/2 200
date: Wed, 25 Jun 2025 07:39:41 GMT
content-type: application/json; charset=utf-8
content-length: 3089291
cache-control: private, max-age=60, s-maxage=60
vary: Accept, Authorization, Cookie, X-GitHub-OTP,Accept-Encoding, Accept, X-Requested-With
etag: "5149c4aee9619a4ab210666746fc8555a33a55fc5a435cac65ed68fecc7889ee"
x-oauth-scopes: admin:enterprise, admin:gpg_key, admin:org, admin:org_hook, admin:public_key, admin:repo_hook, admin:ssh_signing_key, audit_log, codespace, copilot, delete:packages, delete_repo, gist, notifications, project, repo, user, workflow, write:discussion, write:network_configurations, write:packages
x-accepted-oauth-scopes: repo
x-github-media-type: github.v3; format=json
link: <https://api.github.com/repositories/43225113/releases?page=2>; rel="next", <https://api.github.com/repositories/43225113/releases?page=6>; rel="last"
x-github-api-version-selected: 2022-11-28
x-ratelimit-limit: 5000
x-ratelimit-remaining: 4999
x-ratelimit-reset: 1750840781
x-ratelimit-used: 1
x-ratelimit-resource: core
access-control-expose-headers: ETag, Link, Location, Retry-After, X-GitHub-OTP, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Used, X-RateLimit-Resource, X-RateLimit-Reset, X-OAuth-Scopes, X-Accepted-OAuth-Scopes, X-Poll-Interval, X-GitHub-Media-Type, X-GitHub-SSO, X-GitHub-Request-Id, Deprecation, Sunset
access-control-allow-origin: *
strict-transport-security: max-age=31536000; includeSubdomains; preload
x-frame-options: deny
x-content-type-options: nosniff
x-xss-protection: 0
referrer-policy: origin-when-cross-origin, strict-origin-when-cross-origin
content-security-policy: default-src 'none'
server: github.com
x-github-request-id: 9C9D:3CB572:A0A8500:A4E029C:685BA7BB


curl -I -L -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/mikefarah/yq/releases

HTTP/2 200
date: Wed, 25 Jun 2025 07:16:19 GMT
content-type: application/json; charset=utf-8
cache-control: public, max-age=60, s-maxage=60
vary: Accept,Accept-Encoding, Accept, X-Requested-With
etag: W/"34b9f1f158d3ea6771fda85fc3004518e788826d1e37d88ae7ba63ed416a2dae"
x-github-media-type: github.v3; format=json
link: <https://api.github.com/repositories/43225113/releases?page=2>; rel="next", 
<https://api.github.com/repositories/43225113/releases?page=6>; rel="last"
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

[2025-06-25 12:14:21.044] DEBUG GitHub API-GGR: Fetching releases page 1: https://api.github.com/repos/mikefarah/yq/releases
[2025-06-25 12:14:21.147] INFO GitHub API-GRI: Using GitHub token for API request to: https://api.github.com/repos/mikefarah/yq/releases
[2025-06-25 12:14:22.802] DEBUG GitHub API: Pagination links: next, last
[2025-06-25 12:14:22.803] INFO GitHub API: Rate limits: 4985 / 5000 requests remaining. Reset at 2025-06-25 12:40:44 (resource: core)
[2025-06-25 12:14:23.547] INFO GitHub API-GGR: Fetched 30 releases on page 1, total so far: 30
[2025-06-25 12:14:24.957] DEBUG GitHub API: Pagination links: next, last
[2025-06-25 12:14:24.957] INFO GitHub API: Rate limits: 41 / 60 requests remaining. Reset at 2025-06-25 12:31:27 (resource: core)
[2025-06-25 12:14:24.957] DEBUG GitHub API-GGR: Fetching releases page 2: https://api.github.com/repositories/43225113/releases?page=2
[2025-06-25 12:14:24.958] INFO GitHub API-GRI: Using GitHub token for API request to: https://api.github.com/repositories/43225113/releases?page=2
[2025-06-25 12:14:26.382] DEBUG GitHub API: Pagination links: prev, next, last, first
[2025-06-25 12:14:26.382] INFO GitHub API: Rate limits: 4984 / 5000 requests remaining. Reset at 2025-06-25 12:40:44 (resource: core)
[2025-06-25 12:14:26.912] INFO GitHub API-GGR: Fetched 30 releases on page 2, total so far: 60
[2025-06-25 12:14:28.019] DEBUG GitHub API: Pagination links: prev, next, last, first
[2025-06-25 12:14:28.020] INFO GitHub API: Rate limits: 40 / 60 requests remaining. Reset at 2025-06-25 12:31:27 (resource: core)
[2025-06-25 12:14:28.020] DEBUG GitHub API-GGR: Fetching releases page 3: https://api.github.com/repositories/43225113/releases?page=3
[2025-06-25 12:14:28.020] INFO GitHub API-GRI: Using GitHub token for API request to: https://api.github.com/repositories/43225113/releases?page=3
[2025-06-25 12:14:29.390] DEBUG GitHub API: Pagination links: prev, next, last, first
[2025-06-25 12:14:29.390] INFO GitHub API: Rate limits: 4983 / 5000 requests remaining. Reset at 2025-06-25 12:40:44 (resource: core)
[2025-06-25 12:14:29.901] INFO GitHub API-GGR: Fetched 30 releases on page 3, total so far: 90
[2025-06-25 12:14:30.691] DEBUG GitHub API: Pagination links: prev, next, last, first
[2025-06-25 12:14:30.691] INFO GitHub API: Rate limits: 39 / 60 requests remaining. Reset at 2025-06-25 12:31:27 (resource: core)
[2025-06-25 12:14:30.691] DEBUG GitHub API-GGR: Fetching releases page 4: https://api.github.com/repositories/43225113/releases?page=4
[2025-06-25 12:14:30.691] INFO GitHub API-GRI: Using GitHub token for API request to: https://api.github.com/repositories/43225113/releases?page=4
[2025-06-25 12:14:31.511] DEBUG GitHub API: Pagination links: prev, next, last, first
[2025-06-25 12:14:31.512] INFO GitHub API: Rate limits: 4982 / 5000 requests remaining. Reset at 2025-06-25 12:40:44 (resource: core)
[2025-06-25 12:14:31.888] INFO GitHub API-GGR: Fetched 30 releases on page 4, total so far: 120
[2025-06-25 12:14:32.731] DEBUG GitHub API: Pagination links: prev, next, last, first
[2025-06-25 12:14:32.731] INFO GitHub API: Rate limits: 38 / 60 requests remaining. Reset at 2025-06-25 12:31:27 (resource: core)
[2025-06-25 12:14:32.731] DEBUG GitHub API-GGR: Fetching releases page 5: https://api.github.com/repositories/43225113/releases?page=5
[2025-06-25 12:14:32.731] INFO GitHub API-GRI: Using GitHub token for API request to: https://api.github.com/repositories/43225113/releases?page=5
[2025-06-25 12:14:33.236] DEBUG GitHub API: Pagination links: prev, next, last, first
[2025-06-25 12:14:33.236] INFO GitHub API: Rate limits: 4981 / 5000 requests remaining. Reset at 2025-06-25 12:40:44 (resource: core)
[2025-06-25 12:14:33.518] INFO GitHub API-GGR: Fetched 30 releases on page 5, total so far: 150
[2025-06-25 12:14:33.975] DEBUG GitHub API: Pagination links: prev, next, last, first
[2025-06-25 12:14:33.975] INFO GitHub API: Rate limits: 37 / 60 requests remaining. Reset at 2025-06-25 12:31:27 (resource: core)
[2025-06-25 12:14:33.975] DEBUG GitHub API-GGR: Fetching releases page 6: https://api.github.com/repositories/43225113/releases?page=6
[2025-06-25 12:14:33.975] INFO GitHub API-GRI: Using GitHub token for API request to: https://api.github.com/repositories/43225113/releases?page=6
[2025-06-25 12:14:34.492] DEBUG GitHub API: Pagination links: prev, first
[2025-06-25 12:14:34.493] INFO GitHub API: Rate limits: 4980 / 5000 requests remaining. Reset at 2025-06-25 12:40:44 (resource: core)
[2025-06-25 12:14:34.544] INFO GitHub API-GGR: Fetched 3 releases on page 6, total so far: 153
[2025-06-25 12:14:34.801] DEBUG GitHub API: Pagination links: prev, first
[2025-06-25 12:14:34.802] INFO GitHub API: Rate limits: 36 / 60 requests remaining. Reset at 2025-06-25 12:31:27 (resource: core)
[2025-06-25 12:14:34.802] INFO GitHub API-GGR: Successfully fetched 153 releases from GitHub
[2025-06-25 12:14:34.803] INFO GitHub API-GRI: Using GitHub token for API request to: https://api.github.com/repos/mikefarah/yq/releases/tags/v4.45.4
[2025-06-25 12:14:35.290] INFO GitHub API: Rate limits: 4979 / 5000 requests remaining. Reset at 2025-06-25 12:40:44 (resource: core)