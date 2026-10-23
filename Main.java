import java.util.ArrayList; // ArrayList（複数の値を順番に入れる入れ物）を使います
import java.util.List; // List（複数の値を並べて扱う型）を使います

class Todo { // Todo（やること1件分の情報）を表すクラスです
    String title; // title（タイトル）にやることを書きます
    boolean done; // done（済んだかどうか）に true または false を入れます

    Todo(String title, boolean done) { // Todoを作るときにタイトルと済んだかどうかを受け取ります
        this.title = title; // 受け取ったタイトルをこのTodoに保存します
        this.done = done; // 受け取った状態をこのTodoに保存します
    } // Todoを作る処理はここまでです

    String toItem() { // このTodoを1行の文字列に変えるメソッド（処理）です
        if (done) { // 済んでいるかを確かめます
            return "<li>[済] " + title + "</li>"; // 済んでいれば[済]を付けた行を返します
        } // 済んでいる場合の処理はここまでです
        return "<li>[未完了] " + title + "</li>"; // 済んでいない場合は[未完了]を付けた行を返します
    } // toItemメソッドはここまでです
} // Todoクラスはここまでです

public class Main { // 実行の入口を持つMainクラスです
    public static void main(String[] args) { // Javaが最初に実行するメソッド（処理のまとまり）です
        List<Todo> todos = new ArrayList<>(); // Todoを入れる空のList（並べる入れ物）を作ります
        todos.add(new Todo("牛乳を買う", false)); // 未完了のTodoを1件追加します
        todos.add(new Todo("ゴミを出す", true)); // 完了したTodoを1件追加します
        todos.add(new Todo("さんぽをする", false)); // 未完了のさんぽをするTodoを1件追加します
        todos.add(new Todo("店に行く", false)); // 未完了の店に行くTodoを1件追加します

        for (Todo todo : todos) { // ListからTodoを1件ずつ取り出して繰り返します
            System.out.println(todo.toItem()); // 変換した1行をターミナル（文字を表示する画面）に出します
        } // 繰り返しはここまでです
    } // mainメソッドはここまでです
} // Mainクラスはここまでです
