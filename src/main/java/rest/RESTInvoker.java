package rest;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AccessDeniedException;
import java.util.concurrent.Semaphore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Basic REST Reader
 * @author Axel Wickenkamp
 *
 */
public class RESTInvoker {

    private static final int MAX_CONCURRENT_CALLS = 150;
    private static final Semaphore concurrentCalls = new Semaphore(MAX_CONCURRENT_CALLS);

    private final String baseUrl;
    private final String username;
    private final String password;
    private String secret = null;
    public static final String HTTP_STATUS_FORBIDDEN = "Access forbidden";

    public RESTInvoker(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        disableSSL();
    }

    public RESTInvoker(String baseUrl, String secret) {
        this(baseUrl,"","");
        this.secret = secret;    
    }

    public String getDataFromServer(String path) {
        // Adquirir un permiso. Si no hay permisos disponibles, el hilo se bloquea
        // hasta que alguno de los 80 hilos libere el permiso.
        try {
            concurrentCalls.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("El hilo fue interrumpido al intentar adquirir el semáforo", e);
        }

        StringBuilder sb = new StringBuilder();
        int code = 0;
        HttpURLConnection urlConnection = null; 

        try {
            URL url = new URL(baseUrl + path);

            urlConnection = (HttpURLConnection) setUsernamePassword(url);
            
            if(secret != null){
                urlConnection.setRequestProperty("Authorization","Bearer " + secret);
            }

            code = urlConnection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            return sb.toString();
        } 
        catch (Exception e) {
            if(code == 403) throw new RuntimeException(HTTP_STATUS_FORBIDDEN, e);
            throw new RuntimeException(e);
        } finally {
            // Asegurarse de desconectar
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            // Liberar el permiso al terminar la llamada (éxito o error).
            concurrentCalls.release();
        }
    }

    private URLConnection setUsernamePassword(URL url) throws IOException {
        URLConnection urlConnection = url.openConnection();
        if ( username != null && ! username.isEmpty() ) {
            String authString = username + ":" + password;
            String authStringEnc = new String(Base64.encodeBase64(authString.getBytes()));
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        }
        return urlConnection;
    }

    private void disableSSL() {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };

            // Install the all-trusting trust manager
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
