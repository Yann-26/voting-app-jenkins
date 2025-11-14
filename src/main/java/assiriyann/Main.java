package assiriyann;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet("/")
public class Main extends HttpServlet {

    private final Map<String, AtomicInteger> votes = new ConcurrentHashMap<>();

    @Override
    public void init() {
        votes.put("Apple", new AtomicInteger(0));
        votes.put("Banana", new AtomicInteger(0));
        votes.put("Mango", new AtomicInteger(0));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        int totalVotes = votes.values().stream().mapToInt(AtomicInteger::get).sum();

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset='utf-8'>");
        html.append("<title>Live Voting</title>");
        html.append("<style>body{font-family:sans-serif;padding:20px}</style>");
        html.append("</head><body>");

        html.append("<h1>Live Voting</h1>");
        html.append("<div>Total Votes: ").append(totalVotes).append("</div>");
        html.append("<form method='post' action='/'>");

        for (String opt : votes.keySet()) {
            int count = votes.get(opt).get();
            html.append("<div>");
            html.append("<label>");
            html.append("<input type='radio' name='option' value='").append(opt).append("' required> ");
            html.append(opt).append(" — ").append(count);
            html.append("</label>");
            html.append("</div>");
        }

        html.append("<button type='submit'>Vote</button></form>");
        html.append("</body></html>");

        out.write(html.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String option = req.getParameter("option");
        if (option != null && votes.containsKey(option)) {
            votes.get(option).incrementAndGet();
        }

        resp.sendRedirect("/");
    }
}
