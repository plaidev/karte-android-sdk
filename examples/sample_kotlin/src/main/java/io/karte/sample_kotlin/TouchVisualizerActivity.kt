package io.karte.sample_kotlin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * タッチ視覚化のデモを表示するアクティビティ
 */
class TouchVisualizerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_touch_visualizer)
        
        // ツールバーの設定
        setSupportActionBar(findViewById(R.id.tool_bar))
        supportActionBar?.title = "タッチ視覚化デモ"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // 説明テキストの設定
        val descriptionTextView = findViewById<TextView>(R.id.description_text)
        descriptionTextView.text = "画面上の任意の場所をタップすると、タッチした箇所が視覚的に表示されます。" +
                "複数の指でタップすると、それぞれのタッチポイントが表示されます。"
        
        // ボタンのクリックリスナー設定
        findViewById<Button>(R.id.button_test).setOnClickListener {
            it.isSelected = !it.isSelected
            if (it.isSelected) {
                (it as Button).text = "ボタンがタップされました！"
            } else {
                (it as Button).text = "このボタンをタップしてください"
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
