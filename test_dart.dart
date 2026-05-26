void main() {
  var tests = [
    "0xFF10B981",
    "",
    "null",
    "NaN",
    "undefined",
    " 123",
    "123 ",
    "xFF"
  ];
  for (var t in tests) {
    try {
      int.parse(t);
      print("int.parse('$t') = OK");
    } catch(e) {
      print("int.parse('$t') => $e");
    }
    try {
      double.parse(t);
    } catch(e) {
      print("double.parse('$t') => $e");
    }
  }
}
