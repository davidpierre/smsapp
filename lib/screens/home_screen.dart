import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/sms_provider.dart';
import '../utils/date_formatter.dart';
import 'thread_screen.dart';
import 'new_message_screen.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  Widget build(BuildContext context) {
    // Listen for incoming messages and refresh thread list
    ref.listen(incomingSmsStreamProvider, (previous, next) {
      next.when(
        data: (incomingSms) async {
          print('📱 Received SMS from ${incomingSms.address}: ${incomingSms.body}');
          print('⏳ Waiting for system to save SMS...');
          // Wait a bit for the system to save the SMS to the database
          await Future.delayed(const Duration(milliseconds: 500));
          print('🔄 Refreshing thread list...');
          // Refresh thread list when new message arrives
          ref.read(smsActionsProvider).refreshThreads();
          print('✅ Thread list refresh triggered');
        },
        loading: () => print('⏳ Stream loading...'),
        error: (error, stack) => print('❌ Stream error: $error'),
      );
    });

    final threadList = ref.watch(threadListProvider);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Messages'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.invalidate(threadListProvider);
            },
          ),
        ],
      ),
      body: threadList.when(
        data: (threads) {
          if (threads.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.message_outlined,
                    size: 80,
                    color: theme.colorScheme.outline,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'No messages yet',
                    style: theme.textTheme.titleLarge?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Start a new conversation',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(threadListProvider);
            },
            child: ListView.builder(
              itemCount: threads.length,
              itemBuilder: (context, index) {
                final thread = threads[index];
                return _ThreadListItem(thread: thread);
              },
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stack) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.error_outline,
                size: 80,
                color: theme.colorScheme.error,
              ),
              const SizedBox(height: 16),
              Text(
                'Error loading messages',
                style: theme.textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              Text(
                error.toString(),
                style: theme.textTheme.bodySmall,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: () => ref.invalidate(threadListProvider),
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => const NewMessageScreen()),
          );
        },
        icon: const Icon(Icons.edit),
        label: const Text('New Message'),
      ),
    );
  }
}

class _ThreadListItem extends StatelessWidget {
  final dynamic thread;

  const _ThreadListItem({required this.thread});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: theme.colorScheme.primaryContainer,
        child: Icon(
          Icons.person,
          color: theme.colorScheme.onPrimaryContainer,
        ),
      ),
      title: Text(
        thread.address,
        style: TextStyle(
          fontWeight: thread.isRead ? FontWeight.normal : FontWeight.bold,
        ),
      ),
      subtitle: Text(
        thread.snippet ?? 'No messages',
        maxLines: 2,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontWeight: thread.isRead ? FontWeight.normal : FontWeight.w500,
        ),
      ),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            DateFormatter.formatThreadTime(thread.date),
            style: theme.textTheme.bodySmall?.copyWith(
              color: thread.isRead
                  ? theme.colorScheme.outline
                  : theme.colorScheme.primary,
              fontWeight: thread.isRead ? FontWeight.normal : FontWeight.bold,
            ),
          ),
          if (!thread.isRead) ...[
            const SizedBox(height: 4),
            Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                color: theme.colorScheme.primary,
                shape: BoxShape.circle,
              ),
            ),
          ],
        ],
      ),
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => ThreadScreen(
              threadId: thread.threadId,
              address: thread.address,
            ),
          ),
        );
      },
    );
  }
}
