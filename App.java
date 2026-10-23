import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を読み込みます
import java.net.InetSocketAddress; // 接続先の住所とポート番号を扱う機能を読み込みます
import java.util.ArrayList; // Listに項目を追加する機能を読み込みます
import java.util.List; // 項目を並べて扱う機能を読み込みます

public class App { // Appという名前のプログラムを始めます
    public static void main(String[] args) throws Exception { // Javaが最初に実行する場所です
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待ち受けるサーバーを用意します
        server.createContext("/", exchange -> { // / にアクセスが来たときの処理を登録します
            String path = exchange.getRequestURI().getPath(); // リクエストされたパスを取り出します
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の返事をUTF-8のテキストにします
            if (path.equals("/hello")) { // path が /hello か比べます
                String query = exchange.getRequestURI().getRawQuery(); // URLのクエリを取り出します
                String name; // 挨拶に使う名前を入れます
                if (query == null) { // クエリがないかを確認します
                    name = "ゲスト"; // クエリがないときの名前です
                } else {
                    name = query.substring(5); // name= の後ろを切り出します
                }
                message = "こんにちは、" + name + "さん！"; // 名前を含めた挨拶にします
            } else if (path.equals("/todos")) { // path が /todos か比べます
                List<String> todos = new ArrayList<>(); // Todoの一覧を用意します
                todos.add("牛乳を買う"); // Todoを1件追加します
                todos.add("卵を買う"); // Todoを1件追加します
                todos.add("パンを買う"); // Todoを1件追加します
                String html = "<ul>"; // HTMLのリストを始めます
                for (String todo : todos) { // Todoを1件ずつ取り出します
                    html += "<li>" + todo + "</li>"; // TodoをHTMLに追加します
                } // forの繰り返しを終えます
                html += "</ul>"; // HTMLのリストを閉じます
                message = html; // 組み立てたHTMLを返す中身にします
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // この返事をUTF-8のHTMLにします
            } else if (path.equals("/byebye")) { // path が /bye か比べます
                message = "今日の定食はカレー！";
            } else {
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