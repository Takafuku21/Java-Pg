import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class App {
    private static final List<Todo> todos = new ArrayList<>();
    private static int nextId = 1;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", App::handle);
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 (終了するには Ctrl+C)");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/add") && method.equals("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String title = parseForm(body).getOrDefault("todo", "").trim();
            if (!title.isEmpty()) {
                synchronized (todos) {
                    todos.add(new Todo(nextId++, title));
                }
            }
            redirect(exchange);
            return;
        }

        if (path.equals("/done") && method.equals("GET")) {
            int id = queryId(exchange);
            synchronized (todos) {
                for (Todo todo : todos) {
                    if (todo.id == id) {
                        todo.done = true;
                        break;
                    }
                }
            }
            redirect(exchange);
            return;
        }

        if (path.equals("/delete") && method.equals("GET")) {
            int id = queryId(exchange);
            synchronized (todos) {
                todos.removeIf(todo -> todo.id == id);
            }
            redirect(exchange);
            return;
        }

        if (path.equals("/") && method.equals("GET")) {
            sendHtml(exchange, renderTodos());
        } else {
            send(exchange, 404, "text/plain; charset=UTF-8", "ページが見つかりません");
        }
    }

    private static String renderTodos() {
        List<Todo> snapshot;
        synchronized (todos) {
            snapshot = new ArrayList<>(todos);
        }

        int remaining = 0;
        for (Todo todo : snapshot) {
            if (!todo.done)
                remaining++;
        }

        StringBuilder html = new StringBuilder("<!DOCTYPE html><html lang='ja'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>わたしのTodo</title><style>"
                + ":root{color-scheme:light;--ink:#3e342b;--muted:#827568;--paper:#fffdf8;--line:#eadfce;--rust:#bd633d;--moss:#68744f}"
                + "*{box-sizing:border-box}body{min-height:100vh;margin:0;padding:56px 20px;background:radial-gradient(ellipse at 15% 8%,#f4dfc8 0,transparent 34%),radial-gradient(ellipse at 90% 90%,#e4e6d4 0,transparent 35%),#f7f1e7;color:var(--ink);font:16px/1.6 'Yu Gothic',Meiryo,sans-serif}"
                + ".wrap{max-width:620px;margin:0 auto}.hero{position:relative;min-height:188px;display:flex;align-items:center;overflow:hidden}.hero-copy{position:relative;z-index:1}.scene{position:absolute;right:0;bottom:0;width:min(55%,300px);height:188px;opacity:.8;pointer-events:none}.eyebrow{margin:0 0 10px;color:var(--rust);font-size:12px;font-weight:700;letter-spacing:.2em;text-transform:uppercase}"
                + "h1{margin:0;font-family:Georgia,'Yu Mincho',serif;font-size:clamp(34px,7vw,48px);font-weight:500;letter-spacing:.04em}.intro{margin:10px 0 28px;color:var(--muted)}"
                + ".card{padding:28px;background:var(--paper);border:1px solid rgba(183,148,111,.25);border-radius:18px;box-shadow:0 18px 45px rgba(83,60,38,.09)}"
                + "form{display:flex;gap:10px;margin-bottom:24px}input{min-width:0;flex:1;padding:13px 15px;border:1px solid var(--line);border-radius:10px;background:#fff;color:var(--ink);font:inherit;outline:none}input:focus{border-color:var(--rust);box-shadow:0 0 0 3px rgba(189,99,61,.13)}"
                + "button{padding:12px 19px;border:0;border-radius:10px;background:var(--rust);color:white;font:inherit;font-weight:700;cursor:pointer;transition:background .2s,transform .2s}button:hover{background:#a64f30;transform:translateY(-1px)}"
                + ".list-head{display:flex;align-items:center;justify-content:space-between;padding-bottom:12px;border-bottom:1px solid var(--line);color:var(--muted);font-size:13px}.count{color:var(--moss);font-weight:700}ul{margin:0;padding:0;list-style:none}li{display:flex;align-items:center;gap:12px;padding:15px 2px;border-bottom:1px solid #f0e9df}li:last-child{border-bottom:0}"
                + ".mark{display:grid;width:22px;height:22px;flex:none;place-items:center;border:1.5px solid #cbbba7;border-radius:50%;color:white;font-size:13px}.done .mark{border-color:var(--moss);background:var(--moss)}.title{flex:1;overflow-wrap:anywhere}.done .title{color:#a39a8d;text-decoration:line-through}"
                + "a{color:var(--moss);font-size:13px;text-decoration:none}a:hover{text-decoration:underline}.delete{color:#a26956}.empty{padding:30px 8px;text-align:center;color:var(--muted)}.empty-mark{display:block;margin-bottom:8px;color:var(--rust);font-size:28px}"
                + "footer{margin-top:18px;color:#988a7a;text-align:center;font-size:12px}@media(max-width:480px){body{padding:32px 14px}.card{padding:20px}form{gap:8px}button{padding:11px 14px}}"
                + "</style></head><body><main class='wrap'><header class='hero'><div class='hero-copy'><p class='eyebrow'>A little autumn, a little progress</p>"
                + "<h1>わたしの Todo</h1><p class='intro'>秋の空気と一緒に、今日やることをひとつずつ。</p></div>"
                + "<svg class='scene' viewBox='0 0 300 188' role='img' aria-label='秋色に染まる木立と落ち葉' xmlns='http://www.w3.org/2000/svg'>"
                + "<path d='M0 174 Q65 162 124 172 T300 166 V188 H0Z' fill='#d9d6bd' opacity='.65'/><path d='M214 166 Q222 122 218 69 M251 170 Q246 131 258 92 M174 170 Q181 137 169 111' fill='none' stroke='#806148' stroke-width='7' stroke-linecap='round'/>"
                + "<path d='M218 119l-25-22m27 8 22-25m-6 83 24-25m-85 7-20-19' fill='none' stroke='#806148' stroke-width='4' stroke-linecap='round'/>"
                + "<g opacity='.94'><circle cx='191' cy='81' r='27' fill='#bd7049'/><circle cx='222' cy='65' r='32' fill='#c9874c'/><circle cx='253' cy='77' r='26' fill='#a96842'/><circle cx='198' cy='110' r='25' fill='#b57d4f'/><circle cx='238' cy='103' r='29' fill='#9a7750'/><circle cx='265' cy='112' r='20' fill='#c18a4e'/><circle cx='164' cy='105' r='20' fill='#b27a49'/></g>"
                + "<path d='M54 174q4-30 0-61m0 25-13-13m14 3 13-14' fill='none' stroke='#8a6a4d' stroke-width='4' stroke-linecap='round'/><g fill='#a58a5c'><ellipse cx='37' cy='119' rx='15' ry='9' transform='rotate(28 37 119)'/><ellipse cx='69' cy='112' rx='15' ry='9' transform='rotate(-34 69 112)'/><ellipse cx='50' cy='99' rx='16' ry='10' transform='rotate(15 50 99)'/></g>"
                + "<path d='M95 164q8-10 18-2m-4-17q8-8 15-2m36 32q7-11 16-4' fill='none' stroke='#b6784e' stroke-width='3' stroke-linecap='round'/></svg></header><section class='card'>"
                + "<form method='post' action='/add'><input name='todo' placeholder='今日やることを追加…' aria-label='Todo' required><button type='submit'>＋ 追加</button></form>"
                + "<div class='list-head'><span>今日のリスト</span><span class='count'>あと " + remaining + " 件</span></div>");
        if (snapshot.isEmpty()) {
            html.append("<div class='empty'><span class='empty-mark'>✦</span>まずは、やることをひとつ書いてみましょう。</div>");
        } else {
            html.append("<ul>");
            for (Todo todo : snapshot) {
                html.append("<li class='").append(todo.done ? "done" : "")
                        .append("'><span class='mark'>").append(todo.done ? "✓" : "")
                        .append("</span><span class='title'>").append(escapeHtml(todo.title)).append("</span>");
                if (!todo.done) {
                    html.append("<a href='/done?id=").append(todo.id).append("'>完了</a>");
                }
                html.append("<a class='delete' href='/delete?id=").append(todo.id).append("'>削除</a></li>");
            }
            html.append("</ul>");
        }
        html.append("</section><footer>ゆっくりでも、ひとつずつ。</footer></main></body></html>");
        return html.toString();
    }

    private static Map<String, String> parseForm(String body) {
        return java.util.Arrays.stream(body.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(pair -> pair.length == 2)
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8),
                        (first, second) -> second));
    }

    private static int queryId(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null)
            return -1;
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals("id")) {
                try {
                    return Integer.parseInt(URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void redirect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static void sendHtml(HttpExchange exchange, String html) throws IOException {
        send(exchange, 200, "text/html; charset=UTF-8", html);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String text)
            throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static class Todo {
        final int id;
        final String title;
        boolean done;

        Todo(int id, String title) {
            this.id = id;
            this.title = title;
        }
    }
}