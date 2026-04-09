package com.codo.echocode;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private EditerView myEditor;
    private static final int PICK_FILE_REQUEST = 100;
    private static final int SAVE_FILE_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();

        myEditor = findViewById(R.id.editor_view);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            handleSharedFile(intent);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            handleSharedFile(intent);
        }

        ImageButton folder = findViewById(R.id.folder);
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.folder_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.option_1) {
                            openFilePicker();
                            return true;
                        } else if (id == R.id.option_2) {
                            saveIt();
                            return true;
                        } else if (id == R.id.option_3) {
                        triggerSaveAs();
                        return true;
                    }
                        return true;
                    }
                });
                popup.show();
            }
        });

        ImageButton mainm = findViewById(R.id.main_menu);
        mainm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.op_1) {
                            showEditPopup();
                        } else if (id == R.id.op_4) {
                            System.exit(0);
                        } else if (id == R.id.color_blue) {
                            myEditor.setEditorColor(Color.BLUE);
                        } else if (id == R.id.color_green) {
                            myEditor.setEditorColor(Color.GREEN);
                        } else if (id == R.id.color_red) {
                            myEditor.setEditorColor(Color.RED);
                        } else if (id == R.id.size_14) {
                            myEditor.setEditorSize(14);
                        } else if (id == R.id.size_16) {
                            myEditor.setEditorSize(16);
                        } else if (id == R.id.size_18) {
                            myEditor.setEditorSize(18);
                        } else if (id == R.id.size_20) {
                            myEditor.setEditorSize(20);
                        } else if (id == R.id.op_3) {
                            showAboutDialog();
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });
    }
    private void triggerSaveAs() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Use the most generic type first
        intent.setType("text/plain");

        // This tells Android you accept any of these text-based formats
        String[] mimeTypes = {
                "text/plain",
                "text/x-python",
                "text/x-java",
                "text/x-csrc",
                "text/x-c++src",
                "text/x-perl",
                "text/x-php"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        // Default suggested name
        intent.putExtra(Intent.EXTRA_TITLE, "script.txt");

        startActivityForResult(intent, SAVE_FILE_REQUEST);
    }
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("EchoCode is a lightweight code editor focused on speed and simplicity.")
                .setPositiveButton("ok", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    public void showEditPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go to Line");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = input.getText().toString();
                if (!m_Text.isEmpty()) {
                    try {
                        int lineNum = Integer.parseInt(m_Text);
                        myEditor.goToLine(lineNum);
                    } catch (NumberFormatException ignored) {}
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public void saveIt() {
        TextView rt = findViewById(R.id.titles);
        String currentPath = rt.getText().toString();
        String content = myEditor.getEditor().getText().toString();

        if (currentPath.equals("New")) {
            triggerSaveAs(); // Ask the user where to save it
        } else {
            saveFile(currentPath, content); // Save directly to the path
        }
    }

    public void saveFile(String path, String txt) {
        File file = new File(path);
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(file);
            writer.append(txt);
            writer.flush();
            writer.close();
            Toast.makeText(this, "Saved: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeTextToUri(Uri uri, String text) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            if (pfd != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                fileOutputStream.write(text.getBytes());
                fileOutputStream.close();
                pfd.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            if (requestCode == PICK_FILE_REQUEST) {
                String content = readFileContent(uri);
                String fileName = getFullPath(uri);
                TextView titleView = findViewById(R.id.titles);
                titleView.setText(fileName);
                myEditor.getEditor().setText(content);
                myEditor.updateLineNumbers();
            } else if (requestCode == SAVE_FILE_REQUEST) {
                String content = myEditor.getEditor().getText().toString();
                writeTextToUri(uri, content);
                String fileName = getFullPath(uri);
                TextView titleView = findViewById(R.id.titles);
                titleView.setText(fileName);
                Toast.makeText(this, "File Saved", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Code File"), PICK_FILE_REQUEST);
    }

    private String readFileContent(Uri uri) {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private void handleSharedFile(Intent intent) {
        Uri fileUri = null;
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        } else if (intent.getData() != null) {
            fileUri = intent.getData();
        }

        if (fileUri != null) {
            String content = readFileContent(fileUri);
            String path = getFullPath(fileUri);
            TextView tt = findViewById(R.id.titles);
            tt.setText(path);
            myEditor.getEditor().setText(content);
            myEditor.updateLineNumbers();
        }
    }

    public String getFullPath(Uri uri) {
        String path = null;
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                String[] split = docId.split(":");
                path = Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
                path = getDataColumn(contentUri, null, null);
            } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                else if ("video".equals(type)) contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                else if ("audio".equals(type)) contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                path = getDataColumn(contentUri, "_id=?", new String[]{split[1]});
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            path = getDataColumn(uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }
        return path != null ? path : uri.toString();
    }

    private String getDataColumn(Uri uri, String selection, String[] selectionArgs) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("_data"));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }
}