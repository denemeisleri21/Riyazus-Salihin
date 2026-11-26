# Riyâzü’s-Sâlihîn Android Okuma Uygulaması

3 ciltlik *Riyâzü’s-Sâlihîn (Metin ve Çeviri — Diyanet İşleri Başkanlığı)* PDF metinlerinin tamamını (362 bab, 451 âyet-meal eşleştirmesi ve 1.903 hadis) ve kapak ikonunuzu içeren, tamamen çevrimdışı çalışan Android okuma uygulamasıdır.

## Android Studio Olmadan GitHub ile APK İndirme (Önerilen — ~2 Dakika)
Bilgisayarınıza Android Studio kurmanıza gerek kalmadan GitHub üzerinden ücretsiz olarak doğrudan `.apk` dosyasını oluşturabilirsiniz:

1. [github.com](https://github.com) hesabınızda yeni bir depo (**New repository**) oluşturun (ör. `riyazus-salihin-app`, **Public** veya **Private** olabilir).
2. Bu ZIP dosyasını bir klasöre çıkarın.
3. Klasörün **içindeki tüm dosya ve klasörleri** (`.github`, `app`, `gradle`, `build.gradle.kts`, `settings.gradle.kts` vb.) GitHub deponuza yükleyin (**Add file > Upload files** veya `git push` ile).
   > **Not:** Web arayüzünden yüklerken `.github` klasörü gizli görünürse önce normal dosyaları yükleyip ardından **Add file > Create new file** diyerek dosya adına `.github/workflows/build-apk.yml` yazıp bu klasördeki aynı dosyanın içeriğini yapıştırabilirsiniz.
4. Deponuzun üst menüsündeki **Actions** sekmesine tıklayın. **APK Oluştur (Riyâzü's-Sâlihîn)** iş akışı otomatik başlayacaktır (başlamazsa sağdan **Run workflow** düğmesine basın).
5. Yeşil tik (✓) göründüğünde çalıştırma kaydına tıklayın ve en alttaki **Artifacts** bölümünden **`RiyazusSalihin-APK`** dosyasını indirip içindeki `RiyazusSalihin.apk` dosyasını telefonunuza kurun.

## Özellikler
- **Tam Ekran (Immersive) Okuma:** Saat/şebeke çubuğu ve alttaki telefon tuşları gizlenir.
- **Ortaya Dokununca Menü Kontrolü:** Ekranın ortasına tek dokunuşla üst/alt uygulama menüsü açılıp kapanır.
- **Ayrı Bölüm/Âyet Sayfası ve Âyet-Meal Eşleşmesi:** Konu başlarındaki âyetler ve açıklamalar ayrı bir sayfa olarak açılır; her âyetin hemen ardından kendi meali gelir.
- **Kaldığı Yerden Devam:** Uygulamayı kapatıp açtığınızda tam olarak son okuduğunuz sayfa açılır.
- **3 Okuma Modu:** Dikey sürekli kaydırma, yatay sürekli kaydırma ve hadis bazlı sağa-sola sayfalama.
- **Dil, Yazı Boyutu ve Tema:** Arapça/Türkçe ayrı ayrı açılıp kapatılabilir, yazı boyutu (14–30 sp) ve tema (**Şamua**, **Beyaz**, **Gece**) ayarlanabilir.
