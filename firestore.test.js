const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowTimestamp() {
  return new Date();
}

function validProjectPayload(uid = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    name: "Web Redesign",
    clientName: "Orbit Studio",
    clientAvatarColor: "#6366F1",
    progress: 50,
    dueDate: "Apr 8, 2026",
    budget: 4800,
    status: "Active",
    category: "Web Design",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validClientPayload(uid = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    name: "Elena Rostova",
    company: "Orbit Studio",
    email: "elena@orbit.studio",
    status: "Active",
    totalBilled: 12450,
    projectsCount: 2,
    avatarColor: "#6366F1",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validInvoicePayload(uid = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    invoiceNumber: "INV-2026-101",
    clientName: "Orbit Studio",
    projectTitle: "Web Redesign",
    amountCents: 1245000,
    status: "Paid",
    issuedDate: "Apr 1, 2026",
    dueDate: "Apr 15, 2026",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validTaskPayload(uid = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    title: "Upcoming task checklist",
    projectName: "Web Redesign",
    status: "Todo",
    priority: "High",
    completed: false,
    dueDate: "Apr 8, 2026",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validTimeLogPayload(uid = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    projectName: "Web Redesign",
    taskDescription: "Design system tokens",
    durationMinutes: 120,
    hourlyRate: 95,
    loggedDate: "Apr 8, 2026",
    billable: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validSettingsPayload(uid = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    themeMode: "Dark",
    themePreset: "Classic Dark",
    accentColor: "Indigo",
    fontFamily: "Inter",
    customizeLayoutStyle: true,
    sidebarCompact: false,
    displayName: "Alex Miller",
    roleTitle: "Principal Freelancer",
    createdAt: ts,
    updatedAt: ts,
  };
}

// --- DIRTY DOZEN & SECURITY BOUNDARY TESTS ---

test("1. Unauthenticated user cannot read projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("projects").get());
});

test("2. Cross-user read is rejected on projects", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("projects").doc("bob_proj").set(validProjectPayload(BOB_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("projects").doc("bob_proj").get());
});

test("3. Unfiltered list query is rejected (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("projects").get());
});

test("4. Identity spoofing on create is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("projects").doc("spoof_proj").set(validProjectPayload(BOB_UID))
  );
});

test("5. Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validProjectPayload(ALICE_UID);
  await assertSucceeds(aliceDb.collection("projects").doc("p1").set(payload));
  await assertFails(
    aliceDb.collection("projects").doc("p1").update({
      progress: 80,
      isVerified: true,
      updatedAt: new Date(),
    })
  );
});

test("6. Immutable field mutation (userId) on update is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("t1").set(validTaskPayload(ALICE_UID)));
  await assertFails(
    aliceDb.collection("tasks").doc("t1").update({
      userId: BOB_UID,
      updatedAt: new Date(),
    })
  );
});

test("7. Enum violation on invoice status is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badInvoice = { ...validInvoicePayload(ALICE_UID), status: "Forgiven" };
  await assertFails(aliceDb.collection("invoices").doc("inv1").set(badInvoice));
});

test("8. Out-of-bounds project progress (>100) is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badProject = { ...validProjectPayload(ALICE_UID), progress: 150 };
  await assertFails(aliceDb.collection("projects").doc("p_bad").set(badProject));
});

test("9. PII cross-user read on clients is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("clients").doc("alice_c").set(validClientPayload(ALICE_UID));
  });
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("clients").doc("alice_c").get());
});

test("10. UserSettings path mismatch is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("userSettings").doc(BOB_UID).set(validSettingsPayload(ALICE_UID))
  );
});

// --- POSITIVE CRUD & CLIENT QUERY ALIGNMENT TESTS ---

test("11. Authenticated owner can create, query, and update all SoloWork collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(aliceDb.collection("projects").doc("p1").set(validProjectPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("clients").doc("c1").set(validClientPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("invoices").doc("i1").set(validInvoicePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("tasks").doc("t1").set(validTaskPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("timeLogs").doc("l1").set(validTimeLogPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("userSettings").doc(ALICE_UID).set(validSettingsPayload(ALICE_UID)));

  await assertSucceeds(aliceDb.collection("projects").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("clients").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("invoices").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("tasks").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("timeLogs").where("userId", "==", ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("tasks").doc("t1").update({
      completed: true,
      status: "Done",
      updatedAt: new Date(),
    })
  );
});
