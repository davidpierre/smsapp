class MmsAttachment {
  final String? contentType;
  final String? filePath;
  final String? uri;

  MmsAttachment({
    this.contentType,
    this.filePath,
    this.uri,
  });

  factory MmsAttachment.fromJson(Map<String, dynamic> json) {
    return MmsAttachment(
      contentType: json['contentType'] as String?,
      filePath: json['filePath'] as String?,
      uri: json['uri'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'contentType': contentType,
      'filePath': filePath,
      'uri': uri,
    };
  }

  bool get isImage => contentType?.startsWith('image/') ?? false;
}

