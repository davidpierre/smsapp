class SmsThread {
  final int threadId;
  final String address;
  final String? snippet;
  final int messageCount;
  final int date;
  final bool isRead;

  SmsThread({
    required this.threadId,
    required this.address,
    this.snippet,
    required this.messageCount,
    required this.date,
    required this.isRead,
  });

  factory SmsThread.fromJson(Map<String, dynamic> json) {
    return SmsThread(
      threadId: json['threadId'] as int,
      address: json['address'] as String,
      snippet: json['snippet'] as String?,
      messageCount: json['messageCount'] as int,
      date: json['date'] as int,
      isRead: json['isRead'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'threadId': threadId,
      'address': address,
      'snippet': snippet,
      'messageCount': messageCount,
      'date': date,
      'isRead': isRead,
    };
  }
}

