import 'package:flutter_test/flutter_test.dart';

import 'package:mobile_flutter/src/app.dart';

void main() {
  testWidgets('app renders discovery title', (WidgetTester tester) async {
    await tester.pumpWidget(const RecSystemApp());

    expect(find.text('Movie Discovery'), findsOneWidget);
  });
}
