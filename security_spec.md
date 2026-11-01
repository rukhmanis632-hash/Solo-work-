# SoloWork Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Multi-User Isolation**: Every document across `/projects/{projectId}`, `/clients/{clientId}`, `/invoices/{invoiceId}`, `/tasks/{taskId}`, `/timeLogs/{logId}`, and `/userSettings/{userId}` belongs strictly to a single authenticated user (`userId == request.auth.uid`).
2. **Immutable Ownership & Creation Time**: `userId` and `createdAt` can never be mutated during an `update` operation (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
3. **Strict Key Allowlisting**: Every `create` requires all mandatory keys (`hasAll`) and forbids ghost fields (`hasOnly`). Every `update` validates `isValid[Entity](incoming())` and restricts mutated keys with `incoming().diff(existing()).affectedKeys().hasOnly(...)`.
4. **Query Enforcer**: Every `allow list` rule validates `resource.data.userId == request.auth.uid`, forcing client queries to include `.whereEqualTo("userId", uid)`.
5. **Bounded Strings & Integers**: Every string field enforces `.size() >= 1 && .size() <= MAX` and every numeric metric enforces non-negative range bounds.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("projects").get()` -> Reject (`PERMISSION_DENIED`)
2. **Cross-User Read**: Alice reading Bob's project `/projects/bob_proj` -> Reject (`PERMISSION_DENIED`)
3. **Unfiltered List Query**: Alice calling `db.collection("projects").get()` without `.where("userId", "==", aliceUid)` -> Reject (`PERMISSION_DENIED`)
4. **Identity Spoofing on Create**: Alice creating a project with `userId: "bob_456"` -> Reject (`PERMISSION_DENIED`)
5. **Shadow Update (Ghost Field)**: Alice updating a project with `isVerified: true` -> Reject (`PERMISSION_DENIED`)
6. **Immutable Field Mutation (`userId`)**: Alice updating `userId` on an existing task -> Reject (`PERMISSION_DENIED`)
7. **Immutable Field Mutation (`createdAt`)**: Alice updating `createdAt` on an existing invoice -> Reject (`PERMISSION_DENIED`)
8. **Resource Poisoning (Oversized String)**: Creating a task with a 500-char `title` (max 200) -> Reject (`PERMISSION_DENIED`)
9. **Enum Violation**: Creating an invoice with `status: "Forgiven"` (not in `['Paid', 'Pending', 'Overdue']`) -> Reject (`PERMISSION_DENIED`)
10. **Out-of-Bounds Progress**: Updating a project with `progress: 150` (max 100) -> Reject (`PERMISSION_DENIED`)
11. **PII Cross-User Access**: Bob attempting `get` on Alice's `/clients/alice_client` (contains `email`) -> Reject (`PERMISSION_DENIED`)
12. **UserSettings Path Mismatch**: Alice writing `/userSettings/bob_456` -> Reject (`PERMISSION_DENIED`)
