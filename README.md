# OpenVK Legacy for Android

_[Русский](README_RU.md)_

Author: [Dmitry Tretyakov (Tinelix)](https://github.com/tretdm)

**OpenVK Legacy** is mobile client for retro devices running Android 2.1 Eclair and higher.\
_Powered by OpenVK API._

We will be happy to accept your bugreports [in our bug-tracker](https://github.com/openvk/mobile-android-legacy/projects/1).

![featureGraphic](fastlane/metadata/android/en-US/images/featureGraphic.png)

## Download APK
* **via F-Droid**
  * **[repo.openvk.uk](https://repo.openvk.uk/repo/)** (much faster)
  * [f-droid.org](https://f-droid.org/packages/uk.openvk.android.legacy/)
  * [izzysoft.de](https://apt.izzysoft.de/fdroid/index/apk/uk.openvk.android.legacy)
* **via [Telegram channel](https://t.me/+nPLHBZqAsFlhYmIy)**
* **via [Releases page](https://github.com/openvk/mobile-android-legacy/releases/latest)**
* **via [NashStore](https://store.nashstore.ru/store/637cc36cfb3ed38835524503)** _(for Russian phones kinda 😂)_
* **via [Trashbox](https://trashbox.ru/topics/164477/openvk-legacy)**

## Building
We recommend using [Android Studio 3.1.2](https://developer.android.com/studio/archive) and Java 7 for perfect support of libraries developed for Android 2.1 Eclair and above.

**ATTENTION!** After an `java.util.zip.ZipException: invalid entry compressed size (expected [m] but got [n] bytes)` error occurs in the `:[package_name]:mockableAndroidJar` task when using Android SDK Build-tools 28 and higher, be sure to clean the project.

## Used App Components
**Most compatible app components, including libraries, are guaranteed to work with Android 2.1 and above.**

You may also find them useful for developing applications that support very old Android versions, despite security and stability issues in current Android versions.

#### Libraries
1. **[Android Support Library v24 for 1.6+](https://developer.android.com/topic/libraries/support-library)** (Apache License 2.0)
2. **[HttpUrlWrapper](https://github.com/tinelix/httpurlwrapper)** (Apache License 2.0)
3. **[PhotoView 1.2.5](https://github.com/Baseflow/PhotoView/tree/v1.2.5)** (Apache License 2.0)
4. **[SlidingMenu with Android 10+ patch](https://github.com/tinelix/SlidingMenu)** (Apache License 2.0)
5. **[OkHttp 3.8.0](https://square.github.io/okhttp/)** (Apache License 2.0)
6. **[Twemojicon (Emojicon with Twemoji pack)](https://github.com/tinelix/twemojicon)** (Apache License 2.0)
7. **[Retro-ActionBar](https://github.com/tinelix/retro-actionbar)** (Apache License 2.0)
8. **[Retro-PopupMenu](https://github.com/tinelix/retro-popupmenu)** (Apache License 2.0)
9. **[SystemBarTint](https://github.com/jgilfelt/SystemBarTint)** (Apache License 2.0)
10. **[SwipeRefreshLayout Mod with Pull-to-Refresh](https://github.com/xyxyLiu/SwipeRefreshLayout)** (Apache License 2.0)
11. **[android-i18n-plurals](https://github.com/populov/android-i18n-plurals)** (X11 License)
12. **[Application Crash Reports 4.6.0](https://github.com/ACRA/acra/tree/acra-4.6.0)** (Apache License 2.0) \
    _About our usage of ACRA in application see [issue #153](https://github.com/openvk/mobile-android-legacy/issues/153)._
14. **[Universal Image Loader](https://github.com/nostra13/Android-Universal-Image-Loader/tree/v1.9.5)** (Apache License 2.0)
15. **[NineOldAndroids](https://github.com/JakeWharton/NineOldAndroids)** (Apache License 2.0)
16. **[Apmem FlowLayout 1.8](https://github.com/ApmeM/android-flowlayout/tree/java-flowlayout-1.8)** (Apache License 2.0)

#### Design
1. **VK 3.x original resources** \
   Author: [Gregory Klyushnikov](https://grishka.me)
2. **VK3-based themes:** Gray, Black
3. [**Holo Design Language**](https://web.archive.org/web/20130217132335/http://developer.android.com/design/index.html)

## OpenVK Legacy License
[GNU Affero GPL v3.0](COPYING) or later version.

## Links
[OpenVK API docs](https://docs.openvk.su/openvk_engine/en/api/description/)\
[OpenVK Mobile](https://openvk.uk/app)

<a href="https://codeberg.org/OpenVK/mobile-android-legacy">
    <img alt="Get it on Codeberg" src="https://codeberg.org/Codeberg/GetItOnCodeberg/media/branch/main/get-it-on-blue-on-white.png" height="60">
</a>
