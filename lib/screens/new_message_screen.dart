import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:sms_platform/sms_platform.dart';
import '../providers/sms_provider.dart';
import 'dart:io';

class NewMessageScreen extends ConsumerStatefulWidget {
  const NewMessageScreen({super.key});

  @override
  ConsumerState<NewMessageScreen> createState() => _NewMessageScreenState();
}

class _NewMessageScreenState extends ConsumerState<NewMessageScreen> {
  final TextEditingController _recipientController = TextEditingController();
  final TextEditingController _messageController = TextEditingController();
  final List<String> _recipients = [];
  final List<XFile> _attachments = [];
  bool _isSending = false;

  @override
  void dispose() {
    _recipientController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  void _addRecipient() {
    final recipient = _recipientController.text.trim();
    if (recipient.isNotEmpty && !_recipients.contains(recipient)) {
      setState(() {
        _recipients.add(recipient);
        _recipientController.clear();
      });
    }
  }

  void _removeRecipient(String recipient) {
    setState(() {
      _recipients.remove(recipient);
    });
  }

  Future<void> _pickImage() async {
    final ImagePicker picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);
    
    if (image != null) {
      setState(() {
        _attachments.add(image);
      });
    }
  }

  void _removeAttachment(XFile file) {
    setState(() {
      _attachments.remove(file);
    });
  }

  Future<void> _sendMessage() async {
    if (_recipients.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please add at least one recipient')),
      );
      return;
    }

    final text = _messageController.text.trim();
    if (text.isEmpty && _attachments.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a message or add an attachment')),
      );
      return;
    }

    setState(() => _isSending = true);

    final actions = ref.read(smsActionsProvider);
    bool success;

    if (_attachments.isEmpty) {
      // Send SMS
      if (_recipients.length == 1) {
        success = await actions.sendText(_recipients.first, text);
      } else {
        // For multiple recipients without attachments, send individual SMS
        success = true;
        for (final recipient in _recipients) {
          final result = await actions.sendText(recipient, text);
          if (!result) success = false;
        }
      }
    } else {
      // Send MMS
      final mmsAttachments = _attachments.map((file) {
        return MmsAttachment(
          contentType: 'image/jpeg',
          filePath: file.path,
        );
      }).toList();

      success = await actions.sendMms(
        addresses: _recipients,
        text: text.isEmpty ? null : text,
        attachments: mmsAttachments,
      );
    }

    setState(() => _isSending = false);

    if (mounted) {
      if (success) {
        // Refresh thread list to show the new message
        print('📤 Message sent, refreshing thread list...');
        actions.refreshThreads();
        
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Message sent'),
            backgroundColor: Colors.green,
          ),
        );
        Navigator.pop(context);
      } else {
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
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('New Message'),
        actions: [
          if (!_isSending)
            TextButton(
              onPressed: _sendMessage,
              child: const Text('SEND'),
            )
          else
            const Padding(
              padding: EdgeInsets.all(16.0),
              child: SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2),
              ),
            ),
        ],
      ),
      body: Column(
        children: [
          // Recipients section
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              border: Border(
                bottom: BorderSide(
                  color: theme.colorScheme.outlineVariant,
                  width: 1,
                ),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      'To: ',
                      style: theme.textTheme.bodyLarge?.copyWith(
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    Expanded(
                      child: TextField(
                        controller: _recipientController,
                        decoration: const InputDecoration(
                          hintText: 'Enter phone number',
                          border: InputBorder.none,
                          isDense: true,
                        ),
                        keyboardType: TextInputType.phone,
                        onSubmitted: (_) => _addRecipient(),
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.add_circle),
                      onPressed: _addRecipient,
                      color: theme.colorScheme.primary,
                    ),
                  ],
                ),
                if (_recipients.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: _recipients.map((recipient) {
                      return Chip(
                        label: Text(recipient),
                        deleteIcon: const Icon(Icons.close, size: 18),
                        onDeleted: () => _removeRecipient(recipient),
                      );
                    }).toList(),
                  ),
                ],
              ],
            ),
          ),

          // Attachments section
          if (_attachments.isNotEmpty)
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                border: Border(
                  bottom: BorderSide(
                    color: theme.colorScheme.outlineVariant,
                    width: 1,
                  ),
                ),
              ),
              child: SizedBox(
                height: 100,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  itemCount: _attachments.length,
                  itemBuilder: (context, index) {
                    final attachment = _attachments[index];
                    return Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: Stack(
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: Image.file(
                              File(attachment.path),
                              width: 100,
                              height: 100,
                              fit: BoxFit.cover,
                            ),
                          ),
                          Positioned(
                            top: 4,
                            right: 4,
                            child: InkWell(
                              onTap: () => _removeAttachment(attachment),
                              child: Container(
                                padding: const EdgeInsets.all(4),
                                decoration: BoxDecoration(
                                  color: Colors.black54,
                                  shape: BoxShape.circle,
                                ),
                                child: const Icon(
                                  Icons.close,
                                  size: 16,
                                  color: Colors.white,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
            ),

          // Message input
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: TextField(
                controller: _messageController,
                decoration: InputDecoration(
                  hintText: 'Type a message',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                maxLines: null,
                expands: true,
                textAlignVertical: TextAlignVertical.top,
                textCapitalization: TextCapitalization.sentences,
              ),
            ),
          ),

          // Action bar
          Container(
            padding: EdgeInsets.only(
              left: 16,
              right: 16,
              top: 8,
              bottom: MediaQuery.of(context).padding.bottom + 8,
            ),
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
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.image),
                  onPressed: _isSending ? null : _pickImage,
                  tooltip: 'Add image',
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _attachments.isEmpty
                        ? 'Add attachments for MMS'
                        : '${_attachments.length} attachment(s)',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
