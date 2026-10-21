public class Items { // Itemsという名前のクラス（プログラムのまとまり）を作ります。
    public static void main(String[] args) { // mainは、プログラムを実行したとき最初に動く場所です。
        String[] todos = { "牛乳を買う", "たまごを買う", "パンを買う", "掃除をする" }; // String[]は文字列の配列（順番に並べた入れ物）で、todosに3件を入れます。
        boolean[] done = { true, true, false, false }; // boolean[]はtrue（済み）かfalse（未済み）を並べる配列です。
        for (int i = 0; i < todos.length; i++) { // for文（繰り返し）で、iを0から配列の件数未満まで1ずつ増やします。
            if (!todos[i].isEmpty()) { // isEmptyは空文字列かを調べ、空でないときだけ中の処理を行います。
                System.out.println("<li>" + (done[i] ? "[済] " : "") + todos[i] + "</li>"); // 済みなら[済]を付け、Todoを<li>と</li>で囲んで表示します。
            } // 空文字列でなければ出力する範囲はここまでです。
        } // for文の繰り返し範囲はここまでです。
    } // mainの範囲はここまでです。
} // Itemsクラスの範囲はここまでです。
