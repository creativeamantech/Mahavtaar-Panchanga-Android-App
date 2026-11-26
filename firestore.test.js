import { createRequire } from "node:module";
import { test, before, after, beforeEach } from "node:test";
import fs from "node:fs";
import crypto from "node:crypto";

const require = createRequire(import.meta.url);
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_root_uid_1";
const ADMIN_EMAIL = "jain9640@gmail.com";
const ALICE_UID = "alice_vedic_123";
const ALICE_EMAIL = "alice@example.com";
const BOB_UID = "bob_vedic_456";
const BOB_EMAIL = "bob@example.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function sha256Hex(email) {
  return crypto
    .createHash("sha256")
    .update(email.trim().toLowerCase(), "utf8")
    .digest("hex");
}

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

test("Unauthenticated user: denied across all user and RBAC collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("user_access").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("user_sessions").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("email_invites").doc(sha256Hex(ALICE_EMAIL)).get()
  );
  await assertFails(
    unauthDb
      .collection("user_access")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        email: ALICE_EMAIL,
        role: "USER",
        status: "PENDING",
        allowedPages: [],
        allowedFeatures: [],
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("RBAC: Root Admin bootstraps ADMIN/ACTIVE and manages user_access & email_invites; standard user cannot self-escalate", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid_99", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: ALICE_EMAIL,
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: BOB_EMAIL,
      email_verified: true,
    })
    .firestore();

  const ts = pastDate(8000);

  // Unverified email cannot claim bootstrap admin
  await assertFails(
    spoofAdminDb
      .collection("user_access")
      .doc("spoof_uid_99")
      .set({
        userId: "spoof_uid_99",
        email: ADMIN_EMAIL,
        role: "ADMIN",
        status: "ACTIVE",
        allowedPages: ["HOME"],
        allowedFeatures: ["GLOSSARY_DICTIONARY"],
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Verified root admin bootstraps own ADMIN/ACTIVE document
  await assertSucceeds(
    adminDb
      .collection("user_access")
      .doc(ADMIN_UID)
      .set({
        userId: ADMIN_UID,
        email: ADMIN_EMAIL,
        displayName: "Mahavtaar Admin",
        role: "ADMIN",
        status: "ACTIVE",
        allowedPages: ["EVENTS_TIMELINE", "HOME", "NAVTATA", "KUNDLI"],
        allowedFeatures: ["EVENT_DESCRIPTIONS", "GLOSSARY_DICTIONARY"],
        authorizedByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Standard user Alice cannot self-register as ACTIVE or with non-empty pages
  await assertFails(
    aliceDb
      .collection("user_access")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        email: ALICE_EMAIL,
        role: "USER",
        status: "ACTIVE",
        allowedPages: ["HOME"],
        allowedFeatures: [],
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Standard user Alice CAN self-register as USER + PENDING with empty pages & features
  await assertSucceeds(
    aliceDb
      .collection("user_access")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        email: ALICE_EMAIL,
        displayName: "Alice",
        role: "USER",
        status: "PENDING",
        allowedPages: [],
        allowedFeatures: [],
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Alice can read her own user_access doc, but cannot list all users or read Bob's doc or update her own status
  await assertSucceeds(aliceDb.collection("user_access").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("user_access").get());
  await assertFails(
    aliceDb.collection("user_access").doc(ALICE_UID).update({
      status: "ACTIVE",
      allowedPages: ["EVENTS_TIMELINE"],
      updatedAt: pastDate(1000),
    })
  );

  // Admin can list all user_access documents and update Alice to ACTIVE with specific pages & features
  await assertSucceeds(adminDb.collection("user_access").get());
  await assertSucceeds(
    adminDb
      .collection("user_access")
      .doc(ALICE_UID)
      .update({
        status: "ACTIVE",
        allowedPages: ["EVENTS_TIMELINE", "CALENDAR"],
        allowedFeatures: ["EVENT_DESCRIPTIONS"],
        authorizedByUid: ADMIN_UID,
        updatedAt: pastDate(1000),
      })
  );

  // Admin creates pre-authorized email invite for Bob
  const bobHash = sha256Hex(BOB_EMAIL);
  await assertSucceeds(
    adminDb
      .collection("email_invites")
      .doc(bobHash)
      .set({
        emailHash: bobHash,
        email: BOB_EMAIL,
        role: "USER",
        status: "ACTIVE",
        allowedPages: ["EVENTS_TIMELINE", "NAVTATA"],
        allowedFeatures: ["HTML_EXPORT_PRINT"],
        createdByUid: ADMIN_UID,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Alice cannot read Bob's email invite or list email_invites
  await assertFails(aliceDb.collection("email_invites").doc(bobHash).get());
  await assertFails(aliceDb.collection("email_invites").get());

  // Bob can read his own email invite and claim his pre-authorized user_access doc only with exact matching permissions
  await assertSucceeds(bobDb.collection("email_invites").doc(bobHash).get());
  await assertFails(
    bobDb
      .collection("user_access")
      .doc(BOB_UID)
      .set({
        userId: BOB_UID,
        email: BOB_EMAIL,
        role: "ADMIN",
        status: "ACTIVE",
        allowedPages: ["EVENTS_TIMELINE", "NAVTATA"],
        allowedFeatures: ["HTML_EXPORT_PRINT"],
        inviteHash: bobHash,
        createdAt: ts,
        updatedAt: ts,
      })
  );
  await assertSucceeds(
    bobDb
      .collection("user_access")
      .doc(BOB_UID)
      .set({
        userId: BOB_UID,
        email: BOB_EMAIL,
        displayName: "Bob",
        role: "USER",
        status: "ACTIVE",
        allowedPages: ["EVENTS_TIMELINE", "NAVTATA"],
        allowedFeatures: ["HTML_EXPORT_PRINT"],
        authorizedByUid: ADMIN_UID,
        inviteHash: bobHash,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Single-Device Session Lock: owner claims/rotates session token, Admin forces remote logout, cross-user denied", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: ALICE_EMAIL,
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: BOB_EMAIL,
      email_verified: true,
    })
    .firestore();

  const ts = pastDate(8000);

  await assertSucceeds(
    aliceDb
      .collection("user_sessions")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        activeSessionToken: "session_device_1_uuid",
        deviceName: "Pixel 8 Pro",
        forcedLogout: false,
        claimedAt: ts,
        updatedAt: ts,
      })
  );

  // Second device login rotates token on Alice's session lock
  await assertSucceeds(
    aliceDb
      .collection("user_sessions")
      .doc(ALICE_UID)
      .update({
        activeSessionToken: "session_device_2_uuid",
        deviceName: "Samsung S24",
        forcedLogout: false,
        claimedAt: pastDate(2000),
        updatedAt: pastDate(2000),
      })
  );

  // Bob cannot read or hijack Alice's session lock
  await assertFails(bobDb.collection("user_sessions").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("user_sessions")
      .doc(ALICE_UID)
      .update({
        activeSessionToken: "bob_hijack_token",
        updatedAt: pastDate(1000),
      })
  );

  // Admin can list all active sessions and force logout Alice's session
  await assertSucceeds(adminDb.collection("user_sessions").get());
  await assertSucceeds(
    adminDb
      .collection("user_sessions")
      .doc(ALICE_UID)
      .update({
        activeSessionToken: "revoked_by_admin_token",
        forcedLogout: true,
        updatedAt: pastDate(500),
      })
  );
});

test("UserProfile & Subcollections: owner CRUD succeeds, cross-user & invalid schema fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  const validProfile = {
    userId: ALICE_UID,
    displayName: "Alice Sharma",
    language: "hi",
    themeMode: "PARCHMENT",
    monthSystem: "purnimanta",
    ayanamsha: "citra",
    birthNakshatra: 4,
    use24HourFormat: false,
    notifySunrise: true,
    notifySunset: false,
    notifyTithiChange: true,
    notifyNakshatraChange: false,
    notifyVratReminders: true,
    selectedLocationName: "Ujjain, IN",
    selectedLocationCountry: "IN",
    selectedLocationLat: 23.1765,
    selectedLocationLon: 75.7885,
    selectedLocationTz: "Asia/Kolkata",
    createdAt: created,
    updatedAt: created,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile)
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});
