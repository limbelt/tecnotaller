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

function validClient(uid) {
  return {
    userId: uid,
    name: "Carlos Méndez",
    phone: "+52 55 1234 5678",
    address: "Av. Reforma 220",
    email: "carlos@ejemplo.com",
    notes: "Cliente frecuente",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validPart(uid) {
  return {
    userId: uid,
    sku: "SCR-IP14-01",
    name: "Pantalla OLED iPhone 14",
    category: "PANTALLA",
    compatibility: "iPhone 14",
    stock: 5,
    minStock: 2,
    costPrice: 1200.0,
    salePrice: 2100.0,
    supplier: "TechParts",
    location: "Gaveta A3",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validOrder(uid, status = "RECIBIDO") {
  return {
    userId: uid,
    orderCode: "ORD-1001",
    clientId: "client_1",
    clientName: "Carlos Méndez",
    clientPhone: "+52 55 1234 5678",
    clientAddress: "Av. Reforma 220",
    serviceType: "CELULAR",
    deviceTitle: "iPhone 14 Pro",
    deviceSerial: "356789012345678",
    issueDescription: "Pantalla estrellada tras caída",
    diagnosis: "",
    status,
    laborCost: 500.0,
    partsTotal: 0.0,
    usedParts: [],
    notes: "",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/clients`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/parts`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/orders`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/clients/c1`).set(validClient(ALICE_UID)));
});

test("Owner can create, read, update, and delete clients", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.doc(`users/${ALICE_UID}/clients/client_1`);
  await assertSucceeds(ref.set(validClient(ALICE_UID)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/clients`).get());
  await assertSucceeds(ref.update({ phone: "+52 55 9999 0000", updatedAt: new Date() }));
  await assertSucceeds(ref.delete());
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's PII/orders/parts", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`users/${ALICE_UID}/clients/c1`).set(validClient(ALICE_UID));
    await db.doc(`users/${ALICE_UID}/parts/p1`).set(validPart(ALICE_UID));
    await db.doc(`users/${ALICE_UID}/orders/o1`).set(validOrder(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/clients/c1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/clients`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/parts/p1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/orders/o1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/orders/o1`).update({ status: "CANCELADO", updatedAt: new Date() }));
});

test("Shadow field and invalid enum/negative stock writes are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/parts/p_ghost`).set({
      ...validPart(ALICE_UID),
      isAdminOverride: true,
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/parts/p_neg`).set({
      ...validPart(ALICE_UID),
      stock: -1,
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/orders/o_bad_type`).set({
      ...validOrder(ALICE_UID),
      serviceType: "TELEVISOR",
    })
  );
});

test("Order status progression and terminal state locking works", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.doc(`users/${ALICE_UID}/orders/o1`);
  await assertSucceeds(orderRef.set(validOrder(ALICE_UID, "RECIBIDO")));
  await assertSucceeds(orderRef.update({ status: "EN_REPARACION", updatedAt: new Date() }));
  await assertSucceeds(orderRef.update({ status: "ENTREGADO", updatedAt: new Date() }));
  // Terminal state locking: once ENTREGADO, subsequent updates must be rejected
  await assertFails(orderRef.update({ status: "RECIBIDO", updatedAt: new Date() }));
  await assertFails(orderRef.update({ laborCost: 9999.0, updatedAt: new Date() }));
});

test("Order accepts bounded usedParts array and parts stock update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const partRef = aliceDb.doc(`users/${ALICE_UID}/parts/p1`);
  const orderRef = aliceDb.doc(`users/${ALICE_UID}/orders/o1`);
  await assertSucceeds(partRef.set(validPart(ALICE_UID)));
  await assertSucceeds(orderRef.set(validOrder(ALICE_UID)));
  await assertSucceeds(partRef.update({ stock: 4, updatedAt: new Date() }));
  await assertSucceeds(
    orderRef.update({
      usedParts: [{ partId: "p1", partName: "Pantalla OLED iPhone 14", quantity: 1, unitPrice: 2100.0 }],
      partsTotal: 2100.0,
      updatedAt: new Date(),
    })
  );
});
