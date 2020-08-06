package com.comxa.universo42.proxychecker;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ConfigLoader {
    public static final int DEFAULT_QTD_THREADS = 10;
    public static final String DEFAULT_SSH_SERVER = "shell.openshells.net:443";
    public static final String DEFAULT_PAYLOAD = "CONNECT [host_port] HTTP/1.0[crlf][crlf]";

    private String file;
    private String payload = DEFAULT_PAYLOAD;
    private String sshServer = DEFAULT_SSH_SERVER;
    private int qtdThreads = DEFAULT_QTD_THREADS;

    public ConfigLoader(String file) {
        this.file = file;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSshServer() {
        return sshServer;
    }

    public void setSshServer(String sshServer) {
        this.sshServer = sshServer;
    }

    public int getQtdThreads() {
        return qtdThreads;
    }

    public void setQtdThreads(int qtdThreads) {
        this.qtdThreads = qtdThreads;
    }

    public void save() throws IOException {
        PrintWriter pw = new PrintWriter(new File(this.file));

        pw.println(this.payload);
        pw.println(this.sshServer);
        pw.println(this.qtdThreads);

        pw.close();
    }

    public void load() throws IOException {
        File f = new File(this.file);

        if (f.exists()) {
            Scanner scanner = new Scanner(f);

            if (scanner.hasNextLine()) {
                this.payload = scanner.nextLine();
                if (scanner.hasNextLine()) {
                    this.sshServer = scanner.nextLine();
                    if (scanner.hasNextLine())
                        this.qtdThreads = Integer.parseInt(scanner.nextLine());
                }
            }

            scanner.close();
        }
    }
}
