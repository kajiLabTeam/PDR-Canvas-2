package io.github.harutiro.pdrcanvas2

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SetConfigActivity : AppCompatActivity() {
    private var editText: EditText? = null
    private var checkBox: CheckBox? = null
    private var button: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.set_config)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // 画面縦向き固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        editText = findViewById<View?>(R.id.editText) as EditText
        checkBox = findViewById<View?>(R.id.checkBox) as CheckBox

        // 入力制限
        editText!!.setFilters(inputFilter)
        editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                // 先頭0 削除
                if (s.toString().matches("^0+[0-9]*".toRegex())) {
                    editText!!.setText(s.toString().replaceFirst("^0+".toRegex(), ""))
                }
            }
        })

        // 現在値取得
        val intent = getIntent()
        val size = intent.getIntExtra("size", 50)
        val transparent = intent.getBooleanExtra("transparent", false)

        // 現在値セット
        editText!!.setText(size.toString())
        editText!!.setHint(size.toString())
        editText!!.setSelection(editText!!.getText().length)
        checkBox!!.setChecked(transparent)

        button = findViewById<View?>(R.id.ok_button) as Button

        // 値送信
        button!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                val intent = Intent()
                if (!TextUtils.isEmpty(editText!!.getText().toString())) {
                    intent.putExtra("size", editText!!.getText().toString().toInt())
                }
                intent.putExtra("transparent", checkBox!!.isChecked())
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    // 入力フィルタ 数字3桁
    var inputFilter: Array<InputFilter?> = arrayOf<InputFilter?>( // 数字のみ
        object : InputFilter {
            override fun filter(
                source: CharSequence,
                start: Int,
                end: Int,
                dest: Spanned?,
                dstart: Int,
                dend: Int
            ): CharSequence {
                if (source.toString().matches("^[0-9]*$".toRegex())) {
                    return source
                } else {
                    return ""
                }
            }
        },  // 3文字まで
        LengthFilter(3)
    )
}