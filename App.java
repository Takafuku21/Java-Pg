import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を読み込みます
import java.net.InetSocketAddress; // 接続先の住所とポート番号を扱う機能を読み込みます
import java.net.URLDecoder; // フォームから届いた文字を元に戻します
import java.nio.charset.StandardCharsets; // UTF-8の文字コードを指定します
import java.util.ArrayList; // Listに項目を追加する機能を読み込みます
import java.util.List; // 項目を並べて扱う機能を読み込みます

public class App { // Appという名前のプログラムを始めます
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを保存する一覧です
    static int nextId = 1; // ★変更 次にTodoへ付ける番号です

    public static void main(String[] args) throws Exception { // Javaが最初に実行する場所です
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待ち受けるサーバーを用意します
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更 起動時のTodoを1件追加します
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更 起動時のTodoを作ります
        egg.setDone(true); // ★変更 卵を買うTodoを完了にします
        todos.add(egg); // ★変更 起動時のTodoを一覧に加えます
        server.createContext("/", exchange -> { // / にアクセスが来たときの処理を登録します
            String path = exchange.getRequestURI().getPath(); // リクエストされたパスを取り出します
            String method = exchange.getRequestMethod(); // GETやPOSTなどの方法を取り出します
            String message;
            if (path.equals("/add") && method.equals("POST")) { // ★変更 POSTで届いたTodoを追加します
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // フォームの中身をUTF-8で読み取ります
                String value = body.substring(5); // todo= の後ろを取り出します
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更 フォームの文字を元に戻してタイトルにします
                if (!title.isEmpty()) { // ★変更 入力が空でないことを確かめます
                    todos.add(new Todo(nextId, title)); // ★変更 番号とタイトルをTodoにして一覧へ加えます
                    nextId++; // ★変更 次に使う番号を進めます
                } // 空でない場合の処理を終えます
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先を / にします
                exchange.sendResponseHeaders(303, -1); // / へ移動する返事を送ります
                exchange.close(); // 返事を閉じます
                return; // この分岐の処理を終えます
            } else if (path.equals("/")) { // ★変更 / でフォームとTodo一覧を表示します
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // 入力フォームと一覧の開始部分を作ります
                for (Todo todo : todos) { // ★変更 Todoを1件ずつ取り出します
                    String mark = ""; // ★変更 未完了のTodoには印を付けません
                    if (todo.isDone()) { // ★変更 Todoが完了しているかを確かめます
                        mark = " ✔"; // ★変更 完了済みのTodoに印を付けます
                    } // ★変更 完了状態の確認を終えます
                    html += "<li>" + todo.getTitle() + mark + "</li>"; // ★変更 タイトルと必要な印をHTMLに追加します
                } // forの繰り返しを終えます
                html += "</ul>"; // HTMLのリストを閉じます
                message = html; // 組み立てたHTMLを返す中身にします
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // この返事をUTF-8のHTMLにします
            } else {
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の返事をUTF-8のテキストにします
                message = "ページが見つかりません";
            }

            byte[] body = message.getBytes("UTF-8"); // 文字をUTF-8のデータにします
            exchange.sendResponseHeaders(200, body.length); // 正常の返事とデータの長さを送ります
            exchange.getResponseBody().write(body); // データをブラウザへ送ります
            exchange.getResponseBody().close(); // 返事を閉じます
        }); // / に来たときの処理の登録を終えます
        server.start(); // サーバーの待ち受けを始めます
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことを表示します
    } // mainの処理を終えます

} // Appプログラムを終えます

class Todo { // ★変更 Todoの情報をまとめるクラスです
    private final int id; // ★変更 Todoの番号です
    private final String title; // ★変更 Todoのタイトルです
    private boolean done; // ★変更 Todoが完了したかを保存します

    Todo(int id, String title) { // ★変更 番号とタイトルを受け取ってTodoを作ります
        this.id = id; // ★変更 番号を保存します
        this.title = title; // ★変更 タイトルを保存します
        this.done = false; // ★変更 作成時は未完了にします
    } // ★変更 Todoの作成処理を終えます

    int getId() { // ★変更 Todoの番号を読み出します
        return id; // ★変更 番号を返します
    } // ★変更 番号を読み出す処理を終えます

    String getTitle() { // ★変更 Todoのタイトルを読み出します
        return title; // ★変更 タイトルを返します
    } // ★変更 タイトルを読み出す処理を終えます

    boolean isDone() { // ★変更 Todoが完了したかを読み出します
        return done; // ★変更 完了状態を返します
    } // ★変更 完了状態を読み出す処理を終えます

    void setDone(boolean done) { // ★変更 Todoの完了状態を書き換えます
        this.done = done; // ★変更 完了状態を保存します
    } // ★変更 完了状態を書き換える処理を終えます
} // ★変更 Todoクラスを終えます
