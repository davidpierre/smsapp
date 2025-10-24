import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'models/sms_message.dart';
import 'models/sms_thread.dart';
import 'models/incoming_sms.dart';
import 'models/mms_attachment.dart';

abstract class SmsPlatform extends PlatformInterface {
  SmsPlatform() : super(token: _token);

  static final Object _token = Object();
  static SmsPlatform? _instance;

  static SmsPlatform get instance => _instance!;

  static set instance(SmsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Check if this app is the default SMS app
  Future<bool> isDefaultSmsApp() {
    throw UnimplementedError('isDefaultSmsApp() has not been implemented.');
  }

  /// Request to become the default SMS app
  Future<bool> requestDefaultSmsRole() {
    throw UnimplementedError('requestDefaultSmsRole() has not been implemented.');
  }

  /// Initialize APN settings for MMS (shows selection dialog if needed)
  Future<bool> initializeApnSettings() {
    throw UnimplementedError('initializeApnSettings() has not been implemented.');
  }

  /// Send an SMS text message
  Future<bool> sendText({
    required String address,
    required String body,
  }) {
    throw UnimplementedError('sendText() has not been implemented.');
  }

  /// Send an MMS message with attachments
  Future<bool> sendMms({
    required List<String> addresses,
    String? text,
    List<MmsAttachment>? attachments,
  }) {
    throw UnimplementedError('sendMms() has not been implemented.');
  }

  /// List conversation threads
  Future<List<SmsThread>> listThreads({int limit = 50}) {
    throw UnimplementedError('listThreads() has not been implemented.');
  }

  /// List messages in a specific thread
  Future<List<SmsMessage>> listMessages({
    required int threadId,
    int limit = 100,
  }) {
    throw UnimplementedError('listMessages() has not been implemented.');
  }

  /// Watch for incoming SMS/MMS messages
  Stream<IncomingSms> watchIncoming() {
    throw UnimplementedError('watchIncoming() has not been implemented.');
  }

  /// Request SMS permissions
  Future<bool> requestPermissions() {
    throw UnimplementedError('requestPermissions() has not been implemented.');
  }

  /// Check if SMS permissions are granted
  Future<bool> hasPermissions() {
    throw UnimplementedError('hasPermissions() has not been implemented.');
  }
}

