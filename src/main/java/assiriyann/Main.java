package assiriyann;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8081;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        Map<String, AtomicInteger> votes = new ConcurrentHashMap<>();
        votes.put("Apple", new AtomicInteger(0));
        votes.put("Banana", new AtomicInteger(0));
        votes.put("Mango", new AtomicInteger(0));

        server.createContext("/", new RootHandler(votes));
        server.createContext("/vote", new VoteHandler(votes));

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Voting server started at http://localhost:" + port + "/");
    }

    static class RootHandler implements HttpHandler {
        private final Map<String, AtomicInteger> votes;

        RootHandler(Map<String, AtomicInteger> votes) {
            this.votes = votes;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            int totalVotes = votes.values().stream().mapToInt(AtomicInteger::get).sum();

            StringBuilder html = new StringBuilder();
            html.append("<!doctype html><html><head><meta charset='utf-8'>")
                    .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                    .append("<title>Live Voting</title>")
                    .append("<style>")
                    .append("*{margin:0;padding:0;box-sizing:border-box}")
                    .append("body{font-family:'Segoe UI',Tahoma,sans-serif;")
                    .append("background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);")
                    .append("min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}")
                    .append(".container{background:rgba(255,255,255,0.95);border-radius:20px;")
                    .append("box-shadow:0 20px 60px rgba(0,0,0,0.3);max-width:600px;width:100%;padding:40px;")
                    .append("backdrop-filter:blur(10px)}")
                    .append("h1{color:#333;font-size:2.5em;margin-bottom:10px;text-align:center;")
                    .append("background:linear-gradient(135deg,#667eea,#764ba2);")
                    .append("-webkit-background-clip:text;-webkit-text-fill-color:transparent;")
                    .append("background-clip:text;font-weight:700}")
                    .append(".subtitle{text-align:center;color:#666;margin-bottom:30px;font-size:1.1em}")
                    .append(".total-votes{text-align:center;font-size:1.2em;color:#764ba2;")
                    .append("font-weight:600;margin-bottom:30px;padding:15px;")
                    .append("background:linear-gradient(135deg,rgba(102,126,234,0.1),rgba(118,75,162,0.1));")
                    .append("border-radius:10px}")
                    .append(".option{margin:20px 0;position:relative;transition:transform 0.2s}")
                    .append(".option:hover{transform:translateX(5px)}")
                    .append(".option label{display:flex;align-items:center;cursor:pointer;")
                    .append("padding:20px;border-radius:12px;background:#f8f9fa;")
                    .append("border:2px solid transparent;transition:all 0.3s ease;position:relative;overflow:hidden}")
                    .append(".option label:hover{border-color:#667eea;background:#fff;box-shadow:0 5px 15px rgba(102,126,234,0.2)}")
                    .append(".option input[type='radio']{appearance:none;width:24px;height:24px;")
                    .append("border:2px solid #667eea;border-radius:50%;margin-right:15px;")
                    .append("position:relative;cursor:pointer;flex-shrink:0;transition:all 0.3s}")
                    .append(".option input[type='radio']:checked{background:#667eea;border-color:#667eea;")
                    .append("box-shadow:0 0 0 4px rgba(102,126,234,0.2)}")
                    .append(".option input[type='radio']:checked::after{content:'';position:absolute;")
                    .append("top:50%;left:50%;transform:translate(-50%,-50%);width:8px;height:8px;")
                    .append("background:#fff;border-radius:50%}")
                    .append(".option-text{flex:1;font-size:1.1em;color:#333;font-weight:500}")
                    .append(".count-badge{background:linear-gradient(135deg,#667eea,#764ba2);")
                    .append("color:#fff;padding:8px 16px;border-radius:20px;font-weight:700;")
                    .append("font-size:1em;min-width:60px;text-align:center}")
                    .append(".progress-bar{position:absolute;left:0;top:0;height:100%;")
                    .append("background:linear-gradient(90deg,rgba(102,126,234,0.1),rgba(118,75,162,0.1));")
                    .append("transition:width 0.5s ease;z-index:0;border-radius:12px}")
                    .append(".vote-btn{width:100%;background:linear-gradient(135deg,#667eea,#764ba2);")
                    .append("color:#fff;border:none;padding:18px;border-radius:12px;font-size:1.2em;")
                    .append("font-weight:700;cursor:pointer;margin-top:30px;transition:all 0.3s;")
                    .append("box-shadow:0 5px 20px rgba(102,126,234,0.4)}")
                    .append("</style></head><body>");

            html.append("<div class='container'>")
                    .append("<h1>🗳️ Live Voting</h1>")
                    .append("<div class='subtitle'>Choose your favorite option</div>")
                    .append("<div class='total-votes'>Total Votes: ").append(totalVotes).append("</div>")
                    .append("<form method='post' action='/vote'>");

            for (String opt : votes.keySet()) {
                int count = votes.get(opt).get();
                double pct = totalVotes > 0 ? count * 100.0 / totalVotes : 0;
                html.append("<div class='option'><label>")
                        .append("<div class='progress-bar' style='width:").append(String.format(String.valueOf(pct), "%.1f")).append("%'></div>")
                        .append("<input type='radio' name='option' value='").append(escape(opt)).append("' required>")
                        .append("<span class='option-text'>").append(escape(opt)).append("</span>")
                        .append("<span class='count-badge'>").append(count).append("</span>")
                        .append("</label></div>");
            }

            html.append("<button type='submit' class='vote-btn'>Cast Your Vote</button></form></div>")
                    .append("<script>setTimeout(()=>location.reload(),5000);</script>")
                    .append("</body></html>");

            byte[] resp = html.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }

        private String escape(String s) {
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
        }
    }

    static class VoteHandler implements HttpHandler {
        private final Map<String, AtomicInteger> votes;

        VoteHandler(Map<String, AtomicInteger> votes) {
            this.votes = votes;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String option = parseForm(body, "option");
            if (option != null && votes.containsKey(option)) {
                votes.get(option).incrementAndGet();
            }

            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(303, -1);
        }

        private String parseForm(String body, String key) {
            for (String part : body.split("&")) {
                int eq = part.indexOf('=');
                if (eq < 0) continue;
                String k = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                if (k.equals(key)) return v;
            }
            return null;
        }
    }
}
