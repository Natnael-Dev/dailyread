#!/usr/bin/env python3
"""
DailyRead Live Audit View Server (Localhost Only)
Serves a zero-dependency auto-refreshing web view of emulator-5554 for owner UI audit.
Strictly bound to 127.0.0.1.
"""

import http.server
import socketserver
import subprocess
import threading
import time

HOST = "127.0.0.1"
PORT = 8765

latest_frame = b""
frame_lock = threading.Lock()
frame_timestamp = 0.0

def capture_worker():
    global latest_frame, frame_timestamp
    while True:
        try:
            cmd = ["adb", "-s", "emulator-5554", "exec-out", "screencap", "-p"]
            proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=5)
            if proc.returncode == 0 and len(proc.stdout) > 1000:
                with frame_lock:
                    latest_frame = proc.stdout
                    frame_timestamp = time.time()
        except Exception:
            pass
        time.sleep(0.7)

HTML_PAGE = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DailyRead — Owner Live Audit View</title>
    <style>
        :root {
            --bg: #141715;
            --surface: #1E231F;
            --primary: #9AB093;
            --text: #EDE9DE;
            --muted: #A39E93;
            --accent: #4D6148;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background: var(--bg);
            color: var(--text);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 24px 16px;
        }
        header {
            text-align: center;
            margin-bottom: 20px;
        }
        .badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(154, 176, 147, 0.15);
            color: var(--primary);
            padding: 4px 12px;
            border-radius: 999px;
            font-size: 13px;
            font-weight: 600;
            margin-bottom: 8px;
            border: 1px solid rgba(154, 176, 147, 0.3);
        }
        .dot {
            width: 8px;
            height: 8px;
            background: #4ade80;
            border-radius: 50%;
            box-shadow: 0 0 8px #4ade80;
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.4; }
        }
        h1 {
            font-size: 24px;
            font-weight: 700;
            color: var(--text);
            letter-spacing: -0.01em;
        }
        .sub {
            color: var(--muted);
            font-size: 14px;
            margin-top: 4px;
        }
        .device-frame {
            background: #000;
            border-radius: 36px;
            padding: 12px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.6), 0 0 0 2px rgba(255,255,255,0.08);
            max-width: 440px;
            width: 100%;
        }
        .screen-container {
            border-radius: 26px;
            overflow: hidden;
            background: #000;
            aspect-ratio: 9 / 20;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        img#screen {
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: block;
        }
        footer {
            margin-top: 18px;
            font-size: 13px;
            color: var(--muted);
            display: flex;
            align-items: center;
            gap: 16px;
        }
        .stat {
            background: var(--surface);
            padding: 4px 10px;
            border-radius: 6px;
        }
    </style>
</head>
<body>
    <header>
        <div class="badge"><span class="dot"></span> LIVE MIRROR (127.0.0.1)</div>
        <h1>DailyRead UI Audit</h1>
        <p class="sub">Target: emulator-5554 (Pixel 6, Android 14 API 34) &bull; Phase 2</p>
    </header>

    <div class="device-frame">
        <div class="screen-container">
            <img id="screen" src="/screen.png" alt="Device Screen" />
        </div>
    </div>

    <footer>
        <span class="stat" id="fps">Streaming: ~1 fps</span>
        <span class="stat" id="time">Updated: Just now</span>
    </footer>

    <script>
        const img = document.getElementById('screen');
        const timeEl = document.getElementById('time');
        let lastLoad = Date.now();

        function refresh() {
            const nextImg = new Image();
            const ts = Date.now();
            nextImg.src = '/screen.png?t=' + ts;
            nextImg.onload = () => {
                img.src = nextImg.src;
                lastLoad = Date.now();
                timeEl.textContent = 'Updated: ' + new Date().toLocaleTimeString();
                setTimeout(refresh, 800);
            };
            nextImg.onerror = () => {
                setTimeout(refresh, 1500);
            };
        }
        setTimeout(refresh, 800);
    </script>
</body>
</html>
"""

class LiveAuditHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Suppress noisy HTTP access logs
        pass

    def do_GET(self):
        if self.path == "/" or self.path.startswith("/?"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.end_headers()
            self.wfile.write(HTML_PAGE.encode("utf-8"))
        elif self.path.startswith("/screen.png"):
            with frame_lock:
                frame_data = latest_frame
            if frame_data:
                self.send_response(200)
                self.send_header("Content-Type", "image/png")
                self.send_header("Content-Length", str(len(frame_data)))
                self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
                self.end_headers()
                self.wfile.write(frame_data)
            else:
                self.send_response(503)
                self.end_headers()
        elif self.path == "/health":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"ok","device":"emulator-5554"}')
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == "__main__":
    t = threading.Thread(target=capture_worker, daemon=True)
    t.start()
    # Wait for first frame
    time.sleep(1.0)
    server = socketserver.TCPServer((HOST, PORT), LiveAuditHandler)
    print(f"DailyRead Live View running at http://{HOST}:{PORT}")
    server.serve_forever()
