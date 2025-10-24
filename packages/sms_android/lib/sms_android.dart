import 'package:flutter/services.dart';
import 'package:sms_platform/sms_platform.dart';

class SmsAndroid extends SmsPlatform {
  static const MethodChannel _channel = MethodChannel('sms_android');
  static const EventChannel _eventChannel = EventChannel('sms_android/incoming');

  static void registerWith() {
    SmsPlatform.instance = SmsAndroid();
  }

  @override
  Future<bool> isDefaultSmsApp() async {
    try {
      final bool? result = await _channel.invokeMethod('isDefaultSmsApp');
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  @override
  Future<bool> requestDefaultSmsRole() async {
    try {
      final bool? result = await _channel.invokeMethod('requestDefaultSmsRole');
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  @override
  Future<bool> initializeApnSettings() async {
    try {
      final bool? result = await _channel.invokeMethod('initializeApnSettings');
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  @override
  Future<bool> sendText({
    required String address,
    required String body,
  }) async {
    try {
      final bool? result = await _channel.invokeMethod('sendText', {
        'address': address,
        'body': body,
      });
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  @override
  Future<bool> sendMms({
    required List<String> addresses,
    String? text,
    List<MmsAttachment>? attachments,
  }) async {
    try {
      final bool? result = await _channel.invokeMethod('sendMms', {
        'addresses': addresses,
        'text': text,
        'attachments': attachments?.map((MmsAttachment a) => a.toJson()).toList(),
      });
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  @override
  Future<List<SmsThread>> listThreads({int limit = 50}) async {
    try {
      final List<dynamic>? result = await _channel.invokeMethod('listThreads', {
        'limit': limit,
      });
      if (result == null) return [];
      return result
          .map((item) => SmsThread.fromJson(Map<String, dynamic>.from(item)))
          .toList();
    } catch (e) {
      return [];
    }
  }

  @override
  Future<List<SmsMessage>> listMessages({
    required int threadId,
    int limit = 100,
  }) async {
    try {
      final List<dynamic>? result = await _channel.invokeMethod('listMessages', {
        'threadId': threadId,
        'limit': limit,
      });
      if (result == null) return [];
      return result
          .map((item) => SmsMessage.fromJson(Map<String, dynamic>.from(item)))
          .toList();
    } catch (e) {
      return [];
    }
  }

  @override
  Stream<IncomingSms> watchIncoming() {
    return _eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) =>
            IncomingSms.fromJson(Map<String, dynamic>.from(event)));
  }

  @override
  Future<bool> requestPermissions() async {
    try {
      final bool? result = await _channel.invokeMethod('requestPermissions');
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  @override
  Future<bool> hasPermissions() async {
    try {
      final bool? result = await _channel.invokeMethod('hasPermissions');
      return result ?? false;
    } catch (e) {
      return false;
    }
  }
}

