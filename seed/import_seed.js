/**
 * Firestore'a başlangıç ürün ve müşteri verilerini yükler.
 *
 * Kullanım:
 *   1) Firebase konsolu > Proje Ayarları > Hizmet Hesapları > "Yeni Özel Anahtar Oluştur"
 *      ile indirdiğiniz JSON dosyasını bu klasöre "service-account.json" adıyla koyun.
 *   2) Bu klasörde:  npm install firebase-admin
 *   3) node import_seed.js
 *
 * Not: Script tekrar tekrar çalıştırılırsa mevcut kayıtları günceller (üzerine yazar),
 * kopya oluşturmaz.
 */
const admin = require('firebase-admin');
const serviceAccount = require('./service-account.json');
const products = require('./products_seed.json');
const customers = require('./customers_seed.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});
const db = admin.firestore();

function slugify(str) {
  return str
    .toString()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
}

async function main() {
  console.log(`Ürünler yükleniyor: ${products.length}`);
  const urunBatch = db.batch();
  products.forEach((p) => {
    const ref = db.collection('urunler').doc(p.id);
    urunBatch.set(ref, p, { merge: true });
  });
  await urunBatch.commit();

  console.log(`Müşteriler yükleniyor: ${customers.length}`);
  // Firestore batch limiti 500 yazma işlemidir, 1147 müşteri için parçalara bölüyoruz.
  const chunkSize = 400;
  for (let i = 0; i < customers.length; i += chunkSize) {
    const chunk = customers.slice(i, i + chunkSize);
    const batch = db.batch();
    chunk.forEach((c) => {
      const id = slugify(c.ad).slice(0, 120);
      const ref = db.collection('musteriler').doc(id);
      batch.set(ref, c, { merge: true });
    });
    await batch.commit();
    console.log(`  ${i + chunk.length}/${customers.length}`);
  }

  console.log('Tamamlandı.');
  process.exit(0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
