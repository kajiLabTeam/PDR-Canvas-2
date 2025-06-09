package io.github.harutiro.pdrcanvas2;

import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;

public class SetConfigActivity extends AppCompatActivity {

    private EditText editText;
    private CheckBox checkBox;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_config);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 画面縦向き固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        editText = (EditText) findViewById(R.id.editText);
        checkBox = (CheckBox) findViewById(R.id.checkBox);

        // 入力制限
        editText.setFilters(inputFilter);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 先頭0 削除
                if (s.toString().matches("^0+[0-9]*")) {
                    editText.setText(s.toString().replaceFirst("^0+", ""));
                }
            }
        });

        // 現在値取得
        Intent intent = getIntent();
        int size = intent.getIntExtra("size", 50);
        boolean transparent = intent.getBooleanExtra("transparent", false);

        // 現在値セット
        editText.setText(String.valueOf(size));
        editText.setHint(String.valueOf(size));
        editText.setSelection(editText.getText().length());
        checkBox.setChecked(transparent);

        button = (Button) findViewById(R.id.ok_button);

        // 値送信
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                if (!TextUtils.isEmpty(String.valueOf(editText.getText()))) {
                    intent.putExtra("size", Integer.parseInt(String.valueOf(editText.getText())));
                }
                intent.putExtra("transparent", checkBox.isChecked());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    // 入力フィルタ 数字3桁
    InputFilter[] inputFilter = new InputFilter[]{
            // 数字のみ
            new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    if (source.toString().matches("^[0-9]*$")) {
                        return source;
                    } else {
                        return "";
                    }
                }
            },
            // 3文字まで
            new InputFilter.LengthFilter(3)
    };

}