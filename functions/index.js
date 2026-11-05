const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getDatabase } = require("firebase-admin/database");

initializeApp({
  databaseURL: "https://locationfinder-3df35-default-rtdb.firebaseio.com",
});

const OWNER_EMAIL = "mdasifsorkar64@gmail.com";

function assertOwner(request) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "প্রথমে Owner অ্যাকাউন্টে লগইন করুন।");
  }
  const email = String(request.auth.token.email || "").trim().toLowerCase();
  if (email !== OWNER_EMAIL || request.auth.token.email_verified !== true) {
    throw new HttpsError("permission-denied", "এই ড্যাশবোর্ড ব্যবহারের অনুমতি নেই।");
  }
}

async function writeAudit(ownerUid, action, targetUid = null, details = {}) {
  await getDatabase().ref("ownerAudit").push({
    ownerUid,
    action,
    targetUid,
    details,
    createdAt: Date.now(),
  });
}

exports.getOwnerOverview = onCall({ enforceAppCheck: false }, async (request) => {
  assertOwner(request);

  const auth = getAuth();
  const users = [];
  let pageToken;
  do {
    const page = await auth.listUsers(1000, pageToken);
    for (const user of page.users) {
      users.push({
        uid: user.uid,
        displayName: user.displayName || user.email || "নাম দেওয়া হয়নি",
        email: user.email || "",
        emailVerified: user.emailVerified,
        disabled: user.disabled,
        createdAt: user.metadata.creationTime || null,
        lastSignInAt: user.metadata.lastSignInTime || null,
      });
    }
    pageToken = page.pageToken;
  } while (pageToken);

  const db = getDatabase();
  const [invitesSnapshot, sessionsSnapshot] = await Promise.all([
    db.ref("invites").get(),
    db.ref("sessions").get(),
  ]);

  let pendingInvitations = 0;
  invitesSnapshot.forEach((child) => {
    if (child.child("status").val() === "pending") pendingInvitations += 1;
  });

  let activeConsentSessions = 0;
  sessionsSnapshot.forEach((child) => {
    const value = child.val() || {};
    if (value.active === true && value.stopped !== true &&
        value.consentA === true && value.consentB === true &&
        value.userA && value.userB) {
      activeConsentSessions += 1;
    }
  });

  await writeAudit(request.auth.uid, "dashboard_viewed");
  return {
    ownerEmail: OWNER_EMAIL,
    totalUsers: users.length,
    pendingInvitations,
    activeConsentSessions,
    users,
  };
});

exports.setUserDisabled = onCall({ enforceAppCheck: false }, async (request) => {
  assertOwner(request);
  const targetUid = String(request.data?.targetUid || "").trim();
  const disabled = request.data?.disabled;
  if (!targetUid || typeof disabled !== "boolean") {
    throw new HttpsError("invalid-argument", "ব্যবহারকারী ও ব্লক/আনব্লক অবস্থা দিন।");
  }

  const auth = getAuth();
  const target = await auth.getUser(targetUid);
  if ((target.email || "").toLowerCase() === OWNER_EMAIL) {
    throw new HttpsError("failed-precondition", "Owner অ্যাকাউন্ট ব্লক করা যাবে না।");
  }

  await auth.updateUser(targetUid, { disabled });
  await writeAudit(request.auth.uid, disabled ? "user_blocked" : "user_unblocked", targetUid);
  return { success: true, disabled };
});
