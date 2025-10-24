import 'dart:io';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sms_platform/sms_platform.dart';
import 'package:image_picker/image_picker.dart';
import '../providers/sms_provider.dart';
import '../utils/date_formatter.dart';
import 'package:flutter/services.dart';

class ThreadScreen extends ConsumerStatefulWidget {
  final int threadId;
  final String address;

  const ThreadScreen({
    super.key,
    required this.threadId,
    required this.address,
  });

  @override
  ConsumerState<ThreadScreen> createState() => _ThreadScreenState();
}

class _ThreadScreenState extends ConsumerState<ThreadScreen> {
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final ImagePicker _imagePicker = ImagePicker();
  final List<XFile> _selectedImages = [];
  bool _isSending = false;

  @override
  void initState() {
    super.initState();
    // Listen for incoming messages and refresh this thread
    ref.listenManual(incomingSmsStreamProvider, (previous, next) {
      next.whenData((incomingSms) {
        if (incomingSms.address == widget.address) {
          ref.read(smsActionsProvider).refreshMessages(widget.threadId);
          _scrollToBottom();
        }
      });
    });
  }

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollToBottom() {
    if (_scrollController.hasClients) {
      Future.delayed(const Duration(milliseconds: 100), () {
        if (_scrollController.hasClients) {
          _scrollController.animateTo(
            _scrollController.position.maxScrollExtent,
            duration: const Duration(milliseconds: 300),
            curve: Curves.easeOut,
          );
        }
      });
    }
  }

