**Privacy Policy**

**Introduction**

This app ("LabLog") is an open-source project licensed under GPL-3.0-or-later. See the [LICENSE](https://github.com/jakobkreft/LabLog/blob/master/LICENSE) file for details. It respects user privacy and does not collect, share, or store any user data on external servers.

**Data Collection and Use**

LabLog App does not collect or store any personal data on external servers. All data, including photos taken with the camera, are stored locally on the user's device.

**Permissions Used**

1. **Camera Permission (`android.permission.CAMERA`)**: This permission is required to allow the user to take photos and save them locally as part of their entries or notes. The photos remain on the user's device and are not shared or uploaded anywhere, if MQTT feature is disabled (This feature is disabled by default).
   
2. **Internet Permission (`android.permission.INTERNET`)**: This permission is needed only if the user chooses to use the MQTT feature. If configured, the user can send entries data over MQTT to a server of their choice. The app does not send any data to external servers controlled by the app developers.

**User Control**

Users have full control over their data. The app does not upload, share, or store any data externally unless the user explicitly configures an MQTT server for entry sharing.

**Open Source**

This app is fully open-source, and its source code can be reviewed [here](https://github.com/jakobkreft/LabLog). Users can verify how permissions are used by inspecting the code.

**Contact**

If you have any questions or concerns about this privacy policy, please contact me via [github issues](https://github.com/jakobkreft/LabLog/issues/new/choose).
