import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sms_platform/sms_platform.dart';
import 'package:sms_android/sms_android.dart';

// Initialize the SMS platform
final smsPlatformProvider = Provider<SmsPlatform>((ref) {
  SmsAndroid.registerWith();
  return SmsPlatform.instance;
});

// Default SMS app status
final isDefaultSmsAppProvider = FutureProvider<bool>((ref) async {
  final platform = ref.watch(smsPlatformProvider);
  return await platform.isDefaultSmsApp();
});

// Permission status
final hasPermissionsProvider = FutureProvider<bool>((ref) async {
  final platform = ref.watch(smsPlatformProvider);
  return await platform.hasPermissions();
});

// Thread list provider
final threadListProvider = FutureProvider.autoDispose<List<SmsThread>>((ref) async {
  print('📋 threadListProvider: Fetching threads...');
  final platform = ref.watch(smsPlatformProvider);
  final threads = await platform.listThreads(limit: 100);
  print('📋 threadListProvider: Fetched ${threads.length} threads');
  return threads;
});

// Messages for a specific thread
final threadMessagesProvider = FutureProvider.autoDispose.family<List<SmsMessage>, int>(
  (ref, threadId) async {
    final platform = ref.watch(smsPlatformProvider);
    return await platform.listMessages(threadId: threadId, limit: 200);
  },
);

// Incoming SMS stream
final incomingSmsStreamProvider = StreamProvider<IncomingSms>((ref) {
  final platform = ref.watch(smsPlatformProvider);
  return platform.watchIncoming();
});

// Action providers
class SmsActions {
  final Ref ref;
  
  SmsActions(this.ref);

  Future<bool> requestDefaultSmsRole() async {
    final platform = ref.read(smsPlatformProvider);
    return await platform.requestDefaultSmsRole();
  }

  Future<bool> requestPermissions() async {
    final platform = ref.read(smsPlatformProvider);
    return await platform.requestPermissions();
  }

  Future<bool> initializeApnSettings() async {
    final platform = ref.read(smsPlatformProvider);
    return await platform.initializeApnSettings();
  }

  Future<bool> sendText(String address, String body) async {
    print('📤 Sending SMS to $address: $body');
    final platform = ref.read(smsPlatformProvider);
    final result = await platform.sendText(address: address, body: body);
    print(result ? '✅ SMS sent successfully' : '❌ SMS send failed');
    return result;
  }

  Future<bool> sendMms({
    required List<String> addresses,
    String? text,
    List<MmsAttachment>? attachments,
  }) async {
    final platform = ref.read(smsPlatformProvider);
    return await platform.sendMms(
      addresses: addresses,
      text: text,
      attachments: attachments,
    );
  }

  void refreshThreads() {
    print('🔄 SmsActions.refreshThreads() called - invalidating provider');
    ref.invalidate(threadListProvider);
    print('✅ threadListProvider invalidated');
  }

  void refreshMessages(int threadId) {
    ref.invalidate(threadMessagesProvider(threadId));
  }
}

final smsActionsProvider = Provider((ref) => SmsActions(ref));
