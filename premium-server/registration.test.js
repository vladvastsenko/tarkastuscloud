const test = require("node:test");
const assert = require("node:assert/strict");
const {hash,secret,issuePermit,consumePermit} = require("./registration");
const {product} = require("./policy");
function database(){
 const rows=new Map();
 let queue=Promise.resolve();
 return {collection:name=>({doc:id=>({id,key:name+"/"+id})}),runTransaction(fn){
 const run=queue.then(async()=>{const next=new Map(rows);const result=await fn({
 get:async ref=>({exists:next.has(ref.key),data:()=>next.get(ref.key)}),
 create:(ref,data)=>{if(next.has(ref.key))throw Error("exists");next.set(ref.key,data);},
 update:(ref,data)=>next.set(ref.key,{...next.get(ref.key),...data})
 });for(const [k,v] of next)rows.set(k,v);return result;});queue=run.catch(()=>{});return run;
 }};
}
const purchase=(session,state="ACTIVE")=>({subscriptionState:"SUBSCRIPTION_STATE_"+state,externalAccountIdentifiers:{obfuscatedExternalAccountId:hash(session)},lineItems:[{productId:product,offerDetails:{basePlanId:"monthly"},autoRenewingPlan:{},expiryTime:new Date(1000000).toISOString()}]});
test("reject pending and mismatched purchases",async()=>{const db=database(),s=secret();await assert.rejects(issuePermit(db,"token",s,purchase(s,"PENDING"),1));await assert.rejects(issuePermit(db,"token",s,purchase(secret()),1));});
test("only one concurrent issue and consume succeeds",async()=>{const db=database(),s=secret();const issued=await Promise.allSettled([issuePermit(db,"token",s,purchase(s),1),issuePermit(db,"token",s,purchase(s),1)]);assert.equal(issued.filter(x=>x.status==="fulfilled").length,1);const p=issued.find(x=>x.status==="fulfilled").value;const used=await Promise.allSettled([consumePermit(db,p,s,"uid1",2),consumePermit(db,p,s,"uid2",2)]);assert.equal(used.filter(x=>x.status==="fulfilled").length,1);});
test("wrong session and expired permit denied",async()=>{const db=database(),s=secret();const p=await issuePermit(db,"token",s,purchase(s),1);await assert.rejects(consumePermit(db,p,secret(),"uid",2));await assert.rejects(consumePermit(db,p,s,"uid",600001));});
