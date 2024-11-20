package com.example.myapps;

//import static com.example.myapps.url.serverURL.baseURL;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapps.databases.DatabaseHelper;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase dbSqlite;
    private String baseURL;

    WebView web;
    ProgressDialog progress;
    private ValueCallback<Uri[]> upload;

    // Tambahkan SwipeRefreshLayout
    private SwipeRefreshLayout swipeRefreshLayout;

    // Tambahkan handler dan runnable untuk timeout
    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;

    @SuppressLint({"SetJavaScriptEnabled","WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // Menampilkan tombol back di toolbar
            actionBar.setHomeButtonEnabled(true);
        }

        progress = new ProgressDialog(MainActivity.this);
        progress.setMessage("Menunggu...");
        progress.show();

        dbHelper = new DatabaseHelper(this);
        dbSqlite = dbHelper.getWritableDatabase();

        // Mengambil baseURL dari database
        baseURL = dbHelper.getBaseURL();
        if (baseURL == null || baseURL.isEmpty()) {
            Toast.makeText(this, "URL tidak ditemukan di database", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "URL tidak valid atau kosong.");
            return;
        } else {
            Log.d("MainActivity", "Loaded URL from Database: " + baseURL);
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        web = findViewById(R.id.webView);

        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36";
        web.getSettings().setUserAgentString(userAgent);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.getSettings().setSafeBrowsingEnabled(false);
        web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.clearCache(true);
        web.clearHistory();

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo("com.google.android.webview", 0);
            String webViewVersion = pInfo.versionName;
            Log.d("WebView Version", "Android System WebView Version: " + webViewVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.d("WebView Version", "Android System WebView not found.");
        }

        web.setWebViewClient(new MyWeb());

        WebView.setWebContentsDebuggingEnabled(true);

        web.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                Intent intent;
                intent = fileChooserParams.createIntent();
                upload = filePathCallback;
                startActivityForResult(intent, 101);
                return true;
            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Muat ulang URL saat swipe refresh dilakukan
            web.loadUrl(baseURL);
//            web.loadUrl("http://192.168.1.14:8082/skripsi/20241/arsiplakoro/");
            // Hentikan refresh indicator setelah halaman dimuat
//            swipeRefreshLayout.setRefreshing(false);
        });

        // Atur batas waktu 10 detik untuk pemuatan halaman
        setLoadTimeout(10000);

        // Mulai memuat URL
        web.loadUrl(baseURL);

        web.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long lenght) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("Cookie",cookies);
                request.addRequestHeader("User-Agent",userAgent);
                request.setDescription("Downloading File...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition,userAgent));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(
                                url, contentDisposition, mimeType
                        )
                );
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                manager.enqueue(request);

                Toast.makeText(MainActivity.this,"Sedang mendownload File...",Toast.LENGTH_LONG).show();
            }
        });
    }

    // Metode untuk mengatur batas waktu pemuatan halaman
    private void setLoadTimeout(long timeoutMillis) {
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                // Hentikan pemuatan halaman jika sudah melebihi batas waktu
                if (web != null) {
                    web.stopLoading();

                    // Tampilkan pesan kesalahan
                    showErrorDialog();
                }
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, timeoutMillis);
    }

    // Metode untuk menampilkan dialog kesalahan dengan tombol "Coba Hubungkan Lagi"
    private void showErrorDialog() {
        if (progress.isShowing()) {
            progress.dismiss();
        }

        // Buat EditText untuk menampilkan dan mengedit URL yang ada
        final EditText editText = new EditText(this);
        editText.setText(baseURL);  // Set URL yang ada di EditText

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Koneksi Gagal")
                .setMessage("URL tidak bisa diakses atau server tidak aktif. Silakan periksa koneksi internet Anda atau coba lagi nanti.")
                .setView(editText)  // Tambahkan EditText ke dialog
                .setPositiveButton("Simpan & Coba Lagi", (dialog, which) -> {
                    String newBaseURL = editText.getText().toString().trim();

                    if (!newBaseURL.isEmpty()) {
                        // Perbarui URL di database
                        dbHelper.updateBaseURL(newBaseURL);
                        baseURL = newBaseURL;  // Update baseURL di aplikasi

                        // Tampilkan progress lagi
                        progress.show();

                        // Setel ulang timeout dan muat ulang URL
                        setLoadTimeout(10000);
                        web.loadUrl(baseURL);  // Muat ulang URL yang diperbarui
                    } else {
                        Toast.makeText(MainActivity.this, "URL tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", (dialog, which) -> {
                    dialog.dismiss();

                    // Tutup aplikasi
                    finish(); // Ini akan menutup aktivitas dan mengeluarkan pengguna dari aplikasi
                })
                .show();
    }

    private class MyWeb extends WebViewClient{

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
                return new WebResourceResponse("text/plain", "UTF-8", null);
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // Ambil judul halaman dari WebView dan setel sebagai judul Activity
            String pageTitle = view.getTitle();
            if (pageTitle != null && !pageTitle.isEmpty()) {
                setTitle(pageTitle);  // Mengatur judul activity sesuai dengan judul halaman web
            }
            progress.dismiss();

            // Batalkan timeout jika halaman selesai dimuat
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            Log.e("MainActivity", "HTTP error received: " + errorResponse.getStatusCode() + " - " + errorResponse.getReasonPhrase());
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            Log.d("MainActivity","Terjadi Kesalahan: "+error.getDescription() + " | "+error.toString());

            // Hentikan pemuatan halaman dan tampilkan halaman kosong
            view.stopLoading();
            view.loadUrl("about:blank"); // Menghapus tampilan halaman gagal

            // Batalkan timeout jika ada error yang terdeteksi
            timeoutHandler.removeCallbacks(timeoutRunnable);

            // Tampilkan dialog kesalahan hanya jika halaman utama gagal
            if (request.getUrl().toString().equals(baseURL)) {
                showErrorDialog();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==101){
            if (upload==null)
                return;
            upload.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data));
            upload = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle tombol back di toolbar
        if (web.canGoBack()) {
            web.goBack(); // Kembali ke halaman sebelumnya di WebView
        } else {
            finish(); // Keluar dari activity jika tidak bisa kembali di WebView
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()){
            web.goBack();
        }else{
            super.onBackPressed();
        }
    }
}