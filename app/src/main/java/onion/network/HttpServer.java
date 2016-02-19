/*
 * Network.onion - fully distributed p2p social network using onion routing
 *
 * http://play.google.com/store/apps/details?id=onion.network
 * http://onionapps.github.io/Network.onion/
 * http://github.com/onionApps/Network.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.network;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public class HttpServer {

    Sock serverSock;
    Thread serverThread;
    Handler handler;

    public HttpServer(Sock serverSock, Handler handler) {
        this.handler = handler;
        this.serverSock = serverSock;
        serverThread = new Thread() {
            @Override
            public void run() {
                mainLoop();
            }
        };
    }

    public HttpServer(ServerSocket serverSocket, Handler handler) {
        this(new ServerSocketSock(serverSocket), handler);
    }

    public HttpServer(LocalServerSocket serverSocket, Handler handler) {
        this(new LocalServerSock(serverSocket), handler);
    }

    private static List<String> readHeaders(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        ArrayList<String> headers = new ArrayList<>();
        while (true) {
            String header = r.readLine();
            if (header == null || header.trim().length() == 0) {
                break;
            }
            headers.add(header.trim());
        }
        return headers;
    }

    public void start() {
        serverThread.start();
    }

    public void stop() {
        try {
            serverSock.close();
        } catch (IOException ex) {
        }
    }

    private void mainLoop() {
        while (true) {
            final Sock s;
            try {
                s = serverSock.accept();
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            new Thread() {
                @Override
                public void run() {
                    try {
                        handleSock(s);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            s.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }.start();
        }
    }

    private Request readRequest(InputStream is) throws IOException {
        List<String> hh = readHeaders(is);
        if (hh.size() == 0) throw new ProtocolException();
        String requestLine = hh.get(0);
        log(requestLine);
        hh.remove(0);
        String[] requestToks = requestLine.trim().split(" ");
        if (requestToks.length != 3) throw new ProtocolException();
        String method = requestToks[0];
        String path = requestToks[1];
        String protocol = requestToks[2];
        if (!protocol.startsWith("HTTP/")) throw new ProtocolException();
        TreeMap<String, String> headers = new TreeMap<>();
        for (String h : hh) {
            String[] tt = h.split("\\:", 2);
            if (tt.length == 2) {
                headers.put(tt[0].trim(), tt[1].trim());
            }
        }
        return new Request(method, path, headers);
    }

    private void writeResponse(Response response, OutputStream os) throws IOException {
        byte[] content = response.getContent();
        response.putHeader("Content-Length", "" + content.length);
        response.putHeader("Connection", "close");
        os.write(("HTTP/1.0 " + response.getStatusCode() + " " + response.getStatusString() + "\r\n").getBytes());
        for (Map.Entry<String, String> p : response.getHeaders().entrySet()) {
            os.write((p.getKey() + ": " + p.getValue() + "\r\n").getBytes());
        }
        os.write("\r\n".getBytes());
        os.write(content);
        os.flush();
    }

    private void applyEncodings(Request request, Response response) throws IOException {
        String acceptEncodingHeader = request.getHeader("Accept-Encoding", "");
        HashSet<String> acceptedEncodings = new HashSet<>(Arrays.asList(acceptEncodingHeader.split(",\\s*")));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream encodingStream = null;
        String encodingName = null;
        if (acceptedEncodings.contains("deflate") && encodingStream == null) {
            encodingStream = new DeflaterOutputStream(outputStream);
            encodingName = "deflate";
        }
        if (acceptedEncodings.contains("gzip") && encodingStream == null) {
            encodingStream = new GZIPOutputStream(outputStream);
            encodingName = "gzip";
        }
        if (encodingStream != null) {
            encodingStream.write(response.getContent());
            encodingStream.close();
            outputStream.close();
            byte[] compressed = outputStream.toByteArray();
            if (compressed.length < response.getContent().length) {
                response.setContent(compressed);
                response.putHeader("Content-Encoding", encodingName);
            }
        }
    }

    private void handleSock(Sock s) throws IOException {
        Request request = readRequest(s.getInputStream());
        Response response = new Response();
        handler.handle(request, response);
        int uncompressedSize = response.getContent().length;
        applyEncodings(request, response);
        writeResponse(response, s.getOutputStream());
        log(request.getPath() + " " + response.getStatusCode() + " " + response.getStatusString() + " " + uncompressedSize + " " + response.getHeader("Content-Encoding") + " " + response.getContent().length);
    }

    private void log(String str) {
        Log.i("HttpServer", str);
    }

    public interface Handler {
        void handle(Request request, Response response);
    }

    public interface Sock {
        InputStream getInputStream() throws IOException;

        OutputStream getOutputStream() throws IOException;

        void close() throws IOException;

        Sock accept() throws IOException;
    }

    public static class Request {
        private String method = "";
        private String path = "";
        private Map<String, String> headers = new TreeMap<>();

        public Request() {
        }

        public Request(String method, String path, Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.headers = headers;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getHeader(String key) {
            return headers.get(key);
        }

        public String getHeader(String key, String defaultValue) {
            String ret = headers.get(key);
            if (ret == null) ret = defaultValue;
            return ret;
        }
    }

    public static class Response {
        private static Charset utf8 = Charset.forName("UTF-8");
        private int statusCode = 200;
        private String statusString = "OK";
        private byte[] content = new byte[0];
        private Map<String, String> headers = new TreeMap<>();

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusString() {
            return statusString;
        }

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] data) {
            content = data;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public String getHeader(String key) {
            return headers.get(key);
        }

        public void putHeader(String key, String val) {
            headers.put(key, val);
        }

        public void setContentType(String type) {
            putHeader("Content-Type", type);
        }

        public void setStatus(int statusCode, String statusString) {
            this.statusCode = statusCode;
            this.statusString = statusString;
        }

        public void setContent(byte[] data, String contentType) {
            content = data;
            setContentType(contentType);
        }

        public void setContentHtml(String code) {
            setContent(code.getBytes(utf8), "Content-Type: text/html; charset=utf-8");
        }

        public void setContentPlain(String text) {
            setContent(text.getBytes(utf8), "Content-Type: text/plain; charset=utf-8");
        }
    }

    public static class SocketSock implements Sock {
        private Socket s;

        public SocketSock(Socket s) {
            this.s = s;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return s.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return s.getOutputStream();
        }

        @Override
        public void close() throws IOException {
            s.close();
        }

        @Override
        public Sock accept() throws IOException {
            throw new IOException();
        }
    }

    public static class ServerSocketSock implements Sock {
        private ServerSocket s;

        public ServerSocketSock(ServerSocket s) {
            this.s = s;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException();
        }

        @Override
        public void close() throws IOException {
            s.close();
        }

        @Override
        public Sock accept() throws IOException {
            return new SocketSock(s.accept());
        }
    }

    public static class LocalSock implements Sock {
        private LocalSocket s;

        public LocalSock(LocalSocket s) {
            this.s = s;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return s.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return s.getOutputStream();
        }

        @Override
        public void close() throws IOException {
            s.close();
        }

        @Override
        public Sock accept() throws IOException {
            throw new IOException();
        }
    }

    public static class LocalServerSock implements Sock {
        private LocalServerSocket s;

        public LocalServerSock(LocalServerSocket s) {
            this.s = s;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException();
        }

        @Override
        public void close() throws IOException {
            s.close();
        }

        @Override
        public Sock accept() throws IOException {
            return new LocalSock(s.accept());
        }
    }

    public static class ProtocolException extends IOException {
    }

}
