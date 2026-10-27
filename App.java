import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection; // ★ SQLite JDBCでDB接続を扱います
import java.sql.DriverManager; // ★ SQLite JDBCでDB接続を開きます
import java.sql.PreparedStatement; // ★ SQLの追加・更新・削除に使います
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class App {
    private static Connection connection; // ★ CSVとメモリ上の一覧に代えてSQLite接続を使います

    public static void main(String[] args) throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite:todos.db"); // ★ 要件のSQLiteファイルに接続します
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"); // ★ 起動時にテーブルを用意します
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", App::handle);
        server.createContext("/api/todos", exchange -> { // Todo一覧JSON APIの入口を追加します
            if (!exchange.getRequestMethod().equals("GET")) { // GET以外のリクエストを判定します
                exchange.getResponseHeaders().set("Allow", "GET"); // 許可するHTTPメソッドを返します
                send(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed"); // GET以外を拒否します
                return; // API処理を終了します
            } // HTTPメソッドの判定を終えます
            try { // DB読み出し時の例外を処理します
                String json = renderTodosJson(); // DBのTodo一覧をJSON文字列にします
                byte[] body = json.getBytes(StandardCharsets.UTF_8); // JSONをUTF-8のバイト列にします
                exchange.getResponseHeaders().set("Content-Type", "application/json"); // charsetなしでJSON形式を指定します
                exchange.sendResponseHeaders(200, body.length); // 成功ステータスと本文の長さを返します
                exchange.getResponseBody().write(body); // JSON本文をレスポンスへ書き込みます
                exchange.close(); // レスポンスを閉じます
            } catch (SQLException e) { // DB読み出しエラーを受け取ります
                e.printStackTrace(); // エラー詳細をサーバー側に表示します
                send(exchange, 500, "application/json", "{\"error\":\"database error\"}"); // JSON形式のエラーを返します
            } // DB例外処理を終えます
        }); // Todo一覧JSON APIの入口を登録します
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 (終了するには Ctrl+C)");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String title = parseForm(body).getOrDefault("todo", "").trim();
                if (!title.isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement( // ★ CSV書き込みをINSERTに置き換え
                            "INSERT INTO todos (title, done) VALUES (?, 0)")) {
                        statement.setString(1, title);
                        statement.executeUpdate();
                    }
                }
                redirect(exchange);
                return;
            }

            if (path.equals("/done") && method.equals("GET")) {
                int id = queryId(exchange);
                if (id > 0) {
                    try (PreparedStatement statement = connection.prepareStatement( // ★ 完了状態をUPDATEで保存
                            "UPDATE todos SET done = 1 WHERE id = ?")) {
                        statement.setInt(1, id);
                        statement.executeUpdate();
                    }
                }
                redirect(exchange);
                return;
            }

            if (path.equals("/delete") && method.equals("GET")) {
                int id = queryId(exchange);
                if (id > 0) {
                    try (PreparedStatement statement = connection.prepareStatement( // ★ CSV書き換えをDELETEに置き換え
                            "DELETE FROM todos WHERE id = ?")) {
                        statement.setInt(1, id);
                        statement.executeUpdate();
                    }
                }
                redirect(exchange);
                return;
            }

            if (path.equals("/")) {
                sendHtml(exchange, renderTodos()); // ★ メモリ上の一覧ではなくSELECT結果を表示
            } else {
                send(exchange, 404, "text/plain; charset=UTF-8", "ページが見つかりません");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500, "text/plain; charset=UTF-8", "データベースエラーが発生しました");
        }
    }

    private static String renderTodos() throws SQLException {
        List<Todo> todos = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery( // ★ 一覧をSELECTで読み出します
                     "SELECT id, title, done FROM todos ORDER BY id")) {
            while (results.next()) {
                todos.add(new Todo(results.getInt("id"), results.getString("title"),
                        results.getInt("done") != 0));
            }
        }

        StringBuilder html = new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>body{max-width:600px;margin:24px auto;font:16px sans-serif}"
                + "li{margin:12px 0}.done{text-decoration:line-through;color:#777}</style></head><body>");
        html.append("<h1>わたしのTodo</h1>")
                .append("<form method='post' action='/add'><input name='todo' required>")
                .append("<button>追加</button></form>");
        if (todos.isEmpty()) {
            html.append("<p>やることは、いまゼロです</p>");
        } else {
            html.append("<ul>");
            for (Todo todo : todos) {
                html.append("<li><span class='").append(todo.done ? "done" : "").append("'>")
                        .append(escapeHtml(todo.title)).append(todo.done ? " ✓" : "")
                        .append("</span> ");
                if (!todo.done) {
                    html.append("<a href='/done?id=").append(todo.id).append("'>完了</a> ");
                }
                html.append("<a href='/delete?id=").append(todo.id).append("'>削除</a></li>");
            }
            html.append("</ul>");
        }
        return html.append("</body></html>").toString();
    }

    private static String renderTodosJson() throws SQLException { // Todo一覧を指定形式のJSONに変換します
        StringBuilder json = new StringBuilder("["); // JSON配列の開始を用意します
        try (Statement statement = connection.createStatement(); // Todo一覧取得用のSQL文を作ります
             ResultSet results = statement.executeQuery("SELECT title, done FROM todos ORDER BY id")) { // 全Todoを読み出します
            boolean first = true; // 最初の要素かどうかを記録します
            while (results.next()) { // 読み出したTodoを順に処理します
                if (!first) json.append(","); // 2件目以降の要素の前にカンマを付けます
                json.append("{\"title\":\"").append(escapeJson(results.getString("title"))) // タイトルをJSON用にエスケープして追加します
                        .append("\",\"done\":") // doneプロパティを追加します
                        .append(results.getInt("done") != 0) // done値をJSONの真偽値で追加します
                        .append("}"); // Todoオブジェクトを閉じます
                first = false; // 次のTodoが2件目以降であることを記録します
            } // 全Todoの処理を終えます
        } // SQLリソースを閉じます
        return json.append("]").toString(); // JSON配列を閉じて返します
    } // Todo一覧JSONの生成を終えます

    private static String escapeJson(String value) { // JSON文字列内の特殊文字をエスケープします
        if (value == null) return ""; // nullのタイトルを空文字列として扱います
        StringBuilder escaped = new StringBuilder(); // エスケープ済み文字列を組み立てます
        for (int i = 0; i < value.length(); i++) { // タイトルの文字を1文字ずつ確認します
            char ch = value.charAt(i); // 現在の文字を取り出します
            switch (ch) { // JSONで特別な意味を持つ文字を判定します
                case '"': escaped.append("\\\""); break; // 引用符をバックスラッシュ付きにします
                case '\\': escaped.append("\\\\"); break; // バックスラッシュを二重にします
                case '\b': escaped.append("\\b"); break; // バックスペースをJSON表記にします
                case '\f': escaped.append("\\f"); break; // フォームフィードをJSON表記にします
                case '\n': escaped.append("\\n"); break; // 改行をJSON表記にします
                case '\r': escaped.append("\\r"); break; // 復帰文字をJSON表記にします
                case '\t': escaped.append("\\t"); break; // タブをJSON表記にします
                default: // その他の文字を判定します
                    if (ch < 0x20) escaped.append(String.format("\\u%04x", (int) ch)); // 制御文字をUnicode表記にします
                    else escaped.append(ch); // 通常の文字はそのまま追加します
            } // 文字ごとのエスケープ処理を終えます
        } // タイトル全体の走査を終えます
        return escaped.toString(); // JSON用にエスケープした文字列を返します
    } // JSON文字列エスケープを終えます

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
        if (query == null) return -1;
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
        final boolean done;

        Todo(int id, String title, boolean done) {
            this.id = id;
            this.title = title;
            this.done = done;
        }
    }
}
