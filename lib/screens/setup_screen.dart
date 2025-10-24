import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/sms_provider.dart';
import 'home_screen.dart';

class SetupScreen extends ConsumerStatefulWidget {
  const SetupScreen({super.key});

  @override
  ConsumerState<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends ConsumerState<SetupScreen> {
  bool _isProcessing = false;
  String _statusMessage = '';

  Future<void> _setupDefaultSms() async {
    setState(() {
      _isProcessing = true;
      _statusMessage = 'Checking SMS app status...';
    });

    final actions = ref.read(smsActionsProvider);
    
    // Request default SMS role - the plugin will handle if already default
    setState(() {
      _statusMessage = 'Setting up SMS app...';
    });
    
    await actions.requestDefaultSmsRole();

    // Give system a moment to process
    await Future.delayed(const Duration(milliseconds: 800));
    
    // Move straight to permissions - we need them regardless
    setState(() {
      _statusMessage = 'SMS app configured! Requesting permissions...';
    });
    
    await Future.delayed(const Duration(milliseconds: 400));
    await _requestPermissions();
  }

  Future<void> _requestPermissions() async {
    setState(() {
      _statusMessage = 'Requesting SMS permissions...';
    });

    final actions = ref.read(smsActionsProvider);
    final success = await actions.requestPermissions();

    // Initialize APN settings for MMS
    setState(() {
      _statusMessage = 'Configuring MMS settings...';
    });
    
    await Future.delayed(const Duration(milliseconds: 500));
    
    // This will configure APN for MMS
    await actions.initializeApnSettings();

    setState(() {
      _statusMessage = 'Setup complete!';
    });
    
    // Force refresh providers
    ref.invalidate(isDefaultSmsAppProvider);
    ref.invalidate(hasPermissionsProvider);
    
    // Wait a moment
    await Future.delayed(const Duration(milliseconds: 800));
    
    if (mounted) {
      setState(() {
        _isProcessing = false;
      });
      
      // Navigate directly to home screen
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Icon(
                Icons.message_rounded,
                size: 120,
                color: theme.colorScheme.primary,
              ),
              const SizedBox(height: 32),
              Text(
                'SMS App Setup',
                style: theme.textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              Text(
                'To send and receive SMS messages, this app needs to be set as your default SMS app.',
                style: theme.textTheme.bodyLarge,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 48),
              if (_isProcessing)
                Column(
                  children: [
                    const CircularProgressIndicator(),
                    const SizedBox(height: 16),
                    Text(
                      _statusMessage,
                      style: theme.textTheme.bodyMedium,
                      textAlign: TextAlign.center,
                    ),
                  ],
                )
              else ...[
                FilledButton.icon(
                  onPressed: _setupDefaultSms,
                  icon: const Icon(Icons.check_circle),
                  label: const Text('Set as Default SMS App'),
                  style: FilledButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                ),
                const SizedBox(height: 16),
                if (_statusMessage.isNotEmpty)
                  Text(
                    _statusMessage,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: _statusMessage.contains('Failed')
                          ? theme.colorScheme.error
                          : theme.colorScheme.primary,
                    ),
                    textAlign: TextAlign.center,
                  ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
