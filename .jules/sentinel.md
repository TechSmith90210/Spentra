## 2026-07-12 - [Unbounded Rate Limiting Map Memory Leak]
**Vulnerability:** In `RateLimitingFilter.java`, a `ConcurrentHashMap` stored token buckets for each client IP address without any mechanism for expiration, pruning, or size limitation. This allowed an attacker or scanner using multiple IPs to consume memory indefinitely, resulting in an OutOfMemoryError (OOM) and causing a Denial of Service (DoS) of the backend.
**Learning:** Rate-limiting structures that store per-IP/per-user state in-memory must be bound or periodically pruned. Standard `ConcurrentHashMap` does not evict inactive keys.
**Prevention:** Implement a scheduled eviction task (e.g., using `@Scheduled`) to remove inactive entries, or use self-expiring cache libraries (e.g., Caffeine, Guava) with set maximum size limits and time-to-idle/time-to-live expiration policies.

## 2026-07-14 - [Insecure Passwords Forcing Lack of Special Characters]
**Vulnerability:** In `SignupRequest.java`, the password and confirm password patterns were validated with the regex `^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$`. This strict regex prohibited any non-alphanumeric special characters, forcing users to choose weaker, easier-to-crack passwords.
**Learning:** Password complexity rules must not inadvertently reduce password entropy by excluding special characters. Enforcing length, letter, and digit requirements should securely allow special/non-alphanumeric characters.
**Prevention:** Use a pattern like `^(?=.*[A-Za-z])(?=.*\d).{8,}$` which allows special characters while still ensuring minimum length and character group presence.

## 2026-07-14 - [Unauthenticated NPE in Security Context Extraction]
**Vulnerability:** In `UserService.java`, calling `.getPrincipal()` on `SecurityContextHolder.getContext().getAuthentication()` during unauthenticated requests caused a `NullPointerException` (500 Internal Server Error) instead of failing securely with a proper HTTP 401 Unauthorized API response. This can leak stack traces or crash requests.
**Learning:** Any extraction of credentials/identities from security context in unauthenticated or optionally authenticated flows must check for a null `Authentication` object beforehand to fail securely.
**Prevention:** Always check if `SecurityContextHolder.getContext().getAuthentication()` is null before calling `.getPrincipal()`.
