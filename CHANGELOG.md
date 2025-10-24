# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-10-08

### Added
- Initial release of SMS/MMS app for Android
- Default SMS app capability using RoleManager API
- SMS send and receive functionality
- MMS send and receive functionality with image attachments
- Thread list view showing conversation history
- Individual thread/conversation view
- New message composer with multi-recipient support
- Image attachment picker for MMS
- Real-time incoming message notifications via EventChannel
- Material 3 UI design with light/dark theme support
- Setup flow for default SMS app and permissions
- Date/time formatting for messages
- Unread message indicators
- Pull-to-refresh on thread list
- Android 13-15 (API 26-35) support
- Riverpod state management
- Custom platform interface plugin architecture

### Technical Details
- **Platform**: Android only
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 26 (Android 8.0)
- **Flutter**: 3.3.0+
- **Kotlin**: 2.1.0
- **Gradle**: 8.7.3
- **State Management**: Riverpod 2.6.1

### Permissions Required
- SEND_SMS
- RECEIVE_SMS
- READ_SMS
- RECEIVE_MMS
- READ_PHONE_STATE
- READ_MEDIA_IMAGES
- INTERNET

### Known Limitations
- MMS download implementation is simplified (basic version)
- Group MMS conversations show individual threads
- No contact name resolution (shows phone numbers only)
- No message search functionality
- No message deletion capability
- No backup/restore functionality
- No encryption (relies on OS security)

### Security
- No external data storage
- No cloud sync
- No analytics or tracking
- All data stored in Android Telephony provider
- Suitable for enterprise deployment

### Deployment
- APK side-loading supported
- MDM/EMM deployment ready
- No Google Play Store required
- Enterprise certificate signing supported

---

## Future Considerations (Not in v1.0)

### Potential Future Enhancements
- Contact name integration
- Message search
- Message deletion
- Draft messages
- Scheduled sending
- Delivery reports
- Read receipts
- Rich MMS support (video, audio, contacts)
- Group conversation management
- Message backup/export
- Dark mode preference toggle
- Custom notification sounds
- Message templates
- Auto-reply functionality
- Blocked numbers list
- Conversation archiving
- Multi-SIM support
- RCS messaging support

### Known Issues to Address
- [ ] MMS download needs full implementation
- [ ] Contact resolution requires permission
- [ ] Large image compression for MMS
- [ ] Thread merging for group conversations
- [ ] Better error handling for network issues

---

## Version History

### [1.0.0] - 2025-10-08
- Initial release

---

## Migration Notes

### From Built-in SMS App
1. Install app via MDM
2. Set as default SMS app
3. All existing messages remain in system database
4. App reads existing conversation history
5. No data migration required

### Rollback to Previous SMS App
1. Open Android Settings
2. Apps → Default apps → SMS app
3. Select different SMS app
4. All messages remain accessible
5. Can uninstall this app safely

---

## Support

For issues, questions, or feature requests:
- Internal IT ticket system
- Email: support@company.com
- Documentation: See README.md

---

## License

Internal company use only. All rights reserved.

