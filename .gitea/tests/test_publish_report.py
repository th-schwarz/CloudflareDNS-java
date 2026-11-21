import os
import pathlib
import subprocess
import tempfile
import unittest


ACTION = pathlib.Path(__file__).resolve().parents[1] / "actions/publish-report/action.yml"


def generate_report(directory, outcome, xml=None):
    action = ACTION.read_text()
    script = action.split("        python3 - <<'EOF'\n", 1)[1].split("        EOF", 1)[0]
    script = "\n".join(line[8:] for line in script.splitlines())
    root = pathlib.Path(directory)
    (root / "reports").mkdir()
    (root / "junit-short").mkdir()
    if xml is not None:
        (root / "junit-short/test.xml").write_text(xml)
    env = dict(os.environ, BUILD_OUTCOME=outcome)
    subprocess.run(["python3", "-c", script], cwd=root, env=env, check=True, capture_output=True)
    return (root / "reports/test.md").read_text()


class PublishReportTest(unittest.TestCase):
    def test_missing_reports_are_not_successful(self):
        with tempfile.TemporaryDirectory() as directory:
            report = generate_report(directory, "failure")
            self.assertNotIn("All tests passed", report)
            self.assertIn("No JUnit", report)
            self.assertIn("failure", report)

    def test_failed_build_with_passing_tests_is_not_successful(self):
        with tempfile.TemporaryDirectory() as directory:
            report = generate_report(directory, "failure", '<testsuite name="example" tests="1" failures="0" errors="0" skipped="0"/>')
            self.assertNotIn("All tests passed", report)
            self.assertIn("failure", report)

    def test_passing_build_and_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            report = generate_report(directory, "success", '<testsuite name="example" tests="1" failures="0" errors="0" skipped="0"/>')
            self.assertIn("All tests passed", report)

    def test_all_skipped_is_not_successful(self):
        with tempfile.TemporaryDirectory() as directory:
            report = generate_report(directory, "success", '<testsuite name="example" tests="1" failures="0" errors="0" skipped="1"/>')
            self.assertNotIn("All tests passed", report)

    def test_failed_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            report = generate_report(directory, "failure", '<testsuite name="example" tests="1" failures="1" errors="0" skipped="0"/>')
            self.assertNotIn("All tests passed", report)
            self.assertIn("Some tests failed", report)


if __name__ == "__main__":
    unittest.main()
