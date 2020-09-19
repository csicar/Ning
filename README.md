
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

Build Vendor DB
---------------

```bash
go run createMacVendorDB.go
```

Roadmap
-------

- [ ] Scan History
    - [x] Save past scans in local db
    - [x] Use past scans for candidate selection on next scan
    - [ ] UI for scan history
- [ ] producer consumer architecture for scan
    - run `n` pings in parallel, start new ping when one ping is finished
- [ ] combine scanners into `UnifiedDeviceScanner`