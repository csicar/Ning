
![Ning](fastlane/metadata/android/en-US/images/featureGraphic.png)

Scan local network for active devices.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/de.csicar.ning/)

![CI Status](https://gitlab.com/csicar/Ning/badges/master/pipeline.svg)

Features
--------
* Ping scan
* ARP scan
* TCP and UDP scan
* Network Service Discovery (Bonjour, Avahi)
* Vendor detection


<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1-network-overview.png"
     alt="Screenshot Network Overview"
     height="700">


<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2-device-detail.png"
     alt="Screenshot Device Detail"
     height="700">

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3-drawer.png"
     alt="Screenshot Drawer"
     height="700">

Roadmap
-------

- [ ] Scan History
    - [x] Save past scans in local db
    - [x] Use past scans for candidate selection on next scan
    - [ ] UI for scan history
- [ ] producer consumer architecture for scan
    - run `n` pings in parallel, start new ping when one ping is finished
- [ ] combine scanners into `UnifiedDeviceScanner`
- [ ] Low-Level mDNS Service Discovery


Contributing
------------

### Translate the App

If you want to translate the app in your language: great!

Translatable text is located in two places:

- All strings used in the app are located under https://github.com/csicar/Ning/tree/master/app/src/main/res

- The app description for F-Droid etc. are located under https://github.com/csicar/Ning/tree/master/fastlane/metadata/android

### Development in Android Studio

The project should be easy to import in Android Studio: Just clone the repository and import it.

### Build Vendor DB
```bash
go run createMacVendorDB.go
```