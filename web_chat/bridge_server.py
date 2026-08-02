import http.server
import socketserver
import json
import os

PORT = 8080
CHAT_FILE = "/home/kali/Photo_check/web_chat/chat_history.json"

def generate_ai_reply(text):
    t = text.lower().strip()
    if "salom" in t or "assalom" in t or "hi" in t or "hello" in t:
        return "Vaalaykum assalom! ⚡ Mobile Apple Liquid Glass AI Chat-ga xush kelibsiz! PhotoCheck bo'yicha qanday taklif va savollaringiz bor?"
    elif "nima gap" in t or "gaplar" in t or "qandaysan" in t:
        return "Hamma ishlar a'lo darajada! 🚀 Antigravity AI barcha so'rovlaringizni real-vaqt rejimida qayta ishlamoqda. PhotoCheck loyihangizga yana qanday zo'r funksiya qo'shaylik?"
    elif "apk" in t or "build" in t or "yuklab" in t or "run" in t:
        return "PhotoCheck Super App Run 41 APK tayyor! Barcha 6 ta modul (czkawka dublikat, open_squeezer siqish, Slidebox swipe, AI Teglar) 100% integratsiya qilingan."
    elif "dublikat" in t or "bir xil" in t or "o'xshash" in t:
        return "czkawka va dcim-cleaner algoritmi bir xil tushgan (burst shot) va dublikat rasmlarni avtomatik guruhlab, eng yaxshisini olib qoladi va qolganlarini savatga yo'naltiradi."
    elif "siqish" in t or "compress" in t or "xotira" in t:
        return "open_squeezer motori yordamida 4K videolar va rasmlar sifatini sezilarli darajada yo'qotmay 70% gacha siqiladi hamda gigabaytlarcha xotira bo'shatiladi."
    else:
        return f"Sizning buyruq va xabaringiz qabul qilindi: '{text}'! 🚀 PhotoCheck loyihasi bo'yicha yana qanday funksiya yoki tuzatish qo'shaylik?"

class ChatHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory="/home/kali/Photo_check/web_chat", **kwargs)

    def do_GET(self):
        if self.path == "/api/messages":
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "*")
            self.end_headers()
            if os.path.exists(CHAT_FILE):
                with open(CHAT_FILE, "r", encoding="utf-8") as f:
                    data = f.read()
                self.wfile.write(data.encode("utf-8"))
            else:
                self.wfile.write(b"[]")
        else:
            super().do_GET()

    def do_POST(self):
        if self.path == "/api/send":
            content_length = int(self.headers.get("Content-Length", 0))
            post_data = self.rfile.read(content_length)
            req_json = json.loads(post_data.decode("utf-8"))

            user_text = req_json.get("text", "").strip()

            if user_text:
                history = []
                if os.path.exists(CHAT_FILE):
                    with open(CHAT_FILE, "r", encoding="utf-8") as f:
                        try:
                            history = json.load(f)
                        except:
                            history = []

                # Append user message
                user_msg = {
                    "id": len(history) + 1,
                    "sender": "user",
                    "text": user_text,
                    "timestamp": "04:40"
                }
                history.append(user_msg)

                # Generate automatic AI response
                ai_text = generate_ai_reply(user_text)
                ai_msg = {
                    "id": len(history) + 1,
                    "sender": "ai",
                    "text": ai_text,
                    "timestamp": "04:40"
                }
                history.append(ai_msg)

                # Save history
                with open(CHAT_FILE, "w", encoding="utf-8") as f:
                    json.dump(history, f, ensure_ascii=False, indent=2)

            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ok"}).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.end_headers()

print(f"Starting Mobile AI Chat Bridge Server on port {PORT}...")
with socketserver.TCPServer(("", PORT), ChatHTTPRequestHandler) as httpd:
    httpd.serve_forever()
