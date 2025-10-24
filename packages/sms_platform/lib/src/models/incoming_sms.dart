class IncomingSms {
  final String address;
  final String body;
  final int timestamp;
  final bool isMms;

  IncomingSms({
    required this.address,
    required this.body,
    required this.timestamp,
    required this.isMms,
  });

  factory IncomingSms.fromJson(Map<String, dynamic> json) {
    return IncomingSms(
      address: json['address'] as String,
      body: json['body'] as String,
      timestamp: json['timestamp'] as int,
      isMms: json['isMms'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'address': address,
      'body': body,
      'timestamp': timestamp,
      'isMms': isMms,
    };
  }
}