  Future<void> _pickImage() async {
    try {
      final image = await _imagePicker.pickImage(source: ImageSource.gallery);
      if (image != null) {
        setState(() {
          _selectedImages.add(image);
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error picking image: $e')),
        );
      }
    }
  }

  Future<void> _sendMessage() async {
    final text = _messageController.text.trim();
    if ((text.isEmpty && _selectedImages.isEmpty) || _isSending) return;

    setState(() => _isSending = true);

    final actions = ref.read(smsActionsProvider);
    bool success;

    // Send MMS if there are attachments, otherwise send SMS
    if (_selectedImages.isNotEmpty) {
      // Convert XFile to MmsAttachment
      final List<MmsAttachment> attachments = _selectedImages.map((file) {
        return MmsAttachment(
          filePath: file.path,
          contentType: 'image/jpeg',
        );
      }).toList();

      success = await actions.sendMms(
        addresses: [widget.address],
        text: text.isEmpty ? null : text,
        attachments: attachments,
      );
    } else {
      success = await actions.sendText(widget.address, text);
    }

    if (success) {
      // Longer delay for MMS to allow image parts to be written to database
      final hadAttachments = _selectedImages.isNotEmpty;
      
      setState(() {
        _messageController.clear();
        _selectedImages.clear();
        _isSending = false;
      });
      
      // Refresh messages after sending
      // MMS needs 4s delay to allow library + manual status update (3s) to complete
      final delayMs = hadAttachments ? 4000 : 500;
      await Future.delayed(Duration(milliseconds: delayMs));
      ref.read(smsActionsProvider).refreshMessages(widget.threadId);
      _scrollToBottom();
    } else {
      setState(() => _isSending = false);
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Failed to send message'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final messages = ref.watch(threadMessagesProvider(widget.threadId));
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(widget.address),
            Text(
              'SMS',
              style: theme.textTheme.bodySmall,
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.invalidate(threadMessagesProvider(widget.threadId));
            },
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: messages.when(
              data: (messageList) {
                if (messageList.isEmpty) {
                  return Center(
                    child: Text(
                      'No messages yet',
                      style: theme.textTheme.bodyLarge?.copyWith(
                        color: theme.colorScheme.outline,
                      ),
                    ),
                  );
                }

                // Reverse for chat-like display (newest at bottom)
                final reversedMessages = messageList.reversed.toList();
                
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  _scrollToBottom();
                });

                return ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.all(16),
                  itemCount: reversedMessages.length,
                  itemBuilder: (context, index) {
                    final message = reversedMessages[index];
                    return _MessageBubble(message: message);
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, stack) => Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      Icons.error_outline,
                      size: 64,
                      color: theme.colorScheme.error,
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'Error loading messages',
                      style: theme.textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    TextButton(
                      onPressed: () {
                        ref.invalidate(threadMessagesProvider(widget.threadId));
                      },
                      child: const Text('Retry'),
                    ),
                  ],
                ),
              ),
            ),
          ),
          _MessageInput(
            controller: _messageController,
            isSending: _isSending,
            selectedImages: _selectedImages,
            onSend: _sendMessage,
            onPickImage: _pickImage,
            onRemoveImage: (index) {
              setState(() {
                _selectedImages.removeAt(index);
              });
            },
          ),
        ],
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  final SmsMessage message;

  const _MessageBubble({required this.message});

  List<Widget> _buildMessageContent(BuildContext context, ThemeData theme, bool isSent) {
    final widgets = <Widget>[];
    
    if (message.body != null && message.body!.isNotEmpty) {
      // Check if body contains image URIs
      print('📝 Message body: ${message.body}');
      final imageRegex = RegExp(r'\[IMAGE:(content://[^\]]+)\]');
      final matches = imageRegex.allMatches(message.body!);
      print('🖼️ Found ${matches.length} image tags in message');
      
      if (matches.isNotEmpty) {
        // Body contains images - parse and display them
        String remainingText = message.body!;
        
        for (final match in matches) {
          // Add text before image
          final textBefore = remainingText.substring(0, match.start).trim();
          if (textBefore.isNotEmpty) {
            widgets.add(Text(
              textBefore,
              style: theme.textTheme.bodyLarge?.copyWith(
                color: isSent
                    ? theme.colorScheme.onPrimaryContainer
                    : theme.colorScheme.onSurface,
              ),
            ));
            widgets.add(const SizedBox(height: 8));
          }
          
          // Add image
          final imageUri = match.group(1)!;
          widgets.add(
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: ConstrainedBox(
                constraints: const BoxConstraints(
                  maxWidth: 250,
                  maxHeight: 300,
                  minHeight: 150,
                ),
                child: Image(
                  image: _MmsContentProvider(Uri.parse(imageUri)),
                  fit: BoxFit.contain,
                  errorBuilder: (context, error, stackTrace) {
                    print('❌ Image error: $error');
                    return Container(
                      width: 200,
                      height: 150,
                      decoration: BoxDecoration(
                        color: Colors.grey.shade300,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.broken_image, size: 48, color: Colors.red),
                          SizedBox(height: 8),
                          Text('Image failed to load', style: TextStyle(fontSize: 12)),
                        ],
                      ),
                    );
                  },
                ),
              ),
            ),
          );
          widgets.add(const SizedBox(height: 8));
          
          remainingText = remainingText.substring(match.end);
        }
        
        // Add remaining text
        final finalText = remainingText.trim();
        if (finalText.isNotEmpty) {
          widgets.add(Text(
            finalText,
            style: theme.textTheme.bodyLarge?.copyWith(
              color: isSent
                  ? theme.colorScheme.onPrimaryContainer
                  : theme.colorScheme.onSurface,
            ),
          ));
        }
      } else {
        // No images, just text
        widgets.add(Text(
          message.body!,
          style: theme.textTheme.bodyLarge?.copyWith(
            color: isSent
                ? theme.colorScheme.onPrimaryContainer
                : theme.colorScheme.onSurface,
          ),
        ));
      }
    }
    
    return widgets;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isSent = message.type == 2;
    
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment:
            isSent ? MainAxisAlignment.end : MainAxisAlignment.start,
        children: [
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: isSent
                    ? theme.colorScheme.primaryContainer
                    : theme.colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ..._buildMessageContent(context, theme, isSent),
                  const SizedBox(height: 4),
                  Text(
                    DateFormatter.formatMessageTime(message.date),
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: isSent
                          ? theme.colorScheme.onPrimaryContainer.withOpacity(0.7)
                          : theme.colorScheme.onSurface.withOpacity(0.6),
                      fontSize: 11,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MessageInput extends StatelessWidget {
  final TextEditingController controller;
  final bool isSending;
  final List<XFile> selectedImages;
  final VoidCallback onSend;
  final VoidCallback onPickImage;
  final Function(int) onRemoveImage;

  const _MessageInput({
    required this.controller,
    required this.isSending,
    required this.selectedImages,
    required this.onSend,
    required this.onPickImage,
    required this.onRemoveImage,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return Container(
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 8,
        bottom: MediaQuery.of(context).padding.bottom + 8,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Image preview
          if (selectedImages.isNotEmpty)
            Container(
              height: 80,
              margin: const EdgeInsets.only(bottom: 8),
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: selectedImages.length,
                itemBuilder: (context, index) {
                  return Stack(
                    children: [
                      Container(
                        width: 80,
                        height: 80,
                        margin: const EdgeInsets.only(right: 8),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(8),
                          color: theme.colorScheme.surfaceContainerHighest,
                        ),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: Image.file(
                            File(selectedImages[index].path),
                            fit: BoxFit.cover,
                            errorBuilder: (context, error, stackTrace) => const Icon(
                              Icons.broken_image,
                              size: 40,
                              color: Colors.red,
                            ),
                          ),
                        ),
                      ),
                      Positioned(
                        top: 0,
                        right: 4,
                        child: IconButton(
                          icon: const Icon(Icons.close, size: 18),
                          onPressed: () => onRemoveImage(index),
                          style: IconButton.styleFrom(
                            backgroundColor: Colors.black54,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.all(4),
                            minimumSize: const Size(24, 24),
                          ),
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
          Row(
            children: [
              IconButton(
                onPressed: isSending ? null : onPickImage,
                icon: const Icon(Icons.attach_file),
                tooltip: 'Attach image',
              ),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: controller,
                  decoration: InputDecoration(
                    hintText: 'Message',
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(24),
                      borderSide: BorderSide.none,
                    ),
                    filled: true,
                    fillColor: theme.colorScheme.surfaceContainerHighest,
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 12,
                    ),
                  ),
                  maxLines: null,
                  textCapitalization: TextCapitalization.sentences,
                  onSubmitted: (_) => onSend(),
                ),
              ),
              const SizedBox(width: 8),
              IconButton.filled(
                onPressed: isSending ? null : onSend,
                icon: isSending
                    ? SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: theme.colorScheme.onPrimary,
                        ),
                      )
                    : const Icon(Icons.send),
                style: IconButton.styleFrom(
                  backgroundColor: theme.colorScheme.primary,
                  foregroundColor: theme.colorScheme.onPrimary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

// Custom ImageProvider to load MMS images from content:// URIs
class _MmsContentProvider extends ImageProvider<_MmsContentProvider> {
  final Uri uri;

  _MmsContentProvider(this.uri);

  @override
  Future<_MmsContentProvider> obtainKey(ImageConfiguration configuration) {
    return SynchronousFuture<_MmsContentProvider>(this);
  }

  @override
  ImageStreamCompleter loadImage(_MmsContentProvider key, ImageDecoderCallback decode) {
    return MultiFrameImageStreamCompleter(
      codec: _loadAsync(key, decode),
      scale: 1.0,
    );
  }

  Future<ui.Codec> _loadAsync(_MmsContentProvider key, ImageDecoderCallback decode) async {
    try {
      print('🖼️ Loading MMS image from: ${key.uri}');
      
      // Use MethodChannel to read content:// URI from native side
      const platform = MethodChannel('sms_android');
      final bytes = await platform.invokeMethod('readContentUri', {'uri': key.uri.toString()});
      
      if (bytes == null) {
        print('❌ Failed to load image - bytes are null');
        throw Exception('Failed to load image from ${key.uri}');
      }
      
      print('✅ Loaded ${bytes.length} bytes for image');
      
      final buffer = await ui.ImmutableBuffer.fromUint8List(Uint8List.fromList(bytes.cast<int>()));
      final codec = await decode(buffer);
      
      print('✅ Image decoded successfully');
      return codec;
    } catch (e) {
      print('❌ Error loading MMS image: $e');
      throw Exception('Error loading MMS image: $e');
    }
  }

  @override
  bool operator ==(Object other) {
    if (other.runtimeType != runtimeType) return false;
    return other is _MmsContentProvider && other.uri == uri;
  }

  @override
  int get hashCode => uri.hashCode;
}
