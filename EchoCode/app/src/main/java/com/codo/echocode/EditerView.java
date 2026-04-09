package com.codo.echocode;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class EditerView extends LinearLayout {
    private EditText editor;
    private TextView line;
    private ScrollView lineScroll;
    private static final String[] JAVA_KEYWORDS = {"public", "private", "protected", "class", "interface", "extends", "implements", "import", "package", "new", "return", "void", "static", "final", "try", "catch", "throw", "throws", "if", "else", "for", "while", "switch", "case", "break", "continue"};
    private static final String[] PYTHON_KEYWORDS = {"def", "lambda", "global", "nonlocal", "and", "or", "not", "is", "in", "yield", "with", "as", "assert", "del", "elif", "except", "finally", "from", "import", "pass", "raise"};

    public EditerView(Context con) {
        super(con);
        init();
    }

    public EditerView(Context con, AttributeSet attr) {
        super(con, attr);
        init();
    }

    public EditerView(Context con, AttributeSet attr, int i) {
        super(con, attr, i);
        init();
    }

    public void init() {
        // 1. Set orientation first
        setOrientation(HORIZONTAL);

        ///  ///////
        lineScroll=new ScrollView(getContext());
        lineScroll.setOnTouchListener((v,event) -> true);
        // 2. Initialize the views BEFORE calling methods that use them
        line = new TextView(getContext());
        line.setText("1");
        line.setGravity(Gravity.CENTER_HORIZONTAL);
        line.setPadding(5, 0, 5, 0);
        line.setBackgroundColor(Color.LTGRAY); // Optional: differentiate line number area

        editor = new EditText(getContext());
        editor.setBackgroundColor(Color.TRANSPARENT);
        editor.setPadding(5, 0, 0, 0);
        editor.setGravity(Gravity.TOP);
        editor.setHorizontallyScrolling(true);

        // 3. Define LayoutParams
        LayoutParams lineParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        LayoutParams editorParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);

        // 4. Add views to the layout
        lineScroll.addView(line, lineParams);
        addView(lineScroll,lineParams);
        addView(editor, editorParams);

        // 5. Default styling
        setEditorSize(17);

        /// //////////////////////////////


        editor.addTextChangedListener(new TextWatcher() {
            private boolean isHighlighting = false; // Prevent infinite loop
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isHighlighting) {
                    isHighlighting = true;
                    applySyntaxHighlighting(s);
                    isHighlighting = false;
                }
            }
        });
        /// //////////////////
        editor.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            // Force the line TextView to match the vertical scroll of the editor
            lineScroll.setScrollY(scrollY);
        });

        /// /////////




    }

    public void goToLine(int line) {
        // We use line - 1 because lines are 0-indexed in code
        // but users usually think of the first line as "1"
        int lineIndex = Math.max(0, line - 1);

        // Calculate pixel position: line height * line index
        int y = lineIndex * editor.getLineHeight();

        // Scroll the view to that position
        editor.setScrollY(y);
    }

    public void updateLineNumbers() {
        // Start a new background thread to handle the string building
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int lineCount = editor.getLineCount();
                int effectiveLineCount = (lineCount == 0) ? 1 : lineCount;

                final StringBuilder linesText = new StringBuilder();
                for (int i = 1; i <= effectiveLineCount; i++) {
                    linesText.append(i).append("\n");
                }

                // Return to the UI thread to set the text
                line.post(new Runnable() {
                    @Override
                    public void run() {
                        line.setText(linesText.toString());
                    }
                });
            }
        }).start();
    }
    public void setEditorColor(int col){
        editor.setTextColor(col);
    }

    public void setEditorSize(int size) {
        // Ensure objects aren't null if called externally before init
        if (editor != null && line != null) {
            editor.setTextSize(size);
            // Fix: String.valueOf() is required, otherwise Android looks for a Resource ID
            line.setTextSize(size);
        }
    }
    public void setEditorText(String text){
        editor.setText(text);
    }
    public EditText getEditor(){
        return editor;
    }


    private void applySyntaxHighlighting(Editable s) {
        // 1. Clear old spans
        ForegroundColorSpan[] oldSpans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : oldSpans) s.removeSpan(span);

        // 2. Define Colors
        int keywordColor = Color.parseColor("#CC7832"); // Orange
        int commentColor = Color.parseColor("#808080"); // Gray
        int stringColor  = Color.parseColor("#6A8759"); // Green
        int numberColor  = Color.parseColor("#6897BB"); // Blue
        int symbolColor = Color.parseColor("#A9B7C6");

        // 3. Keywords (Java & Python)
        highlightPattern(s, JAVA_KEYWORDS, keywordColor);
        highlightPattern(s, PYTHON_KEYWORDS, keywordColor);

        // 4. Numbers (\b\d+\b matches whole numbers only)
        highlightRegex(s, "\\b\\d+\\b", numberColor);

        // 5. Strings "sss" (Double quotes)
        // Pattern: " followed by anything not a " followed by "
        highlightRegex(s, "\".*?\"", stringColor);

        // 6. Strings 'sss' (Single quotes)
        highlightRegex(s, "'.*?'", stringColor);

        // 7. Comments //dddd (Java style)
        highlightRegex(s, "//.*", commentColor);

        // 8. Comments #ddd (Python style)
        highlightRegex(s, "#.*", commentColor);

        highlightRegex(s, "[\\(\\)\\[\\]\\{\\}]", symbolColor);

// 2. Match Punctuation: : ; , .
        highlightRegex(s, "[:;,\\.]", symbolColor);

// 3. Match Operators: + - * / = % ! < >
        highlightRegex(s, "[\\+\\-\\*\\/=%!<>?&|]", symbolColor);

    }

    private void highlightPattern(Editable s, String[] keywords, int color) {
        for (String word : keywords) {
            // Pattern \\b ensures we only match whole words
            Pattern p = Pattern.compile("\\b" + word + "\\b");
            Matcher m = p.matcher(s);
            while (m.find()) {
                s.setSpan(new ForegroundColorSpan(color), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void highlightRegex(Editable s, String regex, int color) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);
        while (m.find()) {
            s.setSpan(new ForegroundColorSpan(color), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}