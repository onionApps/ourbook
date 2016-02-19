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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLSocketFactory;

public class HttpClient {

    private static void log(String str) {
        Log.i("HTTP", str);
    }

    public static String get(Context context, Uri uri) throws IOException {
        return new String(getbin(context, uri), Utils.utf8);
    }

    public static String get(Context context, String uriStr) throws IOException {
        return get(context, Uri.parse(uriStr));
    }

    public static String getNoTor(Context context, Uri uri) throws IOException {
        return new String(getbin(context, uri, false, true, 2), Utils.utf8);
    }

    public static byte[] getext(Context context, Uri uri) throws IOException {
        return getbin(context, uri, true, true, 2);
    }

    public static byte[] getbin(Context context, String uriStr) throws IOException {
        return getbin(context, Uri.parse(uriStr));
    }

    public static byte[] getbin(Context context, Uri uri) throws IOException {
        return getbin(context, uri, true, false, 0);
    }

    private static void writeLine(OutputStream os, String str) throws IOException {
        os.write((str + "\r\n").getBytes());
    }

    private static void writeHeader(OutputStream os, String key, String val) throws IOException {
        writeLine(os, key + ": " + val);
    }

    private static byte[] getbin(Context context, Uri uri, boolean torified, boolean allowTls, int redirs) throws IOException {

        log("request " + uri + " " + torified + " " + allowTls + " " + redirs);

        byte[] content = new byte[0];
        HashMap<String, String> headers = new HashMap<>();
        Socket socket = null;

        log("" + uri.getScheme());
        boolean tls = "https".equals(uri.getScheme()) && allowTls;

        try {

            // open connection
            int port = uri.getPort();
            if (port < 0) {
                port = tls ? 443 : 80;
            }
            if (torified) {
                socket = new TorSocket(context, uri.getHost(), port);
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(uri.getHost(), port), 10000);
            }


            // tls encryption
            if (tls) {
                log("tls");
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = factory.createSocket(socket, null, -1, true);
            }


            // send request
            OutputStream os = socket.getOutputStream();
            String req = uri.getEncodedPath();
            if (uri.getEncodedQuery() != null) req += "?" + uri.getEncodedQuery();
            writeLine(os, "GET " + req + " HTTP/1.0");
            writeHeader(os, "Host", uri.getHost());
            writeHeader(os, "Accept-Encoding", "gzip, deflate");
            //writeHeader(os, "Accept-Encoding", "gzip");
            writeLine(os, "");
            os.flush();


            // read response headers
            InputStream is = socket.getInputStream();
            for (int il = 0; ; il++) {

                // read header line
                StringBuilder sb = new StringBuilder();
                while (true) {
                    int c = is.read();
                    if (c < 0) throw new IOException();
                    if (c == '\n') break;
                    sb.append((char) c);
                }
                String l = sb.toString().trim();

                // break if end of header
                if (l.equals("")) break;

                // throw if not http
                if (il == 0 && !l.startsWith("HTTP/")) {
                    throw new IOException();
                }

                // parse header line
                String[] hh = l.split("\\:", 2);
                if (hh.length != 2) continue;
                headers.put(hh[0].trim(), hh[1].trim());

            }


            // log headers
            for (Map.Entry<String, String> p : headers.entrySet()) {
                log(p.getKey() + ": " + p.getValue());
            }


            // get content length
            int len;
            int maxlen = 1024 * 512;
            {
                len = maxlen;
                String slen = headers.get("Content-Length");
                if (slen != null) {
                    try {
                        len = Integer.parseInt(slen);
                    } catch (NumberFormatException ex) {
                        throw new IOException(ex);
                    }
                }
                if (len > maxlen) {
                    throw new IOException();
                }
            }


            // read response
            {
                ByteArrayOutputStream ws = new ByteArrayOutputStream();
                byte[] buf = new byte[1024 * 8];
                for (int i = 0; i < len; ) {
                    int n = is.read(buf);
                    if (n < 0) break;
                    ws.write(buf, 0, n);
                    i += n;
                }
                ws.close();
                content = ws.toByteArray();
            }


            // encodings
            String encoding = headers.get("Content-Encoding");
            InputStream zs = null;
            if ("gzip".equals(encoding)) {
                log("gzip");
                zs = new GZIPInputStream(new ByteArrayInputStream(content));
            }
            if ("deflate".equals(encoding)) {
                log("deflate");
                zs = new InflaterInputStream(new ByteArrayInputStream(content));
            }
            if (zs != null) {
                ByteArrayOutputStream ws = new ByteArrayOutputStream();
                byte[] buf = new byte[1024 * 8];
                for (; ; ) {
                    int n = zs.read(buf);
                    if (n < 0) break;
                    ws.write(buf, 0, n);
                }
                ws.close();
                content = ws.toByteArray();
            }


        } finally {

            if (socket != null) {
                socket.close();
            }

        }


        // handle redirects
        if (redirs > 0) {
            String headerLocation = headers.get("Location");
            if (headerLocation != null) {
                log("redirection");
                content = getbin(context, Uri.parse(headerLocation), torified, allowTls, redirs - 1);
            }
        }


        // return contents
        return content;

    }

}
