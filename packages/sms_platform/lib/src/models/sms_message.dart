import 'mms_attachment.dart';

class SmsMessage {
  final int id;
  final int threadId;
  final String address;
  final String? body;
  final int date;
  final int type; // 1 = received, 2 = sent, 3 = draft, 4 = outbox, 5 = failed
  final bool isRead;
  final bool isMms;
  final List<MmsAttachment>? attachments;

  SmsMessage({
    required this.id,
    required this.threadId,
    required this.address,
    this.body,
    required this.date,
    required this.type,
    required this.isRead,
    required this.isMms,
    this.attachments,
  });

  factory SmsMessage.fromJson(Map<String, dynamic> json) {
    final body = json['body'] as String?;
    if ((json['isMms'] as bool) && body != null) {
      print('🔍 MMS Message - ID: ${json['id']}, body length: ${body.length}, body: $body');
    }
    return SmsMessage(
      id: json['id'] as int,
      threadId: json['threadId'] as int,
      address: json['address'] as String,
      body: body,
      date: json['date'] as int,
      type: json['type'] as int,
      isRead: json['isRead'] as bool,
      isMms: json['isMms'] as bool,
      attachments: json['attachments'] != null
          ? (json['attachments'] as List)
              .map((e) => MmsAttachment.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'threadId': threadId,
      'address': address,
      'body': body,
      'date': date,
      'type': type,
      'isRead': isRead,
      'isMms': isMms,
      'attachments': attachments?.map((e) => e.toJson()).toList(),
    };
  }

  bool get isSent => type == 2;
  bool get isReceived => type == 1;
}

