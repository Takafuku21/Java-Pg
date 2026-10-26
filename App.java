import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を読み込みます
import java.net.InetSocketAddress; // 接続先の住所とポート番号を扱う機能を読み込みます
import java.net.URLDecoder; // ★変更 フォームから届いた文字を元に戻します
import java.nio.charset.StandardCharsets; // ★変更 UTF-8の文字コードを指定します
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
            String method = exchange.getRequestMethod(); // ★変更 GETやPOSTなどの方法を取り出します
            String message;
            if (path.equals("/add") && method.equals("POST")) { // ★変更 POSTで届いたTodoを追加します
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // ★変更
                                                                                                            // フォームの中身をUTF-8で読み取ります
                String value = body.substring(5); // ★変更 todo= の後ろを取り出します
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更 フォームの文字を元に戻してタイトルにします
                if (!title.isEmpty()) { // ★変更 入力が空でないことを確かめます
                    todos.add(new Todo(nextId, title)); // ★変更 番号とタイトルをTodoにして一覧へ加えます
                    nextId++; // ★変更 次に使う番号を進めます
                } // 空でない場合の処理を終えます
                exchange.getResponseHeaders().set("Location", "/"); // ★変更 戻り先を / にします
                exchange.sendResponseHeaders(303, -1); // ★変更 / へ移動する返事を送ります
                exchange.close(); // ★変更 返事を閉じます
                return; // ★変更 この分岐の処理を終えます
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加 GETで届いたidのTodoを完了にします
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLのidを受け取ります
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idが付いているか確認します
                    try { // ★追加 idを数値に変換します
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idを数値にします
                        for (Todo todo : todos) { // ★追加 Todoを1件ずつ確認します
                            if (todo.getId() == id) { // ★追加 idが一致するか確認します
                                todo.setDone(true); // ★追加 Todoを完了にします
                                break; // ★追加 一致したTodoの確認を終えます
                            } // ★追加 idの一致確認を終えます
                        } // ★追加 Todoの確認を終えます
                    } catch (NumberFormatException e) { // ★追加 数字でないidを受け止めます
                    } // ★追加 idの数値変換を終えます
                } // ★追加 idがある場合の処理を終えます
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 戻り先を / にします
                exchange.sendResponseHeaders(303, -1); // ★追加 / へ移動する返事を送ります
                exchange.close(); // ★追加 返事を閉じます
                return; // ★追加 この分岐の処理を終えます
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加 GETで届いたidのTodoを削除します
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLのidを受け取ります
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idが付いているか確認します
                    try { // ★追加 idを数値に変換します
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idを数値にします
                        todos.removeIf(todo -> todo.getId() == id); // ★追加 idが一致するTodoを一覧から削除します
                    } catch (NumberFormatException e) { // ★追加 数字でないidを受け止めます
                    } // ★追加 idの数値変換を終えます
                } // ★追加 idがある場合の処理を終えます
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 戻り先を / にします
                exchange.sendResponseHeaders(303, -1); // ★追加 / へ移動する返事を送ります
                exchange.close(); // ★追加 返事を閉じます
                return; // ★追加 この分岐の処理を終えます
            } else if (path.equals("/reorder") && method.equals("POST")) { // ドラッグ後の並び順を受け取ります
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 並び順を読み取ります
                String[] ids = body.substring(4).split(","); // ids= の後ろから番号を取り出します
                List<Todo> reordered = new ArrayList<>(); // 新しい並び順を一時的に用意します
                for (String value : ids) { // 受け取った番号を順に見ます
                    int id = Integer.parseInt(value); // 番号を数値にします
                    for (Todo todo : todos) { // 保存中のTodoを探します
                        if (todo.getId() == id) { // 番号が一致したか確かめます
                            reordered.add(todo); // Todoを新しい順番に加えます
                            break; // 一致したTodoの検索を終えます
                        }
                    }
                }
                todos.clear(); // 元の並びを空にします
                todos.addAll(reordered); // 新しい並び順を保存します
                exchange.getResponseHeaders().set("Location", "/"); // / に戻します
                exchange.sendResponseHeaders(303, -1); // / へ移動する返事を送ります
                exchange.close(); // 返事を閉じます
                return; // この分岐の処理を終えます
            } else if (path.equals("/")) { // ★変更 / でフォームとTodo一覧を表示します
                String html = "<!DOCTYPE html><html><head><style>body { max-width: 600px; margin: 24px auto; font-size: 16px; } ul { list-style: none; padding-left: 0; } .priority { display: inline-block; min-width: 2em; } li { cursor: grab; }</style></head><body><h1>わたしのTodo</h1><form method='post' action='/add'><input name='todo'><button>追加</button></form>"; // 見出し・スタイル・入力フォームを表示します
                if (todos.isEmpty()) { // 一覧が空かどうかを確かめます
                    html += "<p>やることは、いまゼロです</p>"; // 空のときの案内を表示します
                } else { // Todoがあるときの一覧を作ります
                    html += "<ul id='todo-list'>"; // ドラッグできる一覧を始めます
                    int index = 0; // 優先順位の番号を数えます
                    for (Todo todo : todos) { // ★変更 Todoを1件ずつ取り出します
                        String priority = "優先度" + (index < 20 ? String.valueOf((char) (0x2460 + index)) : String.valueOf(index + 1)); // 優先度①から順に表示します
                        String mark = ""; // ★変更 未完了のTodoには印を付けません
                        if (todo.isDone()) { // ★変更 Todoが完了しているかを確かめます
                            mark = " ✔"; // ★変更 完了済みのTodoに印を付けます
                        } // ★変更 完了状態の確認を終えます
                        html += "<li draggable='true' data-id='" + todo.getId() + "' ondragstart='dragTodo(event)' ondragover='event.preventDefault()' ondrop='dropTodo(event)'><span class='priority'>" + priority + "</span>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId()
                                + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // 優先順位・Todo・操作リンクを表示します
                        index++; // 次の優先順位に進みます
                    } // forの繰り返しを終えます
                    html += "</ul>"; // ★変更 HTMLのリストを閉じます
                    html += "<script>function dragTodo(event){event.dataTransfer.setData('text/plain',event.currentTarget.dataset.id);} function dropTodo(event){event.preventDefault();const target=event.currentTarget;const id=event.dataTransfer.getData('text/plain');const items=Array.from(document.querySelectorAll('#todo-list li'));const dragged=items.find(item=>item.dataset.id===id);if(!dragged||dragged===target)return;const rect=target.getBoundingClientRect();target.parentNode.insertBefore(dragged,event.clientY>rect.top+rect.height/2?target.nextSibling:target);const ordered=Array.from(document.querySelectorAll('#todo-list li'));ordered.forEach((item,index)=>{item.querySelector('.priority').textContent='優先度'+(index<20?String.fromCharCode(0x2460+index):String(index+1));});fetch('/reorder',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'ids='+ordered.map(item=>item.dataset.id).join(',')});}</script>"; // マウスで並べ替え、優先度とサーバー側の順番を更新します
                } // 空かどうかによる表示を終えます
                html += "</body></html>"; // HTMLページを閉じます
                message = html; // ★変更 組み立てたHTMLを返す中身にします
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // ★変更
                                                                                               // この返事をUTF-8のHTMLにします
            } else { // ★変更 該当するパスがないときの受け皿です
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の返事をUTF-8のテキストにします
                message = "ページが見つかりません"; // ★変更 見つからないことを返します
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
