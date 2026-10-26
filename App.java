import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を読み込みます
import java.net.InetSocketAddress; // 接続先の住所とポート番号を扱う機能を読み込みます
import java.net.URLDecoder; // フォームから届いた文字を元に戻します
import java.nio.charset.StandardCharsets; // UTF-8の文字コードを指定します
import java.util.ArrayList; // Listに項目を追加する機能を読み込みます
import java.util.List; // 項目を並べて扱う機能を読み込みます

public class App { // Appという名前のプログラムを始めます
    public static void main(String[] args) throws Exception { // Javaが最初に実行する場所です
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待ち受けるサーバーを用意します
        List<String> todos = new ArrayList<>(); // 追加したTodoを保存する空の一覧です
        server.createContext("/", exchange -> { // / にアクセスが来たときの処理を登録します
            String path = exchange.getRequestURI().getPath(); // リクエストされたパスを取り出します
            String method = exchange.getRequestMethod(); // GETやPOSTなどの方法を取り出します
            String message;
            if (path.equals("/add") && method.equals("POST")) { // POSTで届いたTodoを追加します
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // フォームの中身をUTF-8で読み取ります
                String value = body.substring(5); // todo= の後ろを取り出します
                String todo = URLDecoder.decode(value, StandardCharsets.UTF_8); // URL形式の日本語を元に戻します
                if (!todo.isEmpty()) { // 入力が空でないことを確かめます
                    todos.add(todo); // Todoを一覧に追加します
                } // 空でない場合の処理を終えます
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先を / にします
                exchange.sendResponseHeaders(303, -1); // / へ移動する返事を送ります
                exchange.close(); // 返事を閉じます
                return; // この分岐の処理を終えます
            } else if (path.equals("/")) { // / でフォームとTodo一覧を表示します
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // 入力フォームと一覧の開始部分を作ります
                for (String todo : todos) { // Todoを1件ずつ取り出します
                    html += "<li>" + todo + "</li>"; // TodoをHTMLに追加します
                } // forの繰り返しを終えます
                html += "</ul>"; // HTMLのリストを閉じます
                message = html; // 組み立てたHTMLを返す中身にします
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // この返事をUTF-8のHTMLにします
            } else if (path.equals("/hello")) { // path が /hello か比べます
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の返事をUTF-8のテキストにします
                String query = exchange.getRequestURI().getRawQuery(); // URLのクエリを取り出します
                String name; // 挨拶に使う名前を入れます
                if (query == null) { // クエリがないかを確認します
                    name = "ゲスト"; // クエリがないときの名前です
                } else {
                    name = query.substring(5); // name= の後ろを切り出します
                }
                message = "こんにちは、" + name + "さん！"; // 名前を含めた挨拶にします
            } else if (path.equals("/byebye")) { // path が /bye か比べます
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の返事をUTF-8のテキストにします
                message = "今日の定食はカレー！";
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
