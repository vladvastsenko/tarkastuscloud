const {onCall,HttpsError} = require("firebase-functions/v2/https");
const {GoogleAuth} = require("google-auth-library");
const {assess,product} = require("./policy");
const auth = new GoogleAuth({scopes:["https://www.googleapis.com/auth/androidpublisher"]});
const base = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/fi.vladi.tarkastuscloud/purchases";
exports.verifyPremium = onCall({region:"europe-west1",maxInstances:5}, async request => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated","Sign in required");
  const token = request.data?.purchaseToken;
  if (typeof token !== "string" || token.length < 8 || token.length > 4096) throw new HttpsError("invalid-argument","Invalid token");
  try {
    const client = await auth.getClient();
    const {data} = await client.request({url:base+"/subscriptionsv2/tokens/"+encodeURIComponent(token),timeout:15000});
    const claim = (await getFirestore().collection("premiumRegistrationClaims").doc(hash(token)).get()).data();
    if (claim && claim.uid !== uid) throw new Error("purchase binding mismatch");
    const result = claim
      ? assess(data,uid,Date.now(),claim.sessionHash)
      : assess(data,uid);
    if (result.eligible && data.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
      await client.request({method:"POST",url:base+"/subscriptions/"+product+"/tokens/"+encodeURIComponent(token)+":acknowledge",data:{},timeout:15000});
    } else if (result.eligible && data.acknowledgementState !== "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED") {
      throw new Error("Unknown acknowledgement state");
    }
    return {...result,expiresAt:Math.min(result.expiresAt,Date.now()+5*60*1000)};
  } catch (_) {
    // Never expose purchase tokens or upstream credentials in errors.
    throw new HttpsError("failed-precondition","Subscription could not be verified");
  }
});

const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {issuePermit,hash} = require("./registration");
initializeApp();
exports.preparePremiumRegistration = onCall({region:"europe-west1",maxInstances:5,enforceAppCheck:true}, async request => {
  const {purchaseToken,session} = request.data || {};
  if (typeof purchaseToken !== "string" || purchaseToken.length < 8 || purchaseToken.length > 4096 ||
      typeof session !== "string" || !/^[a-f0-9]{64}$/.test(session))
    throw new HttpsError("invalid-argument","Invalid registration request");
  try {
    const client = await auth.getClient();
    const {data} = await client.request({url:base+"/subscriptionsv2/tokens/"+encodeURIComponent(purchaseToken),timeout:15000});
    const permit = await issuePermit(getFirestore(),purchaseToken,session,data);
    return {permit};
  } catch (_) {
    throw new HttpsError("failed-precondition","Registration permit unavailable");
  }
});

 // Deploy only after the paid registration flow is ready.
const {beforeUserCreated} = require("firebase-functions/v2/identity");
exports.blockDirectRegistration = beforeUserCreated({region:"europe-west1"}, () => {
  throw new HttpsError("permission-denied","Registration requires a verified purchase");
});

const {getAuth} = require("firebase-admin/auth");
const {reserveAccount,assertPurchase} = require("./registration");
exports.completePremiumEmailRegistration = onCall({region:"europe-west1",maxInstances:5,enforceAppCheck:true}, async request => {
  const {purchaseToken,session,permit,email,password} = request.data || {};
  if (typeof purchaseToken !== "string" || purchaseToken.length < 8 || purchaseToken.length > 4096 ||
      typeof email !== "string" || email.length > 254 || !email.includes("@") ||
      typeof password !== "string" || password.length < 8 || password.length > 128 ||
      typeof permit !== "string" || !/^[a-f0-9]{64}$/.test(permit))
    throw new HttpsError("invalid-argument","Invalid registration request");
  try {
    const db = getFirestore();
    const client = await auth.getClient();
    const {data} = await client.request({url:base+"/subscriptionsv2/tokens/"+encodeURIComponent(purchaseToken),timeout:15000});
    assertPurchase(data,session);
    const p = (await db.collection("premiumRegistrationPermits").doc(hash(permit)).get()).data();
    if (!p || p.claimId !== hash(purchaseToken)) throw new Error("purchase mismatch");
    const address = email.trim().toLowerCase();
    const uid = await reserveAccount(db,permit,session,address);
    const admin = getAuth();
    try {
      await admin.createUser({uid,email:address,password,emailVerified:false});
    } catch (e) {
      if (e.code !== "auth/uid-already-exists" && e.code !== "auth/email-already-exists") throw e;
      const existing = await admin.getUser(uid);
      if (existing.email?.toLowerCase() !== address) throw new Error("account mismatch");
    }
    if (data.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
      await client.request({method:"POST",url:base+"/subscriptions/"+product+"/tokens/"+encodeURIComponent(purchaseToken)+":acknowledge",data:{},timeout:15000});
    } else if (data.acknowledgementState !== "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED") throw new Error("unknown acknowledgement");
    return {created:true};
  } catch (_) {
    throw new HttpsError("failed-precondition","Registration could not be completed");
  }
});
