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
    const result = assess(data,uid);
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
