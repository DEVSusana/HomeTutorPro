import unittest

from mcp_server import handle_request


class TestMcpServer(unittest.TestCase):
    def test_initialize_and_tools_list(self):
        init_resp = handle_request({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}})
        self.assertEqual(init_resp["id"], 1)
        self.assertIn("tools", init_resp["result"]["capabilities"])

        list_resp = handle_request({"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}})
        tool_names = [t["name"] for t in list_resp["result"]["tools"]]
        self.assertEqual(tool_names, ["check_hilt_bindings", "check_code_metrics", "list_specs"])

    def test_notification_returns_none(self):
        resp = handle_request({"jsonrpc": "2.0", "method": "notifications/initialized"})
        self.assertIsNone(resp)

    def test_tools_call_list_specs_and_unknown_tool(self):
        specs_resp = handle_request(
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {"name": "list_specs", "arguments": {}},
            }
        )
        self.assertFalse(specs_resp["result"]["isError"])
        self.assertTrue(len(specs_resp["result"]["content"][0]["text"]) > 0)

        err_resp = handle_request(
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {"name": "nonexistent_tool", "arguments": {}},
            }
        )
        self.assertTrue(err_resp["result"]["isError"])


if __name__ == "__main__":
    unittest.main()
