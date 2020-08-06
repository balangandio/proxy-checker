package com.comxa.universo42.proxychecker;

import android.app.Activity;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.comxa.universo42.proxychecker.modelo.Proxy;
import com.comxa.universo42.proxychecker.modelo.ProxyChecker;
import com.comxa.universo42.proxychecker.modelo.ProxyFile;
import com.comxa.universo42.proxychecker.view.FileExplorerActivity;
import com.comxa.universo42.proxychecker.view.ProxyListActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements ServiceConnection {
    public static final int FILE_EXPLORER_REQUEST_CODE = 1;

    public static final String FILES_DIR = "ProxyChecker";
    public static final String FILE_ON = "Ons.txt";
    public static final String FILE_OFF = "Offs.txt";
    public static final String FILE_CONFIG = "config.conf";

    private Button btnSsh;
    private Button btnCheck;
    private Button btnProxies;
    private Button btnThreads;
    private Button btnPayload;
    private Button btnArq;
    private Button btnColar;

    private int qtdThreads;
    private String sshServer;
    private String payload;

    private ConfigLoader config;
    private File fileSelecionado;
    private ProxyFile loader;
    private List<Proxy> proxies = new ArrayList<Proxy>();
    private ProxyChecker checker;

    private boolean needUnbind;
    private CheckerControl control;
    private ThreadCheck threadCheck;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_main);

        btnSsh = (Button) findViewById(R.id.btnSsh);
        btnCheck = (Button) findViewById(R.id.btnCheck);
        btnCheck.setBackgroundResource(R.drawable.btn_checked);
        btnCheck.setEnabled(false);
        btnPayload = (Button) findViewById(R.id.btnPayload);
        btnArq = (Button) findViewById(R.id.btnArq);
        btnColar = (Button) findViewById(R.id.btnColar);
        btnProxies = (Button) findViewById(R.id.btnProxies);
        btnProxies.setEnabled(false);
        btnThreads = (Button) findViewById(R.id.btnThreads);

        config = new ConfigLoader(getFilesDir().getAbsolutePath() + "/" + FILE_CONFIG);
        loadConfig();
        refreshBtnThreads();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveConfig();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, CheckerService.class), this, 0);
        needUnbind = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopThreadCheck();
        unbindService();
    }

    public void onClickBtnSsh(View view) {
        showSshDialog();
    }

    public void onClickBtnThreads(View view) {
        showThreadsDialog();
    }

    public void onClickBtnPayload(View view) {
        showPayloadDialog();
        showMsg(getString(R.string.msgPayload));
    }

    public void onClickBtnArq(View view) {
        Intent intent = new Intent(this, FileExplorerActivity.class);

        startActivityForResult(intent, FILE_EXPLORER_REQUEST_CODE);
    }

    public void onClickBtnColar(View view) {
        String str = getClipBoardStr();

        if (str != null) {
            this.loader = new ProxyFile(str);
            loadProxies();
        }
    }

    public void onClickBtnProxies(View view) {
        Intent intent = new Intent(this, ProxyListActivity.class);

        if (checker == null) {
            intent.putExtra(ProxyListActivity.INTENT_PROXIES, serializarProxies(proxies));
        } else {
            if (checker.getProxyOns().size() > 0)
                intent.putExtra(ProxyListActivity.INTENT_PROXY_ONS, serializarProxies(checker.getProxyOns()));
            if (checker.getProxyOffs().size() > 0)
                intent.putExtra(ProxyListActivity.INTENT_PROXY_OFFS, serializarProxies(checker.getProxyOffs()));
        }

        startActivity(intent);
    }

    public void onClickBtnCheck(View view) {
        deleteInput();
        btnCheck.setEnabled(false);
        btnProxies.setText(getString(R.string.btnProxies));
        bindService(new Intent(this, CheckerService.class), this, BIND_AUTO_CREATE);
    }

    private void setupBtnCheckStop() {
        btnCheck.setText(getString(R.string.btnCheckInterromper));
        btnCheck.setBackgroundResource(R.drawable.btn_stop);
        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnCheck.setEnabled(false);
                stopChecking();
                btnCheck.setText(getString(R.string.btnCheckInterrompido));
            }
        });
        btnCheck.setEnabled(true);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder controller) {
        needUnbind = true;

        CheckerService.ServiceController con = (CheckerService.ServiceController) controller;
        control = con.getControl();
        checker = control.getChecker();

        if (checker == null) {
            control.setChecker(proxies, qtdThreads, payload.replace("[crlf]", "\r\n").replace("[host_port]", sshServer));
            checker = control.getChecker();

            startService();
            checker.check(false);
            startThreadCheck();
            setupBtnCheckStop();
        } else {
            deleteInput();
            btnProxies.setEnabled(true);

            if (!checker.isComplete()) {
                setupBtnCheckStop();
                startThreadCheck();
            } else {
                onCheckerDone();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        btnCheck.setEnabled(false);
        btnCheck.setText(getString(R.string.btnCheckInterrompido));
        stopThreadCheck();
        stopChecking();
        unbindService();
        stopService();
        showMsg(getString(R.string.msgServiceDisconnected));
    }

    private void unbindService() {
        if (needUnbind)
            unbindService(this);
        needUnbind = false;
        control = null;
    }

    private void startService() {
        Intent i = new Intent(this, CheckerService.class);

        startService(i);
    }

    private void stopService() {
        Intent i = new Intent(this, CheckerService.class);

        stopService(i);
    }

    private void stopChecking() {
        if (checker != null && checker.isRunning())
            checker.stop();
    }

    private void startThreadCheck() {
        threadCheck = new ThreadCheck();
        threadCheck.start();
    }

    private void stopThreadCheck() {
        if (threadCheck != null)
            threadCheck.stop();
    }

    private void onCheckerDone() {
        btnCheck.setEnabled(false);

        if (checker != null && checker.isComplete()) {
            btnCheck.setText(getString(R.string.btnChecked));
            btnCheck.setBackgroundResource(R.drawable.btn_checked);
        } else {
            btnCheck.setText(getString(R.string.btnCheckInterrompido));
        }

        saveProxies();
        unbindService();
        stopService();
    }


    private void loadProxies() {
        if (loader == null)
            return;

        try {
            loader.load();
            proxies.addAll(loader.getProxies());
            refleshBtnProxies();

            if (proxies.size() > 0) {
                btnCheck.setEnabled(true);
                btnProxies.setEnabled(true);
            }
        } catch (IOException e) {
            showMsg(e.getMessage());
        }
    }

    private void saveProxies() {
        if (checker == null)
            return;

        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + FILES_DIR;

        try {
            File fileDir = new File(dir);
            if (!fileDir.exists()) {
                if (!new File(dir).mkdir())
                    throw new IOException(getString(R.string.msgErroAoSalvarDir));
            }

            dir += "/";

            ProxyFile saver = new ProxyFile(new File(dir + FILE_ON));
            saver.setProxies(checker.getProxyOns());
            saver.save();
            saver = new ProxyFile(new File(dir + FILE_OFF));
            saver.setProxies(checker.getProxyOffs());
            saver.save();

            showMsg(getString(R.string.msgSucessoAoSalvar));
        } catch(IOException e) {
            showMsg(getString(R.string.msgErroAoSalvar) + " " + e.getMessage());
        }
    }

    private void saveConfig() {
        config.setPayload(payload);
        config.setQtdThreads(qtdThreads);
        config.setSshServer(sshServer);

        try {
            config.save();
        } catch(IOException e) {
            showMsg(e.getMessage());
        }
    }

    private void loadConfig() {
        try {
            config.load();

            payload = config.getPayload();
            qtdThreads = config.getQtdThreads();
            sshServer = config.getSshServer();
        } catch(IOException e) {
            showMsg(e.getMessage());
        }
    }

    private ArrayList<String> serializarProxies(List<Proxy> proxies) {
        ArrayList<String> serial = new ArrayList<String>(proxies.size());

        for (Proxy p : proxies)
            serial.add(p.toString());

        return serial;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_EXPLORER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                fileSelecionado = new File(data.getStringExtra("file"));

                loader = new ProxyFile(fileSelecionado);
                loadProxies();
            }
        }
    }

    private void deleteInput() {
        btnColar.setVisibility(View.GONE);
        btnArq.setVisibility(View.GONE);
        btnPayload.setVisibility(View.GONE);
        btnSsh.setVisibility(View.GONE);
        btnThreads.setVisibility(View.GONE);
    }

    private void refleshBtnProxies() {
        btnProxies.setText(getString(R.string.btnProxies) + " ( " + proxies.size() + " ) ");
    }

    private void refreshBtnThreads() {
        btnThreads.setText(getString(R.string.btnThreads) + " ( " +  qtdThreads + " )");
    }

    private void showThreadsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.titleThreadsDialog));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(qtdThreads));
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    qtdThreads = Integer.parseInt(input.getText().toString());
                    if (qtdThreads < 1)
                        throw new NumberFormatException();
                    refreshBtnThreads();
                } catch(NumberFormatException e) {
                    showMsg(getString(R.string.msgQtdThreadsNaoNumerica));
                }
            }
        });

        builder.show();
    }

    private void showPayloadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.titlePayloadDialog));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(payload);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String str = input.getText().toString();
                if (str.length() > 0)
                    payload = str;
            }
        });

        builder.setNegativeButton("DEFAULT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                payload = ConfigLoader.DEFAULT_PAYLOAD;
            }
        });

        builder.show();
    }

    private void showSshDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.titleSshDialog));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(sshServer);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String str = input.getText().toString().trim();

                int aux = str.indexOf(':');

                if (aux == -1 || aux == str.length()-1) {
                    showMsg(getString(R.string.msgSshInvalido));
                    return;
                }

                try {
                    Integer.parseInt(str.substring(aux+1));

                    sshServer = str;
                } catch(NumberFormatException e) {
                    showMsg(getString(R.string.msgSshInvalido));
                }
            }


        });

        builder.show();
    }

    private String getClipBoardStr() {
        String data = null;

        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            data = String.valueOf(clipboard.getText());
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            if (!clipboard.hasPrimaryClip()) {
                showMsg(getString(R.string.msgClipboardVazio));
                return null;
            }

            if (!clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) && !clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                showMsg(getString(R.string.msgClipboardNaoTexto));
                return null;
            }

            android.content.ClipData clip = clipboard.getPrimaryClip();

            data = String.valueOf(clip.getItemAt(0).getText());
        }

        return data;
    }

    private void showMsg(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }


    private class ThreadCheck implements Runnable {
        private boolean running;

        public void start() {
            running = true;
            new Thread(this).start();
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            while (running && checker.isRunning()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {e.printStackTrace();}
            }

            if (running) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onCheckerDone();
                    }
                });
            }
            running = false;
        }
    }
}
